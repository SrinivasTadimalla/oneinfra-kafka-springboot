package com.srikar.kafka.domain;

public record ClusterActionResult(
        String  host,
        String  unit,
        String  action,
        int     exitCode,
        String  stdout,
        String  stderr,
        String  status,
        boolean ok
) {}
