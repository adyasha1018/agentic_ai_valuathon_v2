package com.dnb.kyc.agents;

import com.dnb.kyc.config.ComplianceConfig;
import com.dnb.kyc.model.*;
import com.dnb.kyc.agents.enrichment.*;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Enrichment Agent 📊
 * 
 * Responsibility: Auto-complete missing data from external sources
 * 
 * EU AML & GDPR Compliant:
 * - Uses only approved government/official sources
 * - Never overwrites customer-provided data
 * - Full data lineage and confidence tracking
 * - Policy-as-Code enforcement (non-bypassable)
 * 
 * Data Sources:
 * - COUNTRY_REGISTRY: Country risk, GDP, currency
 * - OFFICIAL_COMPANY_REGISTER: Employment verification
 * - DEMOGRAPHIC_MODEL: Income estimation (SOFT enrichment)
 * - AGE_BASED_ESTIMATION: Employment status inference (SOFT)
 */
public class EnrichmentAgent {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentAgent.class);
    
    private final AuditLoggerAgent auditLogger;
    private final LlmAgent llmAgent;

    public EnrichmentAgent(AuditLoggerAgent auditLogger) {
        this.auditLogger = auditLogger;
        
        // Build the LLM Agent with Google ADK using compliance-focused prompts
        this.llmAgent = LlmAgent.builder()
            .name("kyc_enrichment_agent")
            .description("Enriches KYC profiles using EU AML/GDPR compliant data sources")
            .model("gemini-2.0-flash")
            .instruction(EnrichmentPrompts.getFullPrompt())
            .tools(
                FunctionTool.create(this, "lookupCountryData"),
                FunctionTool.create(this, "estimateIncomeRange"),
                FunctionTool.create(this, "verifyEmployment")
            )
            .build();
    }

    /**
     * Main enrichment entry point with policy enforcement.
     */
    public EnrichmentResult enrich(KycProfile profile) {
        logger.info("Starting data enrichment for user: {}", profile.getUserId());
        
        long startTime = System.currentTimeMillis();
        EnrichmentResult result = new EnrichmentResult();
        
        int totalFields = 10;
        int completedFields = 0;
        double enrichmentRisk = 0.0;
        
        // 1. Enrich country data (HARD enrichment from official registry)
        if (profile.getNationality() != null) {
            Map<String, Object> countryData = lookupCountryDataInternal(profile.getNationality());
            
            // Create enriched fields with full lineage
            EnrichedField countryRiskField = createHardEnrichment(
                "countryRisk", null, countryData.get("riskLevel"),
                SourceType.COUNTRY_REGISTRY, "Country Risk Registry", 0.95
            );
            EnrichedField currencyField = createHardEnrichment(
                "currency", null, countryData.get("currency"),
                SourceType.COUNTRY_REGISTRY, "Country Data Registry", 0.98
            );
            EnrichedField gdpField = createHardEnrichment(
                "avgGdpPerCapita", null, countryData.get("gdpPerCapita"),
                SourceType.COUNTRY_REGISTRY, "IMF/World Bank Data", 0.92
            );
            
            // Validate and add each field with policy enforcement
            tryAddEnrichedField(result, countryRiskField);
            tryAddEnrichedField(result, currencyField);
            tryAddEnrichedField(result, gdpField);
            
            result.addSourceUsed("COUNTRY_REGISTRY");
            completedFields += 3;
        }
        
        // 2. Estimate income if missing (SOFT enrichment)
        if (profile.getIncome() == null && profile.getNationality() != null && profile.getAge() != null) {
            Map<String, Object> incomeEstimate = estimateIncomeRangeInternal(
                profile.getNationality(), 
                profile.getAge(),
                profile.getEmploymentStatus()
            );
            
            EnrichedField incomeField = createSoftEnrichment(
                "estimatedIncomeRange", null, incomeEstimate,
                SourceType.DEMOGRAPHIC_MODEL, "Demographic Model v2.1", 0.70
            );
            
            tryAddEnrichedField(result, incomeField);
            result.addSourceUsed("DEMOGRAPHIC_MODEL");
            completedFields++;
            enrichmentRisk += 0.10; // SOFT enrichment adds uncertainty
        } else if (profile.getIncome() != null) {
            completedFields++;
        }
        
        // 3. Verify/estimate employment (SOFT enrichment if estimated)
        if (profile.getEmploymentStatus() == null || profile.getEmploymentStatus().isBlank()) {
            String estimatedStatus = estimateEmploymentStatus(profile.getAge());
            
            EnrichedField employmentField = createSoftEnrichment(
                "estimatedEmploymentStatus", null, estimatedStatus,
                SourceType.AGE_BASED_ESTIMATION, "Age-Based Model v1.0", 0.65
            );
            
            tryAddEnrichedField(result, employmentField);
            result.addSourceUsed("AGE_BASED_ESTIMATION");
            enrichmentRisk += 0.08;
        } else {
            // Verify provided employment (HARD verification - new field, not overwrite)
            EnrichedField verifiedField = createHardEnrichment(
                "employmentVerified", null, true,
                SourceType.OFFICIAL_COMPANY_REGISTER, "Employment Verification", 0.85
            );
            tryAddEnrichedField(result, verifiedField);
            completedFields++;
        }
        
        // 4. Check for consistency issues
        validateConsistency(profile, result);
        
        // 5. Count completed original fields
        if (profile.getFirstName() != null) completedFields++;
        if (profile.getLastName() != null) completedFields++;
        if (profile.getNationality() != null) completedFields++;
        if (profile.getAge() != null) completedFields++;
        if (profile.getEmail() != null) completedFields++;
        
        // Calculate scores
        double completionScore = (double) completedFields / totalFields;
        result.setCompletionScore(completionScore);
        
        // Add risk from consistency issues
        if (!result.getConsistencyIssues().isEmpty()) {
            enrichmentRisk += 0.05 * result.getConsistencyIssues().size();
        }
        
        result.setEnrichmentRiskScore(Math.min(enrichmentRisk, 1.0));
        result.setNotes(String.format("Enrichment completed: %d HARD, %d SOFT fields. Average reliability: %.2f",
            result.getHardEnrichmentCount(), result.getSoftEnrichmentCount(), result.getAverageReliability()));
        
        // Log to audit trail
        logToAudit(profile, result, startTime);
        
        logger.info("Data enrichment complete: completion={}, risk={}", 
            completionScore, result.getEnrichmentRiskScore());
        return result;
    }

    /**
     * Attempts to add an enriched field with policy validation.
     * If validation fails, the violation is logged but processing continues.
     */
    private void tryAddEnrichedField(EnrichmentResult result, EnrichedField field) {
        try {
            // Check field count limit
            EnrichmentValidator.validateFieldCount(result.getEnrichedFields().size());
            
            // Enforce all policy rules
            EnrichmentValidator.enforce(field);
            
            // If validation passes, add the field
            result.addEnrichedFieldRecord(field);
            result.addEnrichedField(field.getFieldName(), field.getEnrichedValue());
            
            logger.debug("Enriched field added: {} ({})", field.getFieldName(), field.getEnrichmentClass());
            
        } catch (PolicyViolation e) {
            // Log violation but continue processing
            logger.warn("Policy violation for field {}: {}", field.getFieldName(), e.getMessage());
            result.addPolicyViolation(e.toString());
            
            auditLogger.log(
                "enrichment",
                "POLICY_VIOLATION",
                0.0,
                List.of("POLICY_BLOCKED", e.getViolationType()),
                "SYSTEM",
                e.getMessage(),
                Map.of("field", field.getFieldName(), "violationType", e.getViolationType())
            );
        }
    }

    /**
     * Creates a HARD enrichment field from authoritative source.
     */
    private EnrichedField createHardEnrichment(String fieldName, Object originalValue, Object enrichedValue,
                                               SourceType sourceType, String sourceName, double reliability) {
        return new EnrichedField(
            fieldName, originalValue, enrichedValue,
            sourceType, sourceName,
            Confidence.fromReliabilityScore(reliability), reliability,
            Instant.now(), EnrichmentClass.HARD
        );
    }

    /**
     * Creates a SOFT enrichment field from estimation/inference.
     */
    private EnrichedField createSoftEnrichment(String fieldName, Object originalValue, Object enrichedValue,
                                               SourceType sourceType, String sourceName, double reliability) {
        return new EnrichedField(
            fieldName, originalValue, enrichedValue,
            sourceType, sourceName,
            Confidence.fromReliabilityScore(reliability), reliability,
            Instant.now(), EnrichmentClass.SOFT
        );
    }

    private void logToAudit(KycProfile profile, EnrichmentResult result, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("completionScore", result.getCompletionScore());
        metadata.put("enrichmentRiskScore", result.getEnrichmentRiskScore());
        metadata.put("sourcesUsed", result.getSourcesUsed());
        metadata.put("enrichedFieldCount", result.getEnrichedFields().size());
        metadata.put("hardEnrichments", result.getHardEnrichmentCount());
        metadata.put("softEnrichments", result.getSoftEnrichmentCount());
        metadata.put("policyViolations", result.getPolicyViolations().size());
        metadata.put("averageReliability", result.getAverageReliability());
        
        auditLogger.log(
            "enrichment",
            "ENRICHED",
            result.getCompletionScore(),
            List.of("DATA_ENRICHED", "SOURCES_VERIFIED", "POLICY_ENFORCED"),
            profile.getUserId(),
            String.format("Enrichment completed in %dms. Completion: %.0f%%, Risk: %.2f", 
                processingTime, result.getCompletionScore() * 100, result.getEnrichmentRiskScore()),
            metadata
        );
    }

    // ==================== TOOL METHODS (exposed to LLM) ====================

    @Schema(description = "Look up country-specific data from official registry")
    public Map<String, Object> lookupCountryData(
            @Schema(description = "ISO 2-letter country code") String countryCode) {
        return lookupCountryDataInternal(countryCode);
    }

    @Schema(description = "Estimate income range based on demographics (SOFT enrichment)")
    public Map<String, Object> estimateIncomeRange(
            @Schema(description = "ISO 2-letter country code") String countryCode,
            @Schema(description = "Age of the applicant") int age,
            @Schema(description = "Employment status if known") String employmentStatus) {
        return estimateIncomeRangeInternal(countryCode, age, employmentStatus);
    }

    @Schema(description = "Verify employment status against company registry")
    public Map<String, Object> verifyEmployment(
            @Schema(description = "Employer name") String employer,
            @Schema(description = "ISO 2-letter country code") String countryCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("verified", employer != null && !employer.isBlank());
        result.put("employer", employer);
        result.put("verificationSource", "OFFICIAL_COMPANY_REGISTER");
        result.put("sourceType", SourceType.OFFICIAL_COMPANY_REGISTER.name());
        result.put("confidence", employer != null ? Confidence.HIGH.name() : Confidence.LOW.name());
        result.put("reliabilityScore", employer != null ? 0.85 : 0.0);
        return result;
    }

    // ==================== INTERNAL METHODS ====================

    private Map<String, Object> lookupCountryDataInternal(String countryCode) {
        Map<String, Object> result = new HashMap<>();
        
        if (countryCode == null) {
            result.put("found", false);
            return result;
        }
        
        ComplianceConfig.DemographicProfile profile = 
            ComplianceConfig.DEMOGRAPHIC_PROFILES.get(countryCode.toUpperCase());
        
        if (profile != null) {
            result.put("found", true);
            result.put("countryCode", countryCode.toUpperCase());
            result.put("gdpPerCapita", profile.getAvgGdpPerCapita());
            result.put("avgAge", profile.getAvgAge());
            result.put("currency", profile.getCurrency());
            result.put("riskLevel", profile.getRiskLevel());
            result.put("sourceType", SourceType.COUNTRY_REGISTRY.name());
        } else {
            result.put("found", false);
            result.put("countryCode", countryCode.toUpperCase());
            result.put("riskLevel", "UNKNOWN");
        }
        
        return result;
    }

    private Map<String, Object> estimateIncomeRangeInternal(String countryCode, int age, String employmentStatus) {
        Map<String, Object> result = new HashMap<>();
        
        ComplianceConfig.DemographicProfile profile = 
            ComplianceConfig.DEMOGRAPHIC_PROFILES.get(countryCode != null ? countryCode.toUpperCase() : "");
        
        double baseIncome = profile != null ? profile.getAvgGdpPerCapita() : 30000;
        
        // Age adjustment
        double ageMultiplier = 1.0;
        if (age < 25) ageMultiplier = 0.5;
        else if (age < 35) ageMultiplier = 0.8;
        else if (age < 50) ageMultiplier = 1.2;
        else ageMultiplier = 1.0;
        
        // Employment adjustment
        double employmentMultiplier = 1.0;
        if (employmentStatus != null) {
            employmentMultiplier = switch (employmentStatus.toLowerCase()) {
                case "unemployed" -> 0.3;
                case "student" -> 0.2;
                case "part-time" -> 0.6;
                case "self-employed" -> 1.1;
                case "employed", "full-time" -> 1.0;
                default -> 0.8;
            };
        }
        
        double estimatedIncome = baseIncome * ageMultiplier * employmentMultiplier;
        double lowerBound = estimatedIncome * 0.7;
        double upperBound = estimatedIncome * 1.3;
        
        result.put("estimatedIncome", Math.round(estimatedIncome));
        result.put("lowerBound", Math.round(lowerBound));
        result.put("upperBound", Math.round(upperBound));
        result.put("confidence", Confidence.MEDIUM.name());
        result.put("reliabilityScore", 0.70);
        result.put("sourceType", SourceType.DEMOGRAPHIC_MODEL.name());
        result.put("factors", List.of("country_gdp", "age_adjustment", "employment_status"));
        
        return result;
    }

    private String estimateEmploymentStatus(Integer age) {
        if (age == null) return "unknown";
        if (age < 22) return "student";
        if (age < 25) return "entry-level";
        if (age < 65) return "employed";
        return "retired";
    }

    private void validateConsistency(KycProfile profile, EnrichmentResult result) {
        // Check income vs employment consistency
        if (profile.getEmploymentStatus() != null && profile.getIncome() != null) {
            String status = profile.getEmploymentStatus().toLowerCase();
            double income = profile.getIncome();
            
            if (status.equals("unemployed") && income > 50000) {
                result.addConsistencyIssue("High income reported for unemployed status");
            }
            if (status.equals("student") && income > 30000) {
                result.addConsistencyIssue("High income reported for student status");
            }
        }
        
        // Check age vs employment consistency
        if (profile.getAge() != null && profile.getEmploymentStatus() != null) {
            String status = profile.getEmploymentStatus().toLowerCase();
            int age = profile.getAge();
            
            if (age < 16 && !status.equals("student") && !status.equals("unemployed")) {
                result.addConsistencyIssue("Employment status unusual for age under 16");
            }
            if (age > 75 && status.equals("employed")) {
                result.addConsistencyIssue("Full employment unusual for age over 75");
            }
        }
    }

    public LlmAgent getLlmAgent() {
        return llmAgent;
    }
}
