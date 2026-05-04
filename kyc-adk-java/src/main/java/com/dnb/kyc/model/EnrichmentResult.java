package com.dnb.kyc.model;

import com.dnb.kyc.agents.enrichment.EnrichmentClass;
import com.dnb.kyc.agents.enrichment.Confidence;

import java.time.Instant;
import java.util.*;

/**
 * Result from Enrichment Agent data completion.
 * 
 * Guardrails-Safe Contract:
 * This result is validated by EnrichmentValidator before being passed
 * to the Guardrails Agent. All fields have verified source lineage.
 * 
 * Contains:
 * - Enriched fields with full data lineage
 * - Completion score (% of profile filled)
 * - Enrichment risk score (uncertainty measure)
 * - Consistency validation results
 * - Processing notes for audit trail
 */
public class EnrichmentResult {
    
    private Map<String, Object> enrichedData;
    private double completionScore;
    private double enrichmentRiskScore;
    private List<String> consistencyIssues;
    private List<String> sourcesUsed;
    private List<EnrichedField> enrichedFields;
    private List<String> policyViolations;
    private String notes;
    private Instant processedAt;

    public EnrichmentResult() {
        this.enrichedData = new HashMap<>();
        this.consistencyIssues = new ArrayList<>();
        this.sourcesUsed = new ArrayList<>();
        this.enrichedFields = new ArrayList<>();
        this.policyViolations = new ArrayList<>();
        this.processedAt = Instant.now();
    }

    // ======================== GETTERS AND SETTERS ========================

    public Map<String, Object> getEnrichedData() { return enrichedData; }
    public void setEnrichedData(Map<String, Object> enrichedData) { this.enrichedData = enrichedData; }
    public void addEnrichedField(String key, Object value) { this.enrichedData.put(key, value); }

    public double getCompletionScore() { return completionScore; }
    public void setCompletionScore(double completionScore) { this.completionScore = completionScore; }

    public double getEnrichmentRiskScore() { return enrichmentRiskScore; }
    public void setEnrichmentRiskScore(double enrichmentRiskScore) { this.enrichmentRiskScore = enrichmentRiskScore; }

    public List<String> getConsistencyIssues() { return consistencyIssues; }
    public void setConsistencyIssues(List<String> consistencyIssues) { this.consistencyIssues = consistencyIssues; }
    public void addConsistencyIssue(String issue) { this.consistencyIssues.add(issue); }

    public List<String> getSourcesUsed() { return sourcesUsed; }
    public void setSourcesUsed(List<String> sourcesUsed) { this.sourcesUsed = sourcesUsed; }
    public void addSourceUsed(String source) { this.sourcesUsed.add(source); }

    public List<EnrichedField> getEnrichedFields() { return enrichedFields; }
    public void setEnrichedFields(List<EnrichedField> enrichedFields) { this.enrichedFields = enrichedFields; }
    public void addEnrichedFieldRecord(EnrichedField field) { this.enrichedFields.add(field); }

    public List<String> getPolicyViolations() { return policyViolations; }
    public void setPolicyViolations(List<String> policyViolations) { this.policyViolations = policyViolations; }
    public void addPolicyViolation(String violation) { this.policyViolations.add(violation); }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    // ======================== HELPER METHODS ========================

    /**
     * Returns count of HARD enrichments (from authoritative sources).
     */
    public long getHardEnrichmentCount() {
        return enrichedFields.stream()
            .filter(f -> f.getEnrichmentClass() == EnrichmentClass.HARD)
            .count();
    }

    /**
     * Returns count of SOFT enrichments (estimations).
     */
    public long getSoftEnrichmentCount() {
        return enrichedFields.stream()
            .filter(f -> f.getEnrichmentClass() == EnrichmentClass.SOFT)
            .count();
    }

    /**
     * Returns average reliability score across all enrichments.
     */
    public double getAverageReliability() {
        if (enrichedFields.isEmpty()) return 0.0;
        return enrichedFields.stream()
            .mapToDouble(EnrichedField::getReliabilityScore)
            .average()
            .orElse(0.0);
    }

    /**
     * Returns true if all enrichments have HIGH confidence.
     */
    public boolean isHighConfidence() {
        return enrichedFields.stream()
            .allMatch(f -> f.getConfidence() == Confidence.HIGH);
    }

    /**
     * Returns true if there are no policy violations.
     */
    public boolean isPolicyCompliant() {
        return policyViolations.isEmpty();
    }

    /**
     * Returns true if the enrichment is ready for Guardrails processing.
     */
    public boolean isReadyForGuardrails() {
        return isPolicyCompliant() && 
               consistencyIssues.isEmpty() && 
               completionScore >= 0.5;
    }

    @Override
    public String toString() {
        return String.format("EnrichmentResult[fields=%d (HARD:%d, SOFT:%d), completion=%.2f, risk=%.2f, violations=%d]",
            enrichedFields.size(), getHardEnrichmentCount(), getSoftEnrichmentCount(),
            completionScore, enrichmentRiskScore, policyViolations.size());
    }
}
