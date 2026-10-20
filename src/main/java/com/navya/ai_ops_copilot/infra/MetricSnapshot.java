package com.navya.ai_ops_copilot.infra;

import java.time.Instant;

public record MetricSnapshot(
        String service,
        double cpuPercent,
        double memoryPercent,
        double p99LatencyMs,
        double errorRatePercent,
        int dbConnectionsUsed,
        int dbConnectionsMax,
        Instant timestamp
) {}