package com.dnb.kyc.agents.guardrails.enrichment;

import com.dnb.kyc.model.EnrichmentResult;
import com.dnb.kyc.model.EnrichedField;
import com.dnb.kyc.agents.AuditLoggerAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Enrichment Guardrails Agent 🛡️
 * 
 * Validates the output of the EnrichmentAgent before allowing
 * the pipeline to proceed to the Analysis phase.
 * 
 * Checks performed:
 * 1. Completion Quality Gate - minimum completion score
 * 2. Enrichment Risk Containment - maximum risk threshold
 * 3. Over-Enrichment Protection - field count limits
 * 4. Field Class Enforcement - allowed enrichment classes
 * 
 * Decisions:
 * - PROCEED: All checks passed
 * - PROCEED_WITH_MANUAL_REVIEW: Warnings present
 * - BLOCK_ENRICHMENT: Hard violations detected
 */
public class EnrichmentGuardrailsAgent {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentGuardrailsAgent.class);

    private final AuditLoggerAgent auditLogger;

    public EnrichmentGuardrailsAgent(AuditLoggerAgent auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Evaluate enrichment result against guardrails policies.
     * 
     * @param input The enrichment guardrails input containing the result to validate
     * @return EnrichmentGuardrailsOutput with decision and any violations/warnings
     */
    public EnrichmentGuardrailsOutput evaluate(EnrichmentGuardrailsInput input) {
        logger.info("Evaluating enrichment guardrails for case: {}", input.getKycCaseId());

        EnrichmentResult result = input.getEnrichmentResult();
        EnrichmentGuardrailsOutput output = new EnrichmentGuardrailsOutput();

        output.setKycCaseId(input.getKycCaseId());

        boolean hardViolation = false;
        boolean manualReview = false;

        // ----------------------------------------------------
        // 1. Completion Quality Gate
        // ----------------------------------------------------
        if (result.getCompletionScore() < EnrichmentGuardrailsPolicy.MIN_COMPLETION_SCORE) {
            output.addViolation(
                String.format("Completion score (%.2f) below minimum regulatory threshold (%.2f)",
                    result.getCompletionScore(),
                    EnrichmentGuardrailsPolicy.MIN_COMPLETION_SCORE)
            );
            hardViolation = true;
            logger.warn("Completion score violation: {} < {}", 
                result.getCompletionScore(), 
                EnrichmentGuardrailsPolicy.MIN_COMPLETION_SCORE);
        }

        // ----------------------------------------------------
        // 2. Enrichment Risk Containment
        // ----------------------------------------------------
        double enrichmentRisk = result.getEnrichmentRiskScore();
        if (enrichmentRisk > EnrichmentGuardrailsPolicy.MAX_ENRICHMENT_RISK) {
            output.addWarning(
                String.format("Elevated enrichment risk score (%.2f) exceeds threshold (%.2f)",
                    enrichmentRisk,
                    EnrichmentGuardrailsPolicy.MAX_ENRICHMENT_RISK)
            );
            manualReview = true;
            logger.warn("Enrichment risk warning: {} > {}", 
                enrichmentRisk, 
                EnrichmentGuardrailsPolicy.MAX_ENRICHMENT_RISK);
        }

        // ----------------------------------------------------
        // 3. Over-Enrichment Protection
        // ----------------------------------------------------
        List<EnrichedField> enrichedFields = result.getEnrichedFields();
        if (enrichedFields != null && enrichedFields.size() > EnrichmentGuardrailsPolicy.MAX_ENRICHED_FIELDS) {
            output.addViolation(
                String.format("Excessive enrichment volume detected: %d fields (max: %d)",
                    enrichedFields.size(),
                    EnrichmentGuardrailsPolicy.MAX_ENRICHED_FIELDS)
            );
            hardViolation = true;
            logger.warn("Over-enrichment violation: {} fields > {}", 
                enrichedFields.size(), 
                EnrichmentGuardrailsPolicy.MAX_ENRICHED_FIELDS);
        }

        // ----------------------------------------------------
        // 4. Field Class Enforcement
        // ----------------------------------------------------
        if (enrichedFields != null) {
            for (EnrichedField field : enrichedFields) {
                if (field.getEnrichmentClass() != null &&
                    !EnrichmentGuardrailsPolicy.ALLOWED_CLASSES.contains(field.getEnrichmentClass())) {
                    output.addViolation(
                        String.format("Disallowed enrichment class '%s' for field: %s",
                            field.getEnrichmentClass(),
                            field.getFieldName())
                    );
                    hardViolation = true;
                    logger.warn("Field class violation: {} has disallowed class {}", 
                        field.getFieldName(), 
                        field.getEnrichmentClass());
                }
            }
        }

        // ----------------------------------------------------
        // 5. Consistency Checks
        // ----------------------------------------------------
        if (result.getConsistencyIssues() != null && !result.getConsistencyIssues().isEmpty()) {
            for (String issue : result.getConsistencyIssues()) {
                output.addWarning("Consistency issue: " + issue);
            }
            manualReview = true;
            logger.info("Consistency issues detected: {}", result.getConsistencyIssues().size());
        }

        // ----------------------------------------------------
        // 6. Final Decision Assembly
        // ----------------------------------------------------
        if (hardViolation) {
            output.setDecision(EnrichmentGuardrailsDecision.BLOCK_ENRICHMENT);
            output.setAllowedToProceed(false);
            output.setManualReviewRequired(true);
            output.setRegulatorNote(
                "Enrichment blocked due to policy violations. " +
                "Violations: " + String.join("; ", output.getViolations())
            );
            logger.error("Enrichment BLOCKED for case {}: {} violations", 
                input.getKycCaseId(), 
                output.getViolations().size());

        } else if (manualReview) {
            output.setDecision(EnrichmentGuardrailsDecision.PROCEED_WITH_MANUAL_REVIEW);
            output.setAllowedToProceed(true);
            output.setManualReviewRequired(true);
            output.setRegulatorNote(
                "Enrichment allowed, but manual review required due to elevated uncertainty. " +
                "Warnings: " + String.join("; ", output.getWarnings())
            );
            logger.info("Enrichment PROCEED_WITH_REVIEW for case {}: {} warnings", 
                input.getKycCaseId(), 
                output.getWarnings().size());

        } else {
            output.setDecision(EnrichmentGuardrailsDecision.PROCEED);
            output.setAllowedToProceed(true);
            output.setManualReviewRequired(false);
            output.setRegulatorNote("Enrichment passed all guardrails checks");
            logger.info("Enrichment PROCEED for case {}", input.getKycCaseId());
        }

        // Set effective scores
        output.setEffectiveCompletionScore(result.getCompletionScore());
        output.setEffectiveEnrichmentRisk(enrichmentRisk);

        // Log to audit trail
        logToAudit(input, output);

        return output;
    }

    /**
     * Log guardrails evaluation to the audit trail
     */
    private void logToAudit(EnrichmentGuardrailsInput input, EnrichmentGuardrailsOutput output) {
        if (auditLogger != null) {
            auditLogger.log(
                "enrichment_guardrails",
                output.getDecision().name(),
                output.isAllowedToProceed() ? 1.0 - output.getEffectiveEnrichmentRisk() : 0.0,
                List.of(
                    "GUARDRAILS_EVALUATED",
                    output.getDecision().name(),
                    output.hasViolations() ? "HAS_VIOLATIONS" : "NO_VIOLATIONS",
                    output.hasWarnings() ? "HAS_WARNINGS" : "NO_WARNINGS"
                ),
                input.getKycCaseId(),
                output.getRegulatorNote(),
                Map.of(
                    "completionScore", output.getEffectiveCompletionScore(),
                    "enrichmentRisk", output.getEffectiveEnrichmentRisk(),
                    "violationCount", output.getViolations().size(),
                    "warningCount", output.getWarnings().size(),
                    "jurisdiction", input.getJurisdiction() != null ? input.getJurisdiction() : "UNKNOWN"
                )
            );
        }
    }
}
