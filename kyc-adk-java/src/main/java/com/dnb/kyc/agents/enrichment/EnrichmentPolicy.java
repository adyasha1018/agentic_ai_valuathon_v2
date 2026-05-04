package com.dnb.kyc.agents.enrichment;

import java.util.Set;

/**
 * Policy-as-Code for KYC Enrichment.
 * 
 * NON-BYPASSABLE enforcement of EU AML and GDPR regulations.
 * These rules cannot be overridden by the LLM or any runtime configuration.
 * 
 * Compliance Framework:
 * - EU Anti-Money Laundering Directive (AMLD6)
 * - General Data Protection Regulation (GDPR)
 * - DNB (De Nederlandsche Bank) Guidelines
 * - DORA (Digital Operational Resilience Act)
 */
public final class EnrichmentPolicy {

    private EnrichmentPolicy() {
        // Static policy class - no instantiation
    }

    // ======================== SOURCE CONTROLS ========================

    /**
     * Approved data sources for enrichment.
     * ONLY these sources may be used for data lookup.
     */
    public static final Set<SourceType> ALLOWED_HARD_SOURCES = Set.of(
        SourceType.GOVERNMENT_REGISTRY,
        SourceType.OFFICIAL_COMPANY_REGISTER,
        SourceType.PUBLIC_UBO_REGISTER,
        SourceType.OFFICIAL_ADDRESS_DATABASE,
        SourceType.BRIS_REGISTRY,
        SourceType.COUNTRY_REGISTRY
    );

    /**
     * Soft sources permitted for estimation (with lower reliability).
     */
    public static final Set<SourceType> ALLOWED_SOFT_SOURCES = Set.of(
        SourceType.DEMOGRAPHIC_MODEL,
        SourceType.AGE_BASED_ESTIMATION
    );

    // ======================== FIELD CONTROLS ========================

    /**
     * Fields permitted for HARD enrichment from official sources.
     * GDPR Article 5(1)(c): Data minimization principle.
     */
    public static final Set<String> ALLOWED_HARD_FIELDS = Set.of(
        "legal_name",
        "registered_address",
        "business_registration_number",
        "legal_entity_status",
        "ubo_name",
        "ubo_ownership_percentage",
        "date_of_incorporation",
        "legal_form",
        "country_of_registration",
        "trade_name",
        "countryRisk",
        "currency",
        "avgGdpPerCapita",
        "employmentVerified"
    );

    /**
     * Fields permitted for SOFT enrichment (estimations).
     */
    public static final Set<String> ALLOWED_SOFT_FIELDS = Set.of(
        "estimatedIncomeRange",
        "estimatedEmploymentStatus"
    );

    /**
     * Fields that are STRICTLY PROHIBITED from enrichment.
     * These are exclusively determined by the Analysis Agent.
     */
    public static final Set<String> FORBIDDEN_FIELD_PATTERNS = Set.of(
        "riskScore",
        "fraudScore",
        "pep",
        "sanction",
        "sourceOfFunds",
        "watchlist",
        "adverse_media"
    );

    // ======================== CONFIDENCE THRESHOLDS ========================

    /**
     * Minimum confidence level for enrichment to proceed.
     * LOW confidence enrichments are blocked.
     */
    public static final Confidence MIN_CONFIDENCE = Confidence.MEDIUM;

    /**
     * Minimum reliability score for HARD enrichments.
     */
    public static final double MIN_HARD_RELIABILITY = 0.80;

    /**
     * Minimum reliability score for SOFT enrichments.
     */
    public static final double MIN_SOFT_RELIABILITY = 0.60;

    // ======================== ENRICHMENT LIMITS ========================

    /**
     * Maximum number of fields that can be enriched per profile.
     * Prevents over-enrichment and data minimization compliance.
     */
    public static final int MAX_ENRICHED_FIELDS = 10;

    /**
     * Maximum staleness of enrichment data (in days).
     * Data older than this must be re-verified.
     */
    public static final int MAX_DATA_AGE_DAYS = 90;

    // ======================== POLICY CHECKS ========================

    /**
     * Checks if a field name matches any forbidden pattern.
     * 
     * @param fieldName The field name to check
     * @return true if the field is forbidden for enrichment
     */
    public static boolean isForbiddenField(String fieldName) {
        if (fieldName == null) return true;
        
        String lowerField = fieldName.toLowerCase();
        return FORBIDDEN_FIELD_PATTERNS.stream()
            .anyMatch(pattern -> lowerField.contains(pattern.toLowerCase()));
    }

    /**
     * Checks if a source type is approved for enrichment.
     * 
     * @param sourceType The source to validate
     * @return true if the source is approved
     */
    public static boolean isApprovedSource(SourceType sourceType) {
        return ALLOWED_HARD_SOURCES.contains(sourceType) || 
               ALLOWED_SOFT_SOURCES.contains(sourceType);
    }

    /**
     * Checks if a field is allowed for the given enrichment class.
     * 
     * @param fieldName The field name
     * @param enrichmentClass The enrichment class (HARD/SOFT)
     * @return true if the field is allowed
     */
    public static boolean isAllowedField(String fieldName, EnrichmentClass enrichmentClass) {
        if (isForbiddenField(fieldName)) return false;
        
        return switch (enrichmentClass) {
            case HARD -> ALLOWED_HARD_FIELDS.contains(fieldName);
            case SOFT -> ALLOWED_SOFT_FIELDS.contains(fieldName);
            case DERIVED -> ALLOWED_HARD_FIELDS.contains(fieldName) || 
                           ALLOWED_SOFT_FIELDS.contains(fieldName);
            case PROHIBITED -> false;
        };
    }

    /**
     * Validates enrichment class matches source type.
     * 
     * @param sourceType The source used
     * @param enrichmentClass The claimed enrichment class
     * @return true if they are consistent
     */
    public static boolean isConsistentClassification(SourceType sourceType, EnrichmentClass enrichmentClass) {
        if (sourceType.isHardSource()) {
            return enrichmentClass == EnrichmentClass.HARD || 
                   enrichmentClass == EnrichmentClass.DERIVED;
        } else {
            return enrichmentClass == EnrichmentClass.SOFT;
        }
    }
}
