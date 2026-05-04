package com.dnb.kyc.controller;

import com.dnb.kyc.agents.AuditLoggerAgent;
import com.dnb.kyc.agents.KycOrchestrator;
import com.dnb.kyc.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * KYC REST API Controller
 * 
 * Provides REST endpoints for KYC processing.
 * Compatible with both frontend applications and direct API calls.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class KycController {

    private static final Logger logger = LoggerFactory.getLogger(KycController.class);

    private final KycOrchestrator orchestrator;
    private final AuditLoggerAgent auditLogger;

    public KycController(KycOrchestrator orchestrator, AuditLoggerAgent auditLogger) {
        this.orchestrator = orchestrator;
        this.auditLogger = auditLogger;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "KYC Multi-Agent System");
        health.put("version", "1.0.0");
        health.put("framework", "Google ADK Java");
        health.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(health);
    }

    /**
     * Process a KYC application
     * 
     * POST /api/kyc/process
     */
    @PostMapping("/kyc/process")
    public ResponseEntity<Map<String, Object>> processKyc(@RequestBody KycProfile profile) {
        logger.info("Received KYC processing request for: {}", profile.getFullName());
        
        // Assign user ID if not provided
        if (profile.getUserId() == null) {
            profile.setUserId("USER_" + UUID.randomUUID().toString().substring(0, 8));
        }
        
        KycResult result = orchestrator.processKyc(profile);
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("auditTrail", result.getAuditTrail());
        response.put("processingTimeMs", result.getProcessingTimeMs());
        response.put("success", true);
        
        logger.info("KYC processing complete: status={}, time={}ms", 
            result.getStatus(), result.getProcessingTimeMs());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Process batch KYC applications
     * 
     * POST /api/kyc/batch
     */
    @PostMapping("/kyc/batch")
    public ResponseEntity<Map<String, Object>> processBatch(@RequestBody List<KycProfile> profiles) {
        logger.info("Received batch KYC request: {} profiles", profiles.size());
        
        List<KycResult> results = orchestrator.processBatch(profiles);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("totalProcessed", results.size());
        response.put("approved", results.stream().filter(r -> r.getStatus() == KycResult.Status.APPROVED).count());
        response.put("manualReview", results.stream().filter(r -> r.getStatus() == KycResult.Status.MANUAL_REVIEW).count());
        response.put("rejected", results.stream().filter(r -> r.getStatus() == KycResult.Status.REJECTED).count());
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get audit trail
     * 
     * GET /api/audit-trail
     */
    @GetMapping("/audit-trail")
    public ResponseEntity<Map<String, Object>> getAuditTrail(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String agentName) {
        
        List<AuditEntry> entries;
        
        if (userId != null) {
            entries = auditLogger.getEntriesForUser(userId);
        } else if (agentName != null) {
            entries = auditLogger.getEntriesForAgent(agentName);
        } else {
            entries = auditLogger.getAuditTrail();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("entries", entries);
        response.put("count", entries.size());
        response.put("chainIntact", auditLogger.verifyIntegrity());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Verify audit trail integrity
     * 
     * POST /api/audit-trail/verify
     */
    @PostMapping("/audit-trail/verify")
    public ResponseEntity<Map<String, Object>> verifyAuditTrail() {
        boolean intact = auditLogger.verifyIntegrity();
        
        Map<String, Object> response = new HashMap<>();
        response.put("chainIntact", intact);
        response.put("verified", true);
        response.put("timestamp", java.time.Instant.now().toString());
        response.put("compliance", "DORA");
        
        if (!intact) {
            response.put("warning", "Audit trail integrity compromised! Investigation required.");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get compliance statistics
     * 
     * GET /api/compliance/stats
     */
    @GetMapping("/compliance/stats")
    public ResponseEntity<Map<String, Object>> getComplianceStats() {
        Map<String, Object> stats = auditLogger.getComplianceStatistics();
        stats.put("compliance", "DORA");
        stats.put("retentionYears", 6);
        return ResponseEntity.ok(stats);
    }

    /**
     * Export audit trail (DORA compliance)
     * 
     * GET /api/audit-trail/export
     */
    @GetMapping(value = "/audit-trail/export", produces = "application/json")
    public ResponseEntity<String> exportAuditTrail() {
        String json = auditLogger.exportToJson();
        return ResponseEntity.ok(json);
    }

    /**
     * API documentation / info endpoint
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "KYC Multi-Agent System API");
        info.put("version", "1.0.0");
        info.put("framework", "Google ADK Java 1.2.0");
        info.put("compliance", List.of("DORA", "DNB Guidelines", "GDPR", "AML/CFT"));
        info.put("agents", List.of(
            Map.of("name", "guardrails", "description", "Compliance validation & bias detection"),
            Map.of("name", "enrichment", "description", "Data completion from external sources"),
            Map.of("name", "analysis", "description", "Fraud detection & risk scoring"),
            Map.of("name", "audit", "description", "DORA-compliant immutable audit trail")
        ));
        info.put("endpoints", List.of(
            Map.of("path", "POST /api/kyc/process", "description", "Process single KYC application"),
            Map.of("path", "POST /api/kyc/batch", "description", "Process batch KYC applications"),
            Map.of("path", "GET /api/audit-trail", "description", "Get audit trail entries"),
            Map.of("path", "POST /api/audit-trail/verify", "description", "Verify audit chain integrity"),
            Map.of("path", "GET /api/compliance/stats", "description", "Get compliance statistics"),
            Map.of("path", "GET /api/audit-trail/export", "description", "Export audit trail (DORA)")
        ));
        return ResponseEntity.ok(info);
    }
}
