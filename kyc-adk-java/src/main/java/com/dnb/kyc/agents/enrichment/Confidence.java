package com.dnb.kyc.agents.enrichment;

/**
 * Confidence level for enriched data fields.
 * 
 * EU AML Compliance:
 * LOW confidence data is blocked by policy enforcement.
 * Only MEDIUM and HIGH confidence enrichments proceed to Guardrails.
 */
public enum Confidence {
    
    /**
     * High confidence - Data from authoritative source with recent verification.
     * Reliability score >= 0.85
     */
    HIGH(0.85, 1.0, true),
    
    /**
     * Medium confidence - Data from approved source but may be dated.
     * Reliability score 0.60 - 0.84
     */
    MEDIUM(0.60, 0.84, true),
    
    /**
     * Low confidence - Estimated or uncertain data.
     * Reliability score < 0.60
     * BLOCKED by EnrichmentValidator
     */
    LOW(0.0, 0.59, false);

    private final double minReliability;
    private final double maxReliability;
    private final boolean allowedForEnrichment;

    Confidence(double minReliability, double maxReliability, boolean allowedForEnrichment) {
        this.minReliability = minReliability;
        this.maxReliability = maxReliability;
        this.allowedForEnrichment = allowedForEnrichment;
    }

    public double getMinReliability() {
        return minReliability;
    }

    public double getMaxReliability() {
        return maxReliability;
    }

    /**
     * Returns true if this confidence level is permitted for enrichment.
     * LOW confidence is blocked by policy.
     */
    public boolean isAllowedForEnrichment() {
        return allowedForEnrichment;
    }

    /**
     * Derives confidence level from a reliability score.
     */
    public static Confidence fromReliabilityScore(double score) {
        if (score >= 0.85) return HIGH;
        if (score >= 0.60) return MEDIUM;
        return LOW;
    }
}
