package com.dnb.kyc.agents.guardrails.enrichment;

import java.util.ArrayList;
import java.util.List;

/**
 * Output model for Enrichment Guardrails Agent.
 * 
 * Contains the guardrails decision, violations, warnings,
 * and regulator-readable explanations.
 */
public class EnrichmentGuardrailsOutput {

    private String kycCaseId;

    /**
     * Final guardrails decision
     */
    private EnrichmentGuardrailsDecision decision;

    /**
     * Whether downstream agents may proceed automatically
     */
    private boolean allowedToProceed;

    /**
     * Whether a human must review before any decision
     */
    private boolean manualReviewRequired;

    /**
     * HARD violations → block pipeline
     */
    private List<String> violations = new ArrayList<>();

    /**
     * SOFT warnings → do not block, but surface
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * Sanitised scores safe for reuse
     */
    private double effectiveCompletionScore;
    private double effectiveEnrichmentRisk;

    /**
     * Regulator-readable explanation
     */
    private String regulatorNote;

    // Constructors
    public EnrichmentGuardrailsOutput() {}

    // Getters and Setters
    public String getKycCaseId() {
        return kycCaseId;
    }

    public void setKycCaseId(String kycCaseId) {
        this.kycCaseId = kycCaseId;
    }

    public EnrichmentGuardrailsDecision getDecision() {
        return decision;
    }

    public void setDecision(EnrichmentGuardrailsDecision decision) {
        this.decision = decision;
    }

    public boolean isAllowedToProceed() {
        return allowedToProceed;
    }

    public void setAllowedToProceed(boolean allowedToProceed) {
        this.allowedToProceed = allowedToProceed;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public void setManualReviewRequired(boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }

    public List<String> getViolations() {
        return violations;
    }

    public void setViolations(List<String> violations) {
        this.violations = violations;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public double getEffectiveCompletionScore() {
        return effectiveCompletionScore;
    }

    public void setEffectiveCompletionScore(double effectiveCompletionScore) {
        this.effectiveCompletionScore = effectiveCompletionScore;
    }

    public double getEffectiveEnrichmentRisk() {
        return effectiveEnrichmentRisk;
    }

    public void setEffectiveEnrichmentRisk(double effectiveEnrichmentRisk) {
        this.effectiveEnrichmentRisk = effectiveEnrichmentRisk;
    }

    public String getRegulatorNote() {
        return regulatorNote;
    }

    public void setRegulatorNote(String regulatorNote) {
        this.regulatorNote = regulatorNote;
    }

    // Convenience methods
    public void addViolation(String violation) {
        this.violations.add(violation);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
