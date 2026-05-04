package com.dnb.kyc.agents.enrichment;

import com.dnb.kyc.model.EnrichedField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Enforcement Gate for Enrichment Policy.
 * 
 * This validator is called BEFORE any enriched field is added to the result.
 * It enforces EU AML and GDPR compliance as non-bypassable hard constraints.
 * 
 * If any check fails, a PolicyViolation is thrown and the field is rejected.
 */
public final class EnrichmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentValidator.class);

    private EnrichmentValidator() {
        // Static validator - no instantiation
    }

    /**
     * Enforces all policy constraints on an enriched field.
     * 
     * @param field The enriched field to validate
     * @throws PolicyViolation if any policy constraint is violated
     */
    public static void enforce(EnrichedField field) {
        logger.debug("Validating enriched field: {}", field.getFieldName());

        // 1. Source validation
        validateSource(field);

        // 2. No overwrite of customer data
        validateNoOverwrite(field);

        // 3. Confidence threshold
        validateConfidence(field);

        // 4. GDPR data minimization
        validateAllowedField(field);

        // 5. Forbidden fields check
        validateNotForbidden(field);

        // 6. Enrichment class consistency
        validateClassConsistency(field);

        // 7. Data freshness
        validateDataFreshness(field);

        // 8. Reliability score
        validateReliability(field);

        logger.debug("Field {} passed all policy checks", field.getFieldName());
    }

    /**
     * Validates the source is an approved enrichment source.
     */
    private static void validateSource(EnrichedField field) {
        if (field.getSourceType() == null) {
            throw new PolicyViolation(
                "Enrichment source not specified",
                "MISSING_SOURCE",
                field.getFieldName(),
                null
            );
        }

        if (!EnrichmentPolicy.isApprovedSource(field.getSourceType())) {
            throw new PolicyViolation(
                "Unapproved enrichment source: " + field.getSourceType(),
                "UNAPPROVED_SOURCE",
                field.getFieldName(),
                field.getSourceType().getDisplayName()
            );
        }
    }

    /**
     * Validates that customer-provided data is not being overwritten.
     */
    private static void validateNoOverwrite(EnrichedField field) {
        if (field.getOriginalValue() != null && 
            !field.getOriginalValue().toString().isBlank()) {
            
            // If original value exists and enriched value is different, that's an overwrite
            if (field.getEnrichedValue() != null && 
                !field.getOriginalValue().equals(field.getEnrichedValue())) {
                throw new PolicyViolation(
                    "Overwrite of customer data detected",
                    "DATA_OVERWRITE",
                    field.getFieldName(),
                    "Original: " + field.getOriginalValue() + " -> Enriched: " + field.getEnrichedValue()
                );
            }
        }
    }

    /**
     * Validates confidence meets minimum threshold.
     */
    private static void validateConfidence(EnrichedField field) {
        if (field.getConfidence() == null) {
            throw new PolicyViolation(
                "Confidence level not specified",
                "MISSING_CONFIDENCE",
                field.getFieldName(),
                null
            );
        }

        if (!field.getConfidence().isAllowedForEnrichment()) {
            throw new PolicyViolation(
                "Low confidence enrichment blocked: " + field.getConfidence(),
                "LOW_CONFIDENCE",
                field.getFieldName(),
                "Confidence: " + field.getConfidence() + ", Reliability: " + field.getReliabilityScore()
            );
        }
    }

    /**
     * Validates field is allowed under GDPR data minimization.
     */
    private static void validateAllowedField(EnrichedField field) {
        EnrichmentClass ec = field.getEnrichmentClass();
        if (ec == null) {
            ec = EnrichmentClass.HARD; // Default assumption
        }

        if (!EnrichmentPolicy.isAllowedField(field.getFieldName(), ec)) {
            throw new PolicyViolation(
                "GDPR data minimization violation: field not in allowed list",
                "DATA_MINIMIZATION",
                field.getFieldName(),
                "EnrichmentClass: " + ec
            );
        }
    }

    /**
     * Validates field is not in the forbidden list.
     */
    private static void validateNotForbidden(EnrichedField field) {
        if (EnrichmentPolicy.isForbiddenField(field.getFieldName())) {
            throw new PolicyViolation(
                "Forbidden compliance intelligence enrichment",
                "FORBIDDEN_FIELD",
                field.getFieldName(),
                "Risk/PEP/Sanction fields must come from Analysis Agent"
            );
        }
    }

    /**
     * Validates enrichment class is consistent with source type.
     */
    private static void validateClassConsistency(EnrichedField field) {
        if (field.getEnrichmentClass() == null || field.getSourceType() == null) {
            return; // Skip if not fully specified
        }

        if (field.getEnrichmentClass() == EnrichmentClass.PROHIBITED) {
            throw new PolicyViolation(
                "Cannot use PROHIBITED enrichment class",
                "PROHIBITED_CLASS",
                field.getFieldName(),
                null
            );
        }

        if (!EnrichmentPolicy.isConsistentClassification(field.getSourceType(), field.getEnrichmentClass())) {
            throw new PolicyViolation(
                "Enrichment class inconsistent with source type",
                "CLASS_MISMATCH",
                field.getFieldName(),
                "Source: " + field.getSourceType() + " vs Class: " + field.getEnrichmentClass()
            );
        }
    }

    /**
     * Validates data freshness is within acceptable range.
     */
    private static void validateDataFreshness(EnrichedField field) {
        if (field.getValidAsOf() == null) {
            // No timestamp means assume current
            return;
        }

        long daysSinceValid = Duration.between(field.getValidAsOf(), Instant.now()).toDays();
        if (daysSinceValid > EnrichmentPolicy.MAX_DATA_AGE_DAYS) {
            throw new PolicyViolation(
                "Enrichment data too stale: " + daysSinceValid + " days old",
                "STALE_DATA",
                field.getFieldName(),
                "ValidAsOf: " + field.getValidAsOf() + ", MaxAge: " + EnrichmentPolicy.MAX_DATA_AGE_DAYS + " days"
            );
        }
    }

    /**
     * Validates reliability score meets minimum threshold for enrichment class.
     */
    private static void validateReliability(EnrichedField field) {
        double reliability = field.getReliabilityScore();
        EnrichmentClass ec = field.getEnrichmentClass();
        
        if (ec == null) ec = EnrichmentClass.HARD;

        double minRequired = switch (ec) {
            case HARD -> EnrichmentPolicy.MIN_HARD_RELIABILITY;
            case SOFT, DERIVED -> EnrichmentPolicy.MIN_SOFT_RELIABILITY;
            case PROHIBITED -> 1.0; // Unreachable
        };

        if (reliability < minRequired) {
            throw new PolicyViolation(
                "Reliability score below minimum: " + reliability + " < " + minRequired,
                "LOW_RELIABILITY",
                field.getFieldName(),
                "Class: " + ec + ", Required: " + minRequired
            );
        }
    }

    /**
     * Validates the total number of enriched fields doesn't exceed limit.
     * 
     * @param currentCount Current count of enriched fields
     * @throws PolicyViolation if limit would be exceeded
     */
    public static void validateFieldCount(int currentCount) {
        if (currentCount >= EnrichmentPolicy.MAX_ENRICHED_FIELDS) {
            throw new PolicyViolation(
                "Maximum enriched fields exceeded: " + currentCount + " >= " + EnrichmentPolicy.MAX_ENRICHED_FIELDS,
                "OVER_ENRICHMENT",
                null,
                null
            );
        }
    }
}
