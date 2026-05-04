package com.dnb.kyc.agents;

import com.dnb.kyc.model.*;
import com.dnb.kyc.agents.guardrails.enrichment.*;
import com.google.adk.agents.LlmAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * KYC Orchestrator
 * 
 * Coordinates the 4-agent pipeline for KYC processing:
 * 
 * 1. Guardrails Agent 🛡️ - Compliance validation & bias detection
 * 2. Enrichment Agent 📊 - Data completion from external sources
 * 3. Analysis Agent 🔍 - Fraud detection & risk scoring
 * 4. Audit Logger 🔐 - DORA-compliant immutable audit trail
 * 
 * Workflow:
 * INPUT → Guardrails → Enrichment → Analysis → Audit → OUTPUT
 */
public class KycOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(KycOrchestrator.class);

    private final GuardrailsAgent guardrailsAgent;
    private final EnrichmentAgent enrichmentAgent;
    private final EnrichmentGuardrailsAgent enrichmentGuardrailsAgent;
    private final AnalysisAgent analysisAgent;
    private final AuditLoggerAgent auditLogger;
    
    private final LlmAgent coordinatorAgent;

    public KycOrchestrator(
            GuardrailsAgent guardrailsAgent,
            EnrichmentAgent enrichmentAgent,
            EnrichmentGuardrailsAgent enrichmentGuardrailsAgent,
            AnalysisAgent analysisAgent,
            AuditLoggerAgent auditLogger) {
        
        this.guardrailsAgent = guardrailsAgent;
        this.enrichmentAgent = enrichmentAgent;
        this.enrichmentGuardrailsAgent = enrichmentGuardrailsAgent;
        this.analysisAgent = analysisAgent;
        this.auditLogger = auditLogger;
        
        // Build the coordinator agent that orchestrates the sub-agents
        this.coordinatorAgent = LlmAgent.builder()
            .name("kyc_coordinator")
            .description("Coordinates KYC processing across specialized agents")
            .model("gemini-2.0-flash")
            .instruction("""
                You are the KYC (Know Your Customer) Coordinator Agent.
                Your role is to orchestrate the KYC verification process by coordinating
                with specialized sub-agents:
                
                1. Guardrails Agent: Validates compliance and detects bias
                2. Enrichment Agent: Completes missing data from external sources
                3. Analysis Agent: Performs fraud detection and risk scoring
                
                Process flow:
                1. First, run guardrails validation
                2. If validation passes, enrich the profile data
                3. Perform fraud analysis on the enriched profile
                4. Generate final decision based on all agent outputs
                
                Ensure fair treatment and thorough documentation for DORA compliance.
                """)
            .subAgents(
                guardrailsAgent.getLlmAgent(),
                enrichmentAgent.getLlmAgent(),
                analysisAgent.getLlmAgent()
            )
            .build();
    }

    /**
     * Process a KYC application through the full pipeline
     * 
     * @param profile The KYC profile to process
     * @return Complete KYC result with all agent outputs and audit trail
     */
    public KycResult processKyc(KycProfile profile) {
        logger.info("Starting KYC processing for user: {}", profile.getUserId());
        long startTime = System.currentTimeMillis();
        
        String sessionId = UUID.randomUUID().toString();
        KycResult result = new KycResult();
        result.setSessionId(sessionId);
        
        try {
            // ==================== PHASE 1: GUARDRAILS ====================
            logger.info("[Phase 1/3] Running Guardrails validation...");
            GuardrailsResult guardrailsResult = guardrailsAgent.validate(profile);
            result.setGuardrailsResult(guardrailsResult);
            
            // If critical guardrails failure, stop early
            if (!guardrailsResult.isPassed() && 
                guardrailsResult.getRulesFired().contains("PEP_HIGH_RISK_COUNTRY")) {
                logger.warn("KYC rejected at guardrails: PEP high-risk country");
                result.setStatus(KycResult.Status.REJECTED);
                result.setSummary("Application rejected: High-risk country detected. Manual escalation required.");
                finalizeResult(result, startTime, profile.getUserId());
                return result;
            }
            
            // ==================== PHASE 2: ENRICHMENT ====================
            logger.info("[Phase 2/4] Running Data enrichment...");
            EnrichmentResult enrichmentResult = enrichmentAgent.enrich(profile);
            result.setEnrichmentResult(enrichmentResult);
            
            // ==================== PHASE 2.5: ENRICHMENT GUARDRAILS ====================
            logger.info("[Phase 2.5/4] Running Enrichment Guardrails...");
            EnrichmentGuardrailsInput guardrailsInput = new EnrichmentGuardrailsInput(
                profile.getUserId(),
                enrichmentResult,
                profile.getNationality() != null ? profile.getNationality() : "NL",
                "NATURAL_PERSON",
                "1.0"
            );
            EnrichmentGuardrailsOutput enrichmentGuardrailsOutput = enrichmentGuardrailsAgent.evaluate(guardrailsInput);
            
            // Log guardrails violations if any
            if (!enrichmentGuardrailsOutput.getViolations().isEmpty()) {
                logger.warn("Enrichment guardrails violations: {}", enrichmentGuardrailsOutput.getViolations());
                auditLogger.log(
                    "enrichment_guardrails",
                    enrichmentGuardrailsOutput.getDecision().name(),
                    enrichmentGuardrailsOutput.getDecision() == EnrichmentGuardrailsDecision.PROCEED ? 0.0 : 0.5,
                    List.of("ENRICHMENT_GUARDRAILS_" + enrichmentGuardrailsOutput.getDecision().name()),
                    profile.getUserId(),
                    enrichmentGuardrailsOutput.getRegulatorNote(),
                    Map.of("violations", enrichmentGuardrailsOutput.getViolations(),
                           "warnings", enrichmentGuardrailsOutput.getWarnings())
                );
            }
            
            // Handle guardrails decision
            if (!enrichmentGuardrailsOutput.isAllowedToProceed()) {
                logger.warn("KYC blocked by enrichment guardrails");
                result.setStatus(KycResult.Status.REJECTED);
                result.setSummary("Application blocked: Enrichment guardrails failed. " + enrichmentGuardrailsOutput.getRegulatorNote());
                finalizeResult(result, startTime, profile.getUserId());
                return result;
            }
            
            if (enrichmentGuardrailsOutput.isManualReviewRequired()) {
                logger.info("Enrichment guardrails flagged for manual review");
            }
            
            // ==================== PHASE 3: ANALYSIS ====================
            logger.info("[Phase 3/4] Running Fraud analysis...");
            FraudRiskReport fraudRiskReport = analysisAgent.analyze(
                profile, enrichmentResult, guardrailsResult);
            result.setFraudRiskReport(fraudRiskReport);

            // Store legacy AnalysisResult for backward compatibility
            AnalysisResult analysisResult = analysisAgent.analyze(
                profile, enrichmentResult, guardrailsResult, true);
            result.setAnalysisResult(analysisResult);

            // ==================== FINAL DECISION ====================
            result.setStatus(KycResult.fromFraudAction(fraudRiskReport.recommendedAction()));
            result.setSummary(generateSummary(result, profile));
            
            logger.info("KYC processing complete: status={}", result.getStatus());
            
        } catch (Exception e) {
            logger.error("Error during KYC processing", e);
            result.setStatus(KycResult.Status.MANUAL_REVIEW);
            result.setSummary("Processing error occurred. Manual review required. Error: " + e.getMessage());
            
            auditLogger.log(
                "orchestrator",
                "ERROR",
                0.0,
                List.of("PROCESSING_ERROR"),
                profile.getUserId(),
                "Error during processing: " + e.getMessage(),
                Map.of("errorType", e.getClass().getSimpleName())
            );
        }
        
        finalizeResult(result, startTime, profile.getUserId());
        return result;
    }

    /**
     * Process a batch of KYC applications
     */
    public List<KycResult> processBatch(List<KycProfile> profiles) {
        logger.info("Starting batch KYC processing: {} profiles", profiles.size());
        
        List<KycResult> results = new ArrayList<>();
        for (KycProfile profile : profiles) {
            try {
                results.add(processKyc(profile));
            } catch (Exception e) {
                logger.error("Error processing profile in batch: {}", profile.getUserId(), e);
                KycResult errorResult = new KycResult();
                errorResult.setStatus(KycResult.Status.MANUAL_REVIEW);
                errorResult.setSummary("Batch processing error: " + e.getMessage());
                results.add(errorResult);
            }
        }
        
        logger.info("Batch processing complete: {} results", results.size());
        return results;
    }

    /**
     * Get the audit trail for compliance reporting
     */
    public List<AuditEntry> getAuditTrail() {
        return auditLogger.getAuditTrail();
    }

    /**
     * Verify audit trail integrity
     */
    public boolean verifyAuditIntegrity() {
        return auditLogger.verifyIntegrity();
    }

    /**
     * Get compliance statistics
     */
    public Map<String, Object> getComplianceStatistics() {
        return auditLogger.getComplianceStatistics();
    }

    // ==================== PRIVATE METHODS ====================

    private void finalizeResult(KycResult result, long startTime, String userId) {
        long processingTime = System.currentTimeMillis() - startTime;
        result.setProcessingTimeMs(processingTime);
        result.setAuditTrail(auditLogger.getEntriesForUser(userId));
        
        // Log final orchestrator decision
        auditLogger.log(
            "orchestrator",
            result.getStatus().name(),
            result.getStatus() == KycResult.Status.APPROVED ? 1.0 : 0.5,
            List.of("PIPELINE_COMPLETE", result.getStatus().name()),
            userId,
            result.getSummary(),
            Map.of(
                "processingTimeMs", processingTime,
                "sessionId", result.getSessionId()
            )
        );
    }

    private String generateSummary(KycResult result, KycProfile profile) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("KYC Processing Summary for ").append(profile.getFullName()).append("\n");
        summary.append("=".repeat(50)).append("\n\n");
        
        // Status
        summary.append("Final Status: ").append(result.getStatus()).append("\n\n");
        
        // Guardrails
        GuardrailsResult gr = result.getGuardrailsResult();
        if (gr != null) {
            summary.append("Guardrails: ").append(gr.isPassed() ? "✓ PASSED" : "⚠ FLAGGED").append("\n");
            if (gr.getBiasAssessment() != null && gr.getBiasAssessment().isBiasDetected()) {
                summary.append("  → Bias indicators detected (score: ")
                       .append(String.format("%.2f", gr.getBiasAssessment().getBiasScore()))
                       .append(")\n");
            }
        }
        
        // Enrichment
        EnrichmentResult er = result.getEnrichmentResult();
        if (er != null) {
            summary.append("Data Completeness: ")
                   .append(String.format("%.0f%%", er.getCompletionScore() * 100))
                   .append("\n");
        }
        
        // Analysis
        AnalysisResult ar = result.getAnalysisResult();
        if (ar != null) {
            summary.append("Risk Level: ").append(ar.getRiskLevel()).append("\n");
            summary.append("Fraud Score: ").append(String.format("%.2f", ar.getFraudScore())).append("\n");
            
            if (!ar.getRedFlags().isEmpty()) {
                summary.append("Red Flags:\n");
                for (String flag : ar.getRedFlags()) {
                    summary.append("  - ").append(flag).append("\n");
                }
            }
        }
        
        // Action
        summary.append("\nRecommended Action: ");
        summary.append(switch (result.getStatus()) {
            case APPROVED -> "✅ Auto-approve - Low risk applicant";
            case MANUAL_REVIEW -> "🟡 Manual review required - Medium risk or bias indicators";
            case ESCALATED -> "⚠️ Escalate to compliance team - High risk detected";
            case REJECTED -> "🔴 Application blocked - Critical risk factors";
        });
        
        return summary.toString();
    }

    public LlmAgent getCoordinatorAgent() {
        return coordinatorAgent;
    }
}
