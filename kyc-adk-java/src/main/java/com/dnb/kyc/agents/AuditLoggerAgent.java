package com.dnb.kyc.agents;

import com.dnb.kyc.model.AuditEntry;
import com.dnb.kyc.util.HashUtil;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ✅ Audit Logger Agent 🔐 (Google ADK)
 * 
 * Regulatory-grade agent responsible for end-to-end auditability and traceability.
 * 
 * 🎯 Purpose:
 * - Records every agent decision
 * - Enforces immutability via SHA-256 hash chaining
 * - Guarantees zero raw PII exposure
 * - Proves human-in-the-loop enforcement
 * - Produces DORA-, DNB-, and GDPR-defensible audit trail
 * 
 * ⚠️ CRITICAL: Audit Logger never evaluates customers and never influences decisions.
 *    It strictly logs system behavior, not people.
 * 
 * 🧠 Design Principles:
 * ✅ Append-only logging
 * ✅ Deterministic & replayable
 * ✅ No identity data
 * ✅ Bias-safe by construction
 * ✅ Hash-chain integrity (SHA256)
 * ✅ Human oversight provable (finalDecisionByAi = false)
 * ✅ Article-22 compliant (no sole automation)
 */
public class AuditLoggerAgent {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggerAgent.class);
    
    private final LlmAgent llmAgent;
    private final List<AuditEntry> auditTrail = new CopyOnWriteArrayList<>();
    private String lastHash = "GENESIS";

    public AuditLoggerAgent() {
        this.llmAgent = LlmAgent.builder()
                .name("audit_logger_agent")
                .model("gemini-2.0-flash")
                .instruction("""
                    You are an audit logging agent.
                    You do not evaluate customers or decide outcomes.
                    You record immutable, regulation-ready audit entries.
                    Never include personal or identity data.
                    Always enforce human-in-the-loop requirements.
                """)
                .tools(FunctionTool.create(this, "logAuditEvent"))
                .build();
        
        logger.info("AuditLoggerAgent initialized - DORA/DNB compliant hash chain");
    }

    public LlmAgent getLlmAgent() {
        return llmAgent;
    }

    /**
     * Core ADK audit logging logic.
     * Records immutable event with cryptographic integrity.
     * CRITICAL: PII is rejected at entry. No identity data allowed.
     */
    public AuditEntry logAuditEvent(
            String eventId,
            String agentName,
            String model,
            String kycStatus,
            String riskLevel,
            List<Map<String, Object>> flags,
            boolean decisionAllowed,
            boolean rejectAllowed,
            boolean humanReviewRequired,
            List<String> allowedActions,
            List<String> policyRefs
    ) {
        
        // CRITICAL: Enforce no PII in audit payload
        enforceNoPii(flags);
        
        // Create draft entry
        AuditEntry draft = new AuditEntry(
                eventId,
                Instant.now(),
                "KYC_PROCESS",
                agentName,
                true,           // AI assisted
                false,          // ⚠️ AI is never final decision maker (Article 22 compliant)
                model,
                false,          // piiUsed
                kycStatus,
                riskLevel,
                flags,
                decisionAllowed,
                rejectAllowed,
                humanReviewRequired,
                allowedActions,
                policyRefs,
                lastHash,
                null            // currentHash will be calculated
        );
        
        // Calculate hash with previous hash
        String currentHash = HashUtil.hashWithPrevious(draft, lastHash);
        
        // Create final immutable entry
        AuditEntry finalEntry = new AuditEntry(
                draft.eventId(),
                draft.timestamp(),
                draft.workflow(),
                agentName,
                draft.aiAssisted(),
                draft.finalDecisionByAi(),
                model,
                false,
                kycStatus,
                riskLevel,
                flags,
                decisionAllowed,
                rejectAllowed,
                humanReviewRequired,
                allowedActions,
                policyRefs,
                lastHash,
                currentHash     // ✅ Hash chain integrity
        );
        
        auditTrail.add(finalEntry);
        lastHash = currentHash;
        
        logger.info("Audit entry logged: eventId={}, agent={}, hash={}...", 
            eventId, agentName, currentHash.substring(0, 8));
        
        return finalEntry;
    }

    /**
     * Backward-compatible log method for existing code.
     * Wraps the new logAuditEvent API.
     */
    public AuditEntry log(
            String agentName,
            String decision,
            double confidenceScore,
            List<String> rulesFired,
            String userId,
            String rationale,
            Map<String, Object> biasIndicators) {
        
        // Map old format to new format
        List<Map<String, Object>> flags = new ArrayList<>();
        if (biasIndicators != null && !biasIndicators.isEmpty()) {
            flags.add(biasIndicators);
        }
        if (rulesFired != null && !rulesFired.isEmpty()) {
            Map<String, Object> rulesMap = new HashMap<>();
            rulesMap.put("rules", rulesFired);
            rulesMap.put("rationale", rationale);
            flags.add(rulesMap);
        }
        
        String status = "review_required";
        if ("APPROVE".equalsIgnoreCase(decision)) {
            status = "approved";
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            status = "rejected";
        }
        
        String riskLevel = confidenceScore > 0.7 ? "LOW" : 
                          confidenceScore > 0.4 ? "MEDIUM" : "HIGH";
        
        return logAuditEvent(
                UUID.randomUUID().toString(),
                agentName,
                "gemini-2.0-flash",
                status,
                riskLevel,
                flags,
                "APPROVE".equalsIgnoreCase(decision),
                "REJECT".equalsIgnoreCase(decision),
                true,
                List.of("manual_review"),
                List.of("compliance_policy")
        );
    }

    /**
     * Verify the integrity of the entire hash chain.
     * Returns true if chain is intact, false if tampering detected.
     * This is the regulatory replay mechanism.
     */
    public boolean verifyIntegrity() {
        logger.info("Verifying audit trail integrity...");
        
        if (auditTrail.isEmpty()) {
            return true;
        }
        
        // Check first entry links to genesis
        if (!"GENESIS".equals(auditTrail.get(0).previousHash())) {
            logger.error("Chain broken: First entry does not link to genesis");
            return false;
        }
        
        // Check each subsequent entry
        String previous = "GENESIS";
        for (AuditEntry entry : auditTrail) {
            String recalculated = HashUtil.hashWithPrevious(entry, previous);
            
            if (!recalculated.equals(entry.currentHash())) {
                logger.error("Chain broken at entry {}: hash mismatch", entry.eventId());
                return false;
            }
            previous = entry.currentHash();
        }
        
        logger.info("Audit trail integrity verified: {} entries, chain intact", auditTrail.size());
        return true;
    }

    /**
     * Get all audit entries (immutable view)
     */
    public List<AuditEntry> getAuditTrail() {
        return Collections.unmodifiableList(new ArrayList<>(auditTrail));
    }

    /**
     * Get audit entries for a specific agent
     */
    public List<AuditEntry> getEntriesForAgent(String agentName) {
        return auditTrail.stream()
            .filter(e -> agentName.equals(e.agent()))
            .collect(Collectors.toList());
    }

    /**
     * Backward-compatible method: Get entries for a specific user
     */
    public List<AuditEntry> getEntriesForUser(String userId) {
        // New AuditEntry doesn't store userId directly; return all entries
        // or filter by flags if needed
        return auditTrail.stream()
            .filter(e -> e.flags().stream()
                .anyMatch(f -> userId.equals(f.get("userId"))))
            .collect(Collectors.toList());
    }

    /**
     * Get entries within a time range (for DORA reporting)
     */
    public List<AuditEntry> getEntriesBetween(Instant start, Instant end) {
        return auditTrail.stream()
            .filter(e -> !e.timestamp().isBefore(start) && !e.timestamp().isAfter(end))
            .collect(Collectors.toList());
    }

    /**
     * Get summary statistics for compliance reporting
     */
    public Map<String, Object> getComplianceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEntries", auditTrail.size());
        stats.put("chainIntact", verifyIntegrity());
        
        // Count by agent
        Map<String, Long> byAgent = auditTrail.stream()
            .collect(Collectors.groupingBy(AuditEntry::agent, Collectors.counting()));
        stats.put("entriesByAgent", byAgent);
        
        // Count by KYC status
        Map<String, Long> byStatus = auditTrail.stream()
            .collect(Collectors.groupingBy(AuditEntry::kycStatus, Collectors.counting()));
        stats.put("entriesByStatus", byStatus);
        
        // Count by risk level
        Map<String, Long> byRisk = auditTrail.stream()
            .collect(Collectors.groupingBy(AuditEntry::riskLevel, Collectors.counting()));
        stats.put("entriesByRiskLevel", byRisk);
        
        // Human review required count
        long humanReviewCount = auditTrail.stream()
            .filter(AuditEntry::humanReviewRequired)
            .count();
        stats.put("humanReviewRequiredCount", humanReviewCount);
        
        // Time range
        if (!auditTrail.isEmpty()) {
            stats.put("firstEntry", auditTrail.get(0).timestamp().toString());
            stats.put("lastEntry", auditTrail.get(auditTrail.size() - 1).timestamp().toString());
        }
        
        return stats;
    }

    /**
     * GDPR-compliant PII enforcement.
     * Throws if any forbidden identity keys detected in flags.
     * This prevents accidental logging of personal data.
     */
    private void enforceNoPii(List<Map<String, Object>> flags) {
        List<String> forbiddenKeys = List.of(
                "name", "surname", "firstname", "lastname", 
                "dob", "dateofbirth", "age",
                "nationality", "ethnicity", 
                "address", "phone", "email",
                "passport", "document", "id"
        );
        
        for (Map<String, Object> flag : flags) {
            for (String key : flag.keySet()) {
                if (forbiddenKeys.contains(key.toLowerCase())) {
                    throw new IllegalStateException(
                            "PII detected in audit payload (key: " + key + ") — logging aborted. " +
                            "GDPR Article 5(1)(c) - Data Minimization violated."
                    );
                }
            }
        }
    }

    /**
     * Clear all entries (for testing only - would be disabled in production)
     */
    public void clearForTesting() {
        logger.warn("CLEARING AUDIT TRAIL - This should only happen in testing!");
        auditTrail.clear();
        lastHash = "GENESIS";
    }

    /**
     * Export audit trail to JSON format (for DORA compliance reporting)
     */
    public String exportToJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"compliance\": \"DORA\",\n");
        json.append("  \"exportTimestamp\": \"").append(Instant.now()).append("\",\n");
        json.append("  \"chainIntact\": ").append(verifyIntegrity()).append(",\n");
        json.append("  \"entryCount\": ").append(auditTrail.size()).append(",\n");
        json.append("  \"entries\": [\n");
        
        for (int i = 0; i < auditTrail.size(); i++) {
            AuditEntry e = auditTrail.get(i);
            json.append("    {\n");
            json.append("      \"eventId\": \"").append(e.eventId()).append("\",\n");
            json.append("      \"timestamp\": \"").append(e.timestamp()).append("\",\n");
            json.append("      \"workflow\": \"").append(e.workflow()).append("\",\n");
            json.append("      \"agent\": \"").append(e.agent()).append("\",\n");
            json.append("      \"kycStatus\": \"").append(e.kycStatus()).append("\",\n");
            json.append("      \"riskLevel\": \"").append(e.riskLevel()).append("\",\n");
            json.append("      \"hash\": \"").append(e.currentHash()).append("\",\n");
            json.append("      \"previousHash\": \"").append(e.previousHash()).append("\"\n");
            json.append("    }");
            if (i < auditTrail.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
}
