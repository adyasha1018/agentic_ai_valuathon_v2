package com.dnb.kyc.agents.guardrails.enrichment;

import com.dnb.kyc.agents.enrichment.EnrichmentClass;
import java.util.Set;

/**
 * Policy constants for Enrichment Guardrails.
 * 
 * These thresholds define regulatory compliance requirements
 * for the enrichment process.
 */
public final class EnrichmentGuardrailsPolicy {

    /**
     * Minimum acceptable completion score (0-1).
     * Below this threshold, enrichment is considered inadequate.
     */
    public static final double MIN_COMPLETION_SCORE = 0.75;

    /**
     * Maximum acceptable enrichment risk score (0-1).
     * Above this threshold, manual review is required.
     */
    public static final double MAX_ENRICHMENT_RISK = 0.30;

    /**
     * Maximum number of fields that can be enriched in a single pass.
     * Prevents over-reliance on external data sources.
     */
    public static final int MAX_ENRICHED_FIELDS = 8;

    /**
     * Allowed enrichment classes that can be used.
     */
    public static final Set<EnrichmentClass> ALLOWED_CLASSES =
        Set.of(EnrichmentClass.HARD, EnrichmentClass.SOFT, EnrichmentClass.DERIVED);

    private EnrichmentGuardrailsPolicy() {
        // Prevent instantiation
    }
}
