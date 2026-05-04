package com.dnb.kyc.agents.guardrails.enrichment;

/**
 * Enumeration of possible guardrails decisions for enrichment validation.
 */
public enum EnrichmentGuardrailsDecision {
    /**
     * Enrichment passed all checks, proceed automatically
     */
    PROCEED,
    
    /**
     * Enrichment has warnings, proceed but require manual review
     */
    PROCEED_WITH_MANUAL_REVIEW,
    
    /**
     * Enrichment failed critical checks, block pipeline
     */
    BLOCK_ENRICHMENT
}
