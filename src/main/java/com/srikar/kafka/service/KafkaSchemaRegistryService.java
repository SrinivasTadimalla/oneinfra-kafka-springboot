package com.srikar.kafka.service;

import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.db.KafkaSchemaSubjectRepository;
import com.srikar.kafka.db.KafkaSchemaVersionRepository;
import com.srikar.kafka.entity.KafkaClusterEntity;
import com.srikar.kafka.entity.KafkaSchemaSubjectEntity;
import com.srikar.kafka.entity.KafkaSchemaVersionEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaSchemaRegistryService {

    private final KafkaClusterRepository clusterRepo;
    private final KafkaSchemaSubjectRepository subjectRepo;
    private final KafkaSchemaVersionRepository versionRepo;

    @Transactional
    public KafkaSchemaSubjectEntity createOrGetSubject(
            UUID clusterId,
            String subject,
            String schemaType,
            String compatibility
    ) {
        require(clusterId != null, "clusterId is required");

        final String subjectFinal = requireText(subject, "subject is required");
        final String schemaTypeFinal = requireText(schemaType, "schemaType is required");
        final String compatibilityFinal = requireText(compatibility, "compatibility is required");

        KafkaClusterEntity cluster = clusterRepo.findById(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + clusterId));

        Optional<KafkaSchemaSubjectEntity> existing =
                subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal);

        if (existing.isPresent()) return existing.get();

        KafkaSchemaSubjectEntity created = KafkaSchemaSubjectEntity.builder()
                .id(UUID.randomUUID())
                .cluster(cluster)
                .subject(subjectFinal)
                .schemaType(schemaTypeFinal)
                .compatibility(compatibilityFinal)
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return subjectRepo.save(created);
    }

    @Transactional
    public KafkaSchemaVersionEntity registerNewVersion(
            UUID clusterId,
            String subject,
            String schemaType,
            String compatibility,
            String schemaRaw
    ) {
        require(clusterId != null, "clusterId is required");

        final String subjectFinal = requireText(subject, "subject is required");
        final String schemaTypeFinal = requireText(schemaType, "schemaType is required");
        final String compatibilityFinal = requireText(compatibility, "compatibility is required");
        final String schemaRawFinal = requireText(schemaRaw, "schemaRaw is required");

        KafkaSchemaSubjectEntity subj =
                createOrGetSubject(clusterId, subjectFinal, schemaTypeFinal, compatibilityFinal);

        final String canonical = canonicalize(schemaTypeFinal, schemaRawFinal);
        final String hash = sha256Hex(canonical);

        // Idempotency (simple): if same hash already exists, return latest.
        if (versionRepo.existsBySubjectIdAndSchemaHash(subj.getId(), hash)) {
            return versionRepo.findFirstBySubjectIdOrderByVersionDesc(subj.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Schema exists but cannot load latest version for subject: " + subjectFinal));
        }

        Integer max = versionRepo.findMaxVersion(subj.getId());
        final int nextVersion = (max == null ? 1 : max + 1);

        KafkaSchemaVersionEntity ver = KafkaSchemaVersionEntity.builder()
                .id(UUID.randomUUID())
                .subject(subj)
                .version(nextVersion)
                .schemaRaw(schemaRawFinal)
                .schemaCanonical(canonical)
                .schemaHash(hash)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        subj.setUpdatedAt(Instant.now());
        subjectRepo.save(subj);

        return versionRepo.save(ver);
    }

    @Transactional
    public KafkaSchemaVersionEntity getLatest(UUID clusterId, String subject) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");

        KafkaSchemaSubjectEntity subj = subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findFirstBySubjectIdOrderByVersionDesc(subj.getId())
                .orElseThrow(() -> new IllegalArgumentException("No versions found for subject: " + subjectFinal));
    }

    @Transactional
    public KafkaSchemaVersionEntity getByVersion(UUID clusterId, String subject, int version) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");
        require(version > 0, "version must be >= 1");

        KafkaSchemaSubjectEntity subj = subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findBySubjectIdAndVersion(subj.getId(), version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version not found: " + version + " for subject: " + subjectFinal));
    }

    /* =========================================================
       Canonicalization + Hash helpers
       ========================================================= */

    private String canonicalize(String schemaType, String schemaRaw) {
        String s = schemaRaw.replace("\r\n", "\n").trim();
        String[] lines = s.split("\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(rtrim(line)).append('\n');
        }
        return out.toString().trim();
    }

    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash schema", e);
        }
    }

    private static String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) i--;
        return s.substring(0, i + 1);
    }

    private static String requireText(String v, String msg) {
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(msg);
        return v.trim();
    }

    private static void require(boolean ok, String msg) {
        if (!ok) throw new IllegalArgumentException(msg);
    }
}
