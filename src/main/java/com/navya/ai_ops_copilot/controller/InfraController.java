package com.navya.ai_ops_copilot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.navya.ai_ops_copilot.infra.LogEntry;
import com.navya.ai_ops_copilot.infra.MetricSnapshot;
import com.navya.ai_ops_copilot.infra.Scenario;
import com.navya.ai_ops_copilot.infra.SimulatedInfrastructure;

@RestController
@RequestMapping("/api/infra")
public class InfraController {

    private final SimulatedInfrastructure infra;

    // Spring passes in the simulator it created (dependency injection)
    public InfraController(SimulatedInfrastructure infra) {
        this.infra = infra;
    }

    // GET /api/infra/services  → list all services
    @GetMapping("/services")
    public List<String> services() {
        return infra.listServices();
    }

    // GET /api/infra/checkout-service/metrics  → health numbers
    @GetMapping("/{service}/metrics")
    public MetricSnapshot metrics(@PathVariable String service) {
        return infra.getMetrics(service);
    }

    // GET /api/infra/checkout-service/logs?limit=5  → recent log messages
    @GetMapping("/{service}/logs")
    public List<LogEntry> logs(@PathVariable String service,
                               @RequestParam(defaultValue = "10") int limit) {
        return infra.getLogs(service, limit);
    }

    // GET /api/infra/checkout-service/version  → current code version
    @GetMapping("/{service}/version")
    public String version(@PathVariable String service) {
        return infra.currentVersion(service);
    }

    // POST /api/infra/checkout-service/chaos/MEMORY_LEAK  → break it
    @PostMapping("/{service}/chaos/{scenario}")
    public String chaos(@PathVariable String service, @PathVariable Scenario scenario) {
        infra.inject(service, scenario);
        return service + " is now " + scenario;
    }

    // POST /api/infra/checkout-service/restart  → fix 1
    @PostMapping("/{service}/restart")
    public String restart(@PathVariable String service) {
        return infra.restart(service);
    }

    // POST /api/infra/checkout-service/rollback  → fix 2
    @PostMapping("/{service}/rollback")
    public String rollback(@PathVariable String service) {
        return infra.rollback(service);
    }
}
