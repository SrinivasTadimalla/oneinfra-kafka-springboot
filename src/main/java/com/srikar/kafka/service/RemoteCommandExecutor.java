package com.srikar.kafka.service;

import com.srikar.kafka.config.ProcessProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class RemoteCommandExecutor {

    public record CommandResult(int exitCode, String stdout, String stderr) {}

    private final ProcessProperties cfg;

    public RemoteCommandExecutor(ProcessProperties cfg) {
        this.cfg = cfg;
    }

    public CommandResult run(String host, String command, Duration timeout) throws Exception {
        var ssh = cfg.getSsh();

        List<String> cmd = new ArrayList<>();
        cmd.add("ssh");
        cmd.add("-o"); cmd.add("BatchMode=yes");
        cmd.add("-o"); cmd.add("StrictHostKeyChecking=yes");
        cmd.add("-o"); cmd.add("ConnectTimeout=" + Math.max(1, ssh.getConnectTimeoutMs() / 1000));
        cmd.add("-i"); cmd.add(ssh.getPrivateKeyPath());
        cmd.add("-p"); cmd.add(String.valueOf(ssh.getPort()));
        cmd.add(ssh.getUser() + "@" + host);
        cmd.add(command);

        Process p = new ProcessBuilder(cmd).start();

        Callable<String> outReader = () -> readAll(p.getInputStream());
        Callable<String> errReader = () -> readAll(p.getErrorStream());

        ExecutorService es = Executors.newFixedThreadPool(2);
        Future<String> outF = es.submit(outReader);
        Future<String> errF = es.submit(errReader);

        int limitMs = (int) Math.min(timeout.toMillis(), ssh.getCommandTimeoutMs());
        boolean finished = p.waitFor(limitMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            es.shutdownNow();
            throw new TimeoutException("SSH command timed out after " + limitMs + " ms");
        }

        String out = getSafe(outF);
        String err = getSafe(errF);
        es.shutdown();
        return new CommandResult(p.exitValue(), out, err);
    }

    private static String readAll(java.io.InputStream is) throws Exception {
        try (var br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static String getSafe(Future<String> f) {
        try {
            return f.get(100, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            return "";
        }
    }
}
