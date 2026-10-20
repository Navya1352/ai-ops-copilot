package com.navya.ai_ops_copilot.infra;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
public class SimulatedInfrastructure {

    // The three fake services
    private static final List<String> SERVICES =
            List.of("checkout-service", "payment-service", "inventory-service");

    // What's wrong with each service right now, e.g. checkout-service -> HEALTHY
    private final Map<String, Scenario> state = new ConcurrentHashMap<>();

    // Which code version each service runs, e.g. checkout-service -> v1.4.0
    private final Map<String, String> versions = new ConcurrentHashMap<>();

    // Used later to add small random changes to numbers
    private final Random random = new Random();

    // Runs once when Spring creates this object: every service starts healthy
    public SimulatedInfrastructure() {
        for (String service : SERVICES) {
            state.put(service, Scenario.HEALTHY);
            versions.put(service, "v1.4.0");
        }
    }
    // Returns the list of service names
    public List<String> listServices() {
        return SERVICES;
    }
    // Break a service on purpose
    public void inject(String service, Scenario scenario) {
        requireService(service);
        state.put(service, scenario);
        if (scenario == Scenario.BAD_DEPLOY) {
            versions.put(service, "v1.5.0");   // a bad deploy means a new (buggy) version
        }
    }
        // Fill in the metrics form based on what's wrong with the service
    public MetricSnapshot getMetrics(String service) {
        requireService(service);
        return switch (state.get(service)) {
            //                          cpu  mem  latency  errors  dbUsed
            case HEALTHY           -> snapshot(service, 25, 40,  120,   0.1,   12);
            case DB_POOL_EXHAUSTED -> snapshot(service, 30, 45, 4800,   8.5,   50);
            case MEMORY_LEAK       -> snapshot(service, 70, 96,  900,   2.0,   15);
            case BAD_DEPLOY        -> snapshot(service, 35, 50,  300,  22.0,   10);
        };
    }

    // Build the MetricSnapshot, adding small random changes so it looks real
    private MetricSnapshot snapshot(String service, double cpu, double mem,
                                    double p99, double errorRate, int dbUsed) {
        return new MetricSnapshot(
                service,
                Math.min(jitter(cpu), 99.9),    // never above 99.9%
                Math.min(jitter(mem), 99.9),
                jitter(p99),
                errorRate,
                dbUsed,
                50,                             // max DB connections
                Instant.now());
    }

    // Change a number by up to 5% up or down
    private double jitter(double value) {
        double factor = 0.95 + random.nextDouble() * 0.1;   // between 0.95 and 1.05
        return Math.round(value * factor * 10) / 10.0;      // round to 1 decimal place
    }
        // Fill in the log messages based on what's wrong with the service
    public List<LogEntry> getLogs(String service, int limit) {
        requireService(service);

        // 1. Pick the log lines that match the problem
        List<String> lines = switch (state.get(service)) {
            case HEALTHY -> List.of(
                    "INFO Request completed in 110ms",
                    "INFO Health check OK");
            case DB_POOL_EXHAUSTED -> List.of(
                    "ERROR HikariPool-1 - Connection is not available, request timed out after 30000ms",
                    "WARN Slow query detected: SELECT * FROM orders (2900ms)",
                    "ERROR Failed to process checkout: SQLTransientConnectionException");
            case MEMORY_LEAK -> List.of(
                    "WARN GC overhead high: 45% of time spent in GC",
                    "ERROR java.lang.OutOfMemoryError: Java heap space",
                    "WARN Local cache grew to 2.1M entries");
            case BAD_DEPLOY -> List.of(
                    "ERROR NullPointerException at PaymentMapper.toDto(PaymentMapper.java:42)",
                    "ERROR 500 Internal Server Error on POST /api/payments",
                    "INFO Started version v1.5.0");
        };

        // 2. Turn them into LogEntry objects, each 15 seconds apart
        Instant now = Instant.now();
        return IntStream.range(0, limit)
                .mapToObj(i -> {
                    String line = lines.get(i % lines.size());     // cycle through the lines
                    int space = line.indexOf(' ');                 // position of the first space
                    String level = line.substring(0, space);       // "ERROR"
                    String message = line.substring(space + 1);    // everything after the space
                    return new LogEntry(now.minusSeconds(i * 15L), service, level, message);
                })
                .toList();
    }
        // Which version is the service running?
    public String currentVersion(String service) {
        requireService(service);
        return versions.get(service);
    }

    // Fix 1: Restart (turn it off and on again)
    public String restart(String service) {
        requireService(service);
        if (state.get(service) == Scenario.BAD_DEPLOY) {
            // Restart doesn't help: the buggy code is still there
            return service + " restarted, but errors continue: faulty version still deployed";
        }
        state.put(service, Scenario.HEALTHY);
        return service + " restarted successfully";
    }

    // Fix 2: Rollback (go back to the previous working version)
    public String rollback(String service) {
        requireService(service);
        versions.put(service, "v1.4.0");
        if (state.get(service) == Scenario.BAD_DEPLOY) {
            state.put(service, Scenario.HEALTHY);
        }
        return service + " rolled back to v1.4.0";
    }

    // Stops typos: throws an error for unknown service names
    private void requireService(String service) {
        if (!state.containsKey(service)) {
            throw new IllegalArgumentException("Unknown service: " + service);
        }
    }
}
