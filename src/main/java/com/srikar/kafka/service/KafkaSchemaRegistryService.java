// src/main/java/com/srikar/kafka/service/KafkaSchemaRegistryService.java
package com.srikar.kafka.service;

import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.db.KafkaSchemaSubjectRepository;
import com.srikar.kafka.db.KafkaSchemaVersionRepository;
import com.srikar.kafka.dto.schema.CompatibilityMode;
import com.srikar.kafka.dto.schema.SchemaPart;
import com.srikar.kafka.dto.schema.SchemaRegisterRequest;
import com.srikar.kafka.dto.schema.SchemaType;
import com.srikar.kafka.dto.schema.SchemaVersionDto;
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

    // -----------------------------
    // PUBLIC API (DTO RETURNING)
    // -----------------------------

    @Transactional
    public SchemaVersionDto getLatest(UUID clusterId, String subject) {
        KafkaSchemaVersionEntity ver = getLatestEntity(clusterId, subject);
        return toDto(ver);
    }

    @Transactional
    public List<SchemaVersionDto> getAllVersions(UUID clusterId, String subject) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");

        KafkaSchemaSubjectEntity subj = subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectFinal));

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
    // REGISTER (CREATE/UPDATE) SCHEMA VERSION  ✅ NEW
    // Topic Name Strategy:
    //   subject = <topic>-key OR <topic>-value
    // -------------------------------------------------------
    @Transactional
    public SchemaVersionDto register(SchemaRegisterRequest req) {

        require(req != null, "request body is required");

        final UUID clusterId = req.getClusterId();
        require(clusterId != null, "clusterId is required");

        final String topicNameFinal = requireText(req.getTopicName(), "topicName is required");
        require(req.getPart() != null, "part is required");
        require(req.getSchemaType() != null, "schemaType is required");

        final String schemaTextFinal = requireText(req.getSchemaText(), "schemaText is required");

        // Default compatibility if not provided
        final CompatibilityMode mode =
                (req.getCompatibility() == null ? CompatibilityMode.BACKWARD : req.getCompatibility());

        // ✅ TopicNameStrategy subject rule
        final String subject = toTopicSubject(topicNameFinal, req.getPart());

        // Uses your existing core write function
        KafkaSchemaVersionEntity saved = registerNewVersion(
                clusterId,
                subject,
                req.getSchemaType().name(),
                mode.name(),
                schemaTextFinal
        );

        return toDto(saved);
    }

    private static String toTopicSubject(String topicName, SchemaPart part) {
        // part is KEY or VALUE => "key" / "value"
        return topicName.trim() + "-" + part.name().toLowerCase();
    }

    // -----------------------------
    // YOUR EXISTING WRITE METHODS (ENTITY RETURNING OK internally)
    // -----------------------------

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

    // -----------------------------
    // INTERNAL ENTITY LOADERS
    // -----------------------------

    private KafkaSchemaVersionEntity getLatestEntity(UUID clusterId, String subject) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");

        KafkaSchemaSubjectEntity subj = subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findFirstBySubjectIdOrderByVersionDesc(subj.getId())
                .orElseThrow(() -> new IllegalArgumentException("No versions found for subject: " + subjectFinal));
    }

    private KafkaSchemaVersionEntity getByVersionEntity(UUID clusterId, String subject, int version) {
        require(clusterId != null, "clusterId is required");
        final String subjectFinal = requireText(subject, "subject is required");
        require(version > 0, "version must be >= 1");

        KafkaSchemaSubjectEntity subj = subjectRepo.findByClusterIdAndSubject(clusterId, subjectFinal)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectFinal));

        return versionRepo.findBySubjectIdAndVersion(subj.getId(), version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version not found: " + version + " for subject: " + subjectFinal));
    }

    // -----------------------------
    // MAPPER (Entity -> DTO)
    // -----------------------------

    private SchemaVersionDto toDto(KafkaSchemaVersionEntity e) {
        KafkaSchemaSubjectEntity s = e.getSubject(); // safe because we're inside @Transactional

        return SchemaVersionDto.builder()
                .id(e.getId())
                .clusterId(s.getClusterId())
                .subjectId(s.getId())
                .subject(s.getSubject())
                .version(e.getVersion())
                .schemaType(parseSchemaType(s.getSchemaType()))
                .compatibility(parseCompatibility(s.getCompatibility()))
                .schemaCanonical(e.getSchemaCanonical())
                .schemaRaw(e.getSchemaRaw())
                .schemaHash(e.getSchemaHash())
                .enabled(e.isEnabled())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static SchemaType parseSchemaType(String v) {
        if (v == null) return null;
        try { return SchemaType.valueOf(v.trim().toUpperCase()); }
        catch (Exception ex) { return null; }
    }

    private static CompatibilityMode parseCompatibility(String v) {
        if (v == null) return null;
        try { return CompatibilityMode.valueOf(v.trim().toUpperCase()); }
        catch (Exception ex) { return null; }
    }

    /* =========================================================
       Canonicalization + Hash helpers
       ========================================================= */

    private String canonicalize(String schemaType, String schemaRaw) {
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
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(msg);
        return v.trim();
    }

    private static void require(boolean ok, String msg) {
        if (!ok) throw new IllegalArgumentException(msg);
    }
}
