package com.navya.ai_ops_copilot.infra;

import java.time.Instant;

public record LogEntry(
        Instant timestamp,
        String service,
        String level,
        String message
) {}
