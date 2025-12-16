package com.srikar.kafka.service;

import com.srikar.kafka.config.ProcessProperties;
import com.srikar.kafka.domain.ClusterActionResult;
import com.srikar.kafka.model.BrokerSnapshotRow;
import com.srikar.kafka.repository.BrokerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClusterManager {

    private final BrokerRepository repo;
    private final RemoteCommandExecutor exec;
    private final ProcessProperties props;

    private static final Duration SSH_TIMEOUT = Duration.ofSeconds(20);
    private static final int LOG_TRUNCATE = 4000;

    public ClusterManager(BrokerRepository repo,
                          RemoteCommandExecutor exec,
                          ProcessProperties props) {
        this.repo = repo;
        this.exec = exec;
        this.props = props;
    }

    // ---------- Read-only snapshot ----------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> snapshot(String clusterName) {
        List<BrokerSnapshotRow> rows = repo.snapshot(clusterName);
        return rows.stream()
                .map(s -> Map.<String, Object>of(
                        "serverName",    s.getServerName(),
                        "nodeId",        s.getNodeId(),
                        "role",          s.getRole(),
                        "online",        s.isOnline(),
                        "serverAddress", s.getServerAddress(),
                        "lastCheckedAt", s.getLastCheckedAt(),
                        "source",        s.getSource()
                ))
                .collect(Collectors.toList());
    }

    // ---------- Controller actions ----------
    public ClusterActionResult controllerAction(String action) {
        if (!featureOn()) {
            return new ClusterActionResult(
                    null, null, action, 255, "",
                    "process.enabled=false", "disabled", false
            );
        }
        String host = controllerHost();
        String unit = controllerUnit() + ".service";
        if (isBlank(host)) {
            return new ClusterActionResult(
                    null, unit, action, 255, "",
                    "Controller host not configured", "error", false
            );
        }
        ClusterActionResult result = runSystemctl(host, unit, action);
        postActionDbWrite((short) 1, action, result);
        return result;
    }

    // ---------- Broker actions ----------
    public ClusterActionResult brokerAction(short brokerId, String action) {
        if (!featureOn()) {
            return new ClusterActionResult(
                    null, null, action, 255, "",
                    "process.enabled=false", "disabled", false
            );
        }
        String host = brokerHost(String.valueOf(brokerId));
        String unit = brokerUnit() + "@" + brokerId + ".service";
        if (isBlank(host)) {
            return new ClusterActionResult(
                    null, unit, action, 255, "",
                    "No host mapping for brokerId=" + brokerId, "error", false
            );
        }
        ClusterActionResult result = runSystemctl(host, unit, action);
        postActionDbWrite(brokerId, action, result);
        return result;
    }

    // ---------- Internals ----------
    private ClusterActionResult runSystemctl(String host, String unit, String action) {
        String cmd = "sudo -n /usr/bin/systemctl " + action + " " + unit;
        try {
            var r = exec.run(host, cmd, SSH_TIMEOUT);
            boolean ok = (r.exitCode() == 0);
            var probe = exec.run(host, "systemctl is-active " + unit, SSH_TIMEOUT);
            String status = probe.exitCode() == 0 ? trimOrUnknown(probe.stdout()) : "inactive";
            return new ClusterActionResult(
                    host,
                    unit,
                    action,
                    r.exitCode(),
                    truncate(safe(r.stdout())),
                    truncate(safe(r.stderr())),
                    status,
                    ok
            );
        } catch (Exception e) {
            return new ClusterActionResult(
                    host,
                    unit,
                    action,
                    255,
                    "",
                    e.getMessage(),
                    "error",
                    false
            );
        }
    }

    @Transactional
    protected void postActionDbWrite(short nodeId, String action, ClusterActionResult r) {
        try {
            boolean online = r.ok() && "active".equalsIgnoreCase(r.status());
            String logs = safe(r.stderr()) + "\n" + safe(r.stdout());
            repo.recordNodeStatus(
                    clusterName(),
                    nodeId,
                    online,
                    "api." + action,
                    truncate(logs)
            );
        } catch (Exception ignored) {
        }
    }

    private String clusterName() {
        return "kraft-cluster-1";
    }

    private boolean featureOn() {
        if (props != null && props.isEnabled()) return true;
        String envA = System.getenv("PROCESS_ENABLED");
        String envB = System.getenv("ONEINFRA_PROCESS_ENABLED");
        return "true".equalsIgnoreCase(envA) || "true".equalsIgnoreCase(envB);
    }

    // Fallback order: controllerResolved -> controller -> env var
    private String controllerHost() {
        if (props != null && props.getHosts() != null) {
            String resolved = props.getHosts().getControllerResolved();
            String plain    = props.getHosts().getController();
            if (!isBlank(resolved)) return resolved;
            if (!isBlank(plain))    return plain;
        }
        String env = System.getenv("PROCESS_HOSTS_CONTROLLER");
        return isBlank(env) ? null : env;
    }

    private String brokerHost(String id) {
        return (props != null
                && props.getHosts() != null
                && props.getHosts().getBrokers() != null)
                ? props.getHosts().getBrokers().get(id)
                : null;
    }

    private String controllerUnit() {
        return (props != null
                && props.getServices() != null
                && props.getServices().getControllerUnit() != null)
                ? props.getServices().getControllerUnit()
                : "kafka-controller";
    }

    private String brokerUnit() {
        return (props != null
                && props.getServices() != null
                && props.getServices().getBrokerUnit() != null)
                ? props.getServices().getBrokerUnit()
                : "kafka-broker";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String trimOrUnknown(String s) {
        if (s == null) return "unknown";
        String t = s.trim();
        int nl = t.indexOf('\n');
        if (nl > 0) t = t.substring(0, nl).trim();
        return t.isEmpty() ? "unknown" : t;
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= LOG_TRUNCATE ? s : s.substring(0, LOG_TRUNCATE) + "...(truncated)";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
