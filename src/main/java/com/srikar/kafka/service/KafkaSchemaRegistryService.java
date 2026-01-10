// src/main/java/com/srikar/kafka/service/KafkaSchemaRegistryService.java
package com.srikar.kafka.service;

import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.db.KafkaSchemaSubjectRepository;
import com.srikar.kafka.db.KafkaSchemaVersionRepository;
import com.srikar.kafka.dto.schema.*;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaSchemaRegistryService {

    private final KafkaClusterRepository clusterRepo;
    private final KafkaSchemaSubjectRepository subjectRepo;
    private final KafkaSchemaVersionRepository versionRepo;

    /* =========================================================
       PUBLIC API
       ========================================================= */

    @Transactional
    public SchemaVersionDto register(SchemaRegisterRequest req) {

        require(req != null, "request body is required");

        final UUID clusterId = req.getClusterId();
        require(clusterId != null, "clusterId is required");

        final String subject =
                requireText(req.getSubject(), "schema name (subject) is required");

        require(req.getSchemaType() != null, "schemaType is required");

        final String schemaText =
                requireText(req.getSchemaText(), "schemaText is required");

        final CompatibilityMode compatibility =
                (req.getCompatibility() == null
                        ? CompatibilityMode.BACKWARD
                        : req.getCompatibility());

        KafkaSchemaVersionEntity saved = registerNewVersion(
                clusterId,
                subject,
                req.getSchemaType().name(),
                compatibility.name(),
                schemaText
        );

        return toDto(saved);
    }

    @Transactional
    public SchemaVersionDto getLatest(UUID clusterId, String subject) {
        KafkaSchemaVersionEntity ver = getLatestEntity(clusterId, subject);
        return toDto(ver);
    }

    @Transactional
    public List<SchemaVersionDto> getAllVersions(UUID clusterId, String subject) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");

        KafkaSchemaSubjectEntity subj =
                subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findBySubjectIdOrderByVersionDesc(subj.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SchemaVersionDto getByVersion(UUID clusterId, String subject, int version) {
        KafkaSchemaVersionEntity ver = getByVersionEntity(clusterId, subject, version);
        return toDto(ver);
    }

    // -------------------------------------------------------
    // ✅ LIST SUBJECTS (for UI dropdown)  (IDEAL)
    // -------------------------------------------------------
    @Transactional
    public List<SchemaSubjectSummaryDto> listSubjects(UUID clusterId) {
        require(clusterId != null, "clusterId is required");

        // Friendly error if cluster missing
        clusterRepo.findById(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + clusterId));

        return subjectRepo.findAllByClusterIdOrderBySubjectAsc(clusterId)
                .stream()
                .map(this::toSubjectSummaryDto)
                .toList();
    }

    // -------------------------------------------------------
    // ✅ DELETE SUBJECT (HARD DELETE)
    // -------------------------------------------------------
    @Transactional
    public void deleteSubject(UUID clusterId, String subject) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");

        KafkaSchemaSubjectEntity subj =
                subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Subject not found: " + subjectFinal));

        // delete children first (FK)
        versionRepo.deleteBySubjectId(subj.getId());

        // delete subject
        subjectRepo.delete(subj);
    }

    /* =========================================================
       WRITE PATH (INTERNAL)
       ========================================================= */

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
                .orElseThrow(() ->
                        new IllegalArgumentException("Cluster not found: " + clusterId));

        Optional<KafkaSchemaSubjectEntity> existingOpt =
                subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal);

        // ✅ IDEAL: if subject exists, keep metadata up-to-date (compatibility/schemaType)
        if (existingOpt.isPresent()) {
            KafkaSchemaSubjectEntity existing = existingOpt.get();
            existing.setSchemaType(schemaTypeFinal);
            existing.setCompatibility(compatibilityFinal);
            existing.setUpdatedAt(Instant.now());
            return subjectRepo.save(existing);
        }

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

        // if same schema already exists (by hash) -> return latest
        if (versionRepo.existsBySubjectIdAndSchemaHash(subj.getId(), hash)) {
            return versionRepo.findFirstBySubjectIdOrderByVersionDesc(subj.getId())
                    .orElseThrow(() ->
                            new IllegalStateException("Schema exists but latest version missing"));
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
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        // keep subject updated
        subj.setUpdatedAt(Instant.now());
        subjectRepo.save(subj);

        return versionRepo.save(ver);
    }

    /* =========================================================
       READ HELPERS
       ========================================================= */

    private KafkaSchemaVersionEntity getLatestEntity(UUID clusterId, String subject) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");

        KafkaSchemaSubjectEntity subj =
                subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findFirstBySubjectIdOrderByVersionDesc(subj.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("No versions found for subject: " + subjectFinal));
    }

    private KafkaSchemaVersionEntity getByVersionEntity(
            UUID clusterId,
            String subject,
            int version
    ) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");
        require(version > 0, "version must be >= 1");

        KafkaSchemaSubjectEntity subj =
                subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findBySubjectIdAndVersion(subj.getId(), version)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Version not found: " + version + " for subject: " + subjectFinal));
    }

    /* =========================================================
       ENTITY → DTO
       ========================================================= */

    private SchemaVersionDto toDto(KafkaSchemaVersionEntity e) {
        KafkaSchemaSubjectEntity s = e.getSubject();

        return SchemaVersionDto.builder()
                .id(e.getId())
                .clusterId(s.getClusterId())                 // ensure helper exists on entity
                .subjectId(s.getId())
                .subject(s.getSubject())
                .version(e.getVersion())
                .schemaType(parseSchemaType(s.getSchemaType()))
                .compatibility(parseCompatibility(s.getCompatibility()))
                .schemaCanonical(e.getSchemaCanonical())
                .schemaRaw(e.getSchemaRaw())
                .schemaHash(e.getSchemaHash())
                // ✅ ideal: enabled is subject-level for your UI toggle
                .enabled(s.isEnabled())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private SchemaSubjectSummaryDto toSubjectSummaryDto(KafkaSchemaSubjectEntity s) {
        Integer latest = versionRepo.findMaxVersion(s.getId());
        int latestVersion = (latest == null ? 0 : latest);

        return SchemaSubjectSummaryDto.builder()
                .subjectId(s.getId())
                .clusterId(s.getClusterId())                 // ensure helper exists on entity
                .subject(s.getSubject())
                .schemaType(parseSchemaType(s.getSchemaType()))
                .compatibility(parseCompatibility(s.getCompatibility()))
                .latestVersion(latestVersion)
                .enabled(s.isEnabled())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private static SchemaType parseSchemaType(String v) {
        try { return SchemaType.valueOf(v.trim().toUpperCase()); }
        catch (Exception ex) { return null; }
    }

    private static CompatibilityMode parseCompatibility(String v) {
        try { return CompatibilityMode.valueOf(v.trim().toUpperCase()); }
        catch (Exception ex) { return null; }
    }

    /* =========================================================
       CANONICALIZATION + HASHING
       ========================================================= */

    private String canonicalize(String schemaType, String schemaRaw) {
        // light normalization (works for AVRO JSON text)
        String s = schemaRaw.replace("\r\n", "\n").trim();
        String[] lines = s.split("\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) out.append(rtrim(line)).append('\n');
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
        if (v == null || v.trim().isEmpty())
            throw new IllegalArgumentException(msg);
        return v.trim();
    }

    private static void require(boolean ok, String msg) {
        if (!ok) throw new IllegalArgumentException(msg);
    }
}
