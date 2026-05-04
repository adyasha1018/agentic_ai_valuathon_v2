package com.dnb.kyc.model;

import com.dnb.kyc.agents.enrichment.SourceType;
import com.dnb.kyc.agents.enrichment.Confidence;
import com.dnb.kyc.agents.enrichment.EnrichmentClass;

import java.time.Instant;

/**
 * Represents a single enriched field with full data lineage.
 * 
 * Industry-grade model including:
 * - Source traceability (type + name)
 * - Confidence level with reliability score
 * - Data freshness timestamp
 * - Enrichment classification (HARD/SOFT/DERIVED)
 * 
 * Compliant with:
 * - EU AML Directive (source traceability)
 * - GDPR (data minimization, purpose limitation)
 * - DORA (audit trail requirements)
 */
public class EnrichedField {

    private String fieldName;
    private Object originalValue;
    private Object enrichedValue;

    // Source lineage
    private SourceType sourceType;
    private String sourceName;

    // Confidence metrics
    private Confidence confidence;
    private double reliabilityScore; // 0.0 – 1.0

    // Data freshness
    private Instant validAsOf;

    // Classification
    private EnrichmentClass enrichmentClass;

    // Constructors
    public EnrichedField() {
        this.validAsOf = Instant.now();
        this.reliabilityScore = 1.0;
        this.confidence = Confidence.HIGH;
    }

    /**
     * Quick constructor for basic enrichment.
     */
    public EnrichedField(String fieldName, Object enrichedValue, String sourceName, EnrichmentClass enrichmentClass) {
        this();
        this.fieldName = fieldName;
        this.enrichedValue = enrichedValue;
        this.sourceName = sourceName;
        this.enrichmentClass = enrichmentClass;
        this.sourceType = enrichmentClass == EnrichmentClass.SOFT ? 
            SourceType.DEMOGRAPHIC_MODEL : SourceType.GOVERNMENT_REGISTRY;
    }

    /**
     * Full constructor with all lineage data.
     */
    public EnrichedField(String fieldName, Object originalValue, Object enrichedValue,
                        SourceType sourceType, String sourceName,
                        Confidence confidence, double reliabilityScore,
                        Instant validAsOf, EnrichmentClass enrichmentClass) {
        this.fieldName = fieldName;
        this.originalValue = originalValue;
        this.enrichedValue = enrichedValue;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.confidence = confidence;
        this.reliabilityScore = reliabilityScore;
        this.validAsOf = validAsOf;
        this.enrichmentClass = enrichmentClass;
    }

    // Getters and Setters
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(Object originalValue) {
        this.originalValue = originalValue;
    }

    public Object getEnrichedValue() {
        return enrichedValue;
    }

    public void setEnrichedValue(Object enrichedValue) {
        this.enrichedValue = enrichedValue;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * @deprecated Use getSourceName() instead
     */
    @Deprecated
    public String getSource() {
        return sourceName;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    public double getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(double reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
        // Auto-update confidence from reliability
        this.confidence = Confidence.fromReliabilityScore(reliabilityScore);
    }

    public Instant getValidAsOf() {
        return validAsOf;
    }

    public void setValidAsOf(Instant validAsOf) {
        this.validAsOf = validAsOf;
    }

    public EnrichmentClass getEnrichmentClass() {
        return enrichmentClass;
    }

    public void setEnrichmentClass(EnrichmentClass enrichmentClass) {
        this.enrichmentClass = enrichmentClass;
    }

    /**
     * Returns true if this is a HARD enrichment from an authoritative source.
     */
    public boolean isHardEnrichment() {
        return enrichmentClass == EnrichmentClass.HARD;
    }

    /**
     * Returns true if this enrichment has sufficient reliability for production use.
     */
    public boolean isReliable() {
        return reliabilityScore >= 0.60 && confidence.isAllowedForEnrichment();
    }

    @Override
    public String toString() {
        return String.format("EnrichedField[%s=%s, source=%s/%s, confidence=%s(%.2f), class=%s]",
            fieldName, enrichedValue, sourceType, sourceName, confidence, reliabilityScore, enrichmentClass);
    }
}
