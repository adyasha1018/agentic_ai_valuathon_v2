package com.dnb.kyc.agents.enrichment;

/**
 * Classification of enriched data by derivation method.
 * 
 * Industry Standard:
 * - HARD: Direct lookup from authoritative source
 * - SOFT: Statistical estimation or inference
 * - DERIVED: Computed from other enriched fields
 * - PROHIBITED: Fields that cannot be enriched (risk, PEP, sanctions)
 */
public enum EnrichmentClass {
    
    /**
     * HARD enrichment: Direct lookup from authoritative source.
     * Examples: KvK company name, registered address, UBO data
     * 
     * Characteristics:
     * - Source has legal authority
     * - Data is verifiable
     * - Highest reliability
     */
    HARD("Hard Enrichment", true, 1.0),
    
    /**
     * SOFT enrichment: Statistical estimation or inference.
     * Examples: Estimated income range, employment status inference
     * 
     * Characteristics:
     * - Based on demographic models
     * - May require manual verification
     * - Lower reliability
     */
    SOFT("Soft Enrichment", true, 0.7),
    
    /**
     * DERIVED enrichment: Computed from other enriched fields.
     * Examples: Business sector from registration, address validation
     * 
     * Characteristics:
     * - Depends on other enrichments
     * - Reliability inherited from sources
     */
    DERIVED("Derived Enrichment", true, 0.6),
    
    /**
     * PROHIBITED: Fields that cannot be enriched by policy.
     * Examples: Risk scores, PEP status, sanctions flags
     * 
     * These are exclusively determined by the Analysis Agent.
     */
    PROHIBITED("Prohibited Enrichment", false, 0.0);

    private final String displayName;
    private final boolean allowedForEnrichment;
    private final double maxReliabilityWeight;

    EnrichmentClass(String displayName, boolean allowedForEnrichment, double maxReliabilityWeight) {
        this.displayName = displayName;
        this.allowedForEnrichment = allowedForEnrichment;
        this.maxReliabilityWeight = maxReliabilityWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAllowedForEnrichment() {
        return allowedForEnrichment;
    }

    /**
     * Maximum reliability weight for this enrichment class.
     * Used to cap reliability scores in risk calculations.
     */
    public double getMaxReliabilityWeight() {
        return maxReliabilityWeight;
    }
}
