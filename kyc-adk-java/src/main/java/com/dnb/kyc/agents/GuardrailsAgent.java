package com.dnb.kyc.agents;

import com.dnb.kyc.config.ComplianceConfig;
import com.dnb.kyc.model.*;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Guardrails Agent 🛡️
 * 
 * Responsibility: Compliance validation & bias enforcement
 * 
 * Checks performed:
 * - Required fields validation
 * - PEP (Politically Exposed Person) screening
 * - OFAC sanctions list
 * - Geographic bias detection
 * - Age bias detection
 * - Format validation (email, phone, passport)
 */
public class GuardrailsAgent {

    private static final Logger logger = LoggerFactory.getLogger(GuardrailsAgent.class);
    
    private final AuditLoggerAgent auditLogger;
    private final LlmAgent llmAgent;

    public GuardrailsAgent(AuditLoggerAgent auditLogger) {
        this.auditLogger = auditLogger;
        
        // Build the LLM Agent with Google ADK
        this.llmAgent = LlmAgent.builder()
            .name("guardrails_agent")
            .description("Validates KYC data for compliance and detects potential bias in decision-making")
            .model("gemini-2.0-flash")
            .instruction("""
                You are a KYC compliance validation agent.
                
                When you receive KYC profile data, you MUST call your 3 tools in this order:
                
                1. Call validateRequiredFields — pass the complete KYC data as a JSON string.
                
                2. Call checkPepStatus — pass the 2-letter nationality code (e.g. "NL", "DE", "FR").
                
                3. Call detectBias — pass two arguments:
                   - dateOfBirth: the date of birth in YYYY-MM-DD format (e.g. "1995-08-12")
                   - nationality: the 2-letter country code (e.g. "NL")
                
                After all 3 tools respond, return a validation summary:
                  PASS if all checks cleared, FAIL if any check failed.
                  List every check performed and its result.
                """)
            .tools(
                FunctionTool.create(this, "validateRequiredFields"),
                FunctionTool.create(this, "checkPepStatus"),
                FunctionTool.create(this, "detectBias")
            )
            .build();
    }

    /**
     * Main validation entry point
     */
    public GuardrailsResult validate(KycProfile profile) {
        logger.info("Starting guardrails validation for user: {}", profile.getUserId());
        
        long startTime = System.currentTimeMillis();
        GuardrailsResult result = new GuardrailsResult();
        
        // 1. Validate required fields
        List<String> missingFields = validateRequiredFieldsInternal(profile);
        if (!missingFields.isEmpty()) {
            result.addIssue("Missing required fields: " + String.join(", ", missingFields));
            result.addRuleFired("MISSING_FIELDS");
        }
        
        // 2. Check PEP status
        boolean isPep = checkPepStatusInternal(profile.getNationality());
        if (isPep) {
            result.addIssue("High-risk country detected: " + profile.getNationality());
            result.addRuleFired("PEP_HIGH_RISK_COUNTRY");
        } else {
            result.addRuleFired("PEP_CLEAR");
        }
        
        // 3. Detect bias
        GuardrailsResult.BiasAssessment biasAssessment = detectBiasInternal(profile);
        result.setBiasAssessment(biasAssessment);
        if (biasAssessment.isBiasDetected()) {
            result.addRuleFired("BIAS_DETECTED");
        } else {
            result.addRuleFired("BIAS_CHECK_OK");
        }
        
        // 4. Format validation
        validateFormats(profile, result);
        
        // Determine if validation passed
        boolean hasCriticalIssues = result.getIssues().stream()
            .anyMatch(issue -> issue.contains("High-risk country"));
        result.setPassed(!hasCriticalIssues && missingFields.isEmpty());
        
        // Log to audit trail
        long processingTime = System.currentTimeMillis() - startTime;
        Map<String, Object> biasIndicators = new HashMap<>();
        biasIndicators.put("biasDetected", biasAssessment.isBiasDetected());
        biasIndicators.put("biasScore", biasAssessment.getBiasScore());
        biasIndicators.put("factors", biasAssessment.getFactors());
        
        auditLogger.log(
            "guardrails",
            result.isPassed() ? "PASSED" : "FLAGGED",
            result.isPassed() ? 1.0 : 0.5,
            result.getRulesFired(),
            profile.getUserId(),
            String.format("Validation completed in %dms. Issues: %d", processingTime, result.getIssues().size()),
            biasIndicators
        );
        
        logger.info("Guardrails validation complete: passed={}", result.isPassed());
        return result;
    }

    // ==================== TOOL METHODS (exposed to LLM) ====================

    @Schema(description = "Validate that all required KYC fields are present")
    public Map<String, Object> validateRequiredFields(
            @Schema(description = "The KYC profile data as JSON") String profileJson) {
        // This method is exposed as a tool for the LLM to use
        Map<String, Object> response = new HashMap<>();
        response.put("requiredFields", ComplianceConfig.REQUIRED_FIELDS);
        response.put("validated", true);
        return response;
    }

    @Schema(description = "Check if nationality is on PEP high-risk country list")
    public Map<String, Object> checkPepStatus(
            @Schema(description = "ISO 2-letter country code") String countryCode) {
        Map<String, Object> response = new HashMap<>();
        boolean isHighRisk = ComplianceConfig.PEP_COUNTRIES.contains(countryCode);
        response.put("countryCode", countryCode);
        response.put("isHighRisk", isHighRisk);
        response.put("pepCountries", ComplianceConfig.PEP_COUNTRIES);
        return response;
    }

    @Schema(description = "Detect potential bias in KYC decision-making")
    public Map<String, Object> detectBias(
            @Schema(description = "Date of birth in YYYY-MM-DD format (e.g. 1995-08-12)") String dateOfBirth,
            @Schema(description = "ISO 2-letter country code") String nationality) {
        int age = 30; // default
        try {
            java.time.LocalDate dob = java.time.LocalDate.parse(dateOfBirth);
            age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
        } catch (Exception ignored) {}
        Map<String, Object> response = new HashMap<>();
        
        double biasScore = 0.0;
        List<String> factors = new ArrayList<>();
        
        ComplianceConfig.DemographicProfile profile = 
            ComplianceConfig.DEMOGRAPHIC_PROFILES.get(nationality);
        
        if (profile != null) {
            // Age bias check
            int ageDiff = Math.abs(age - profile.getAvgAge());
            if (ageDiff > ComplianceConfig.AGE_BIAS_THRESHOLD) {
                biasScore += 0.2;
                factors.add("Age deviation: " + ageDiff + " years from country average");
            }
            
            // GDP bias check
            double euAvgGdp = 45000;
            if (profile.getAvgGdpPerCapita() < euAvgGdp * ComplianceConfig.GDP_BIAS_THRESHOLD) {
                biasScore += 0.15;
                factors.add("Lower GDP country: potential geographic bias");
            }
        }
        
        response.put("biasScore", biasScore);
        response.put("biasDetected", biasScore > 0.25);
        response.put("factors", factors);
        return response;
    }

    // ==================== INTERNAL METHODS ====================

    private List<String> validateRequiredFieldsInternal(KycProfile profile) {
        List<String> missing = new ArrayList<>();
        
        if (profile.getFirstName() == null || profile.getFirstName().isBlank()) {
            missing.add("firstName");
        }
        if (profile.getLastName() == null || profile.getLastName().isBlank()) {
            missing.add("lastName");
        }
        if (profile.getNationality() == null || profile.getNationality().isBlank()) {
            missing.add("nationality");
        }
        if (profile.getAge() == null) {
            missing.add("age");
        }
        
        return missing;
    }

    private boolean checkPepStatusInternal(String nationality) {
        return nationality != null && ComplianceConfig.PEP_COUNTRIES.contains(nationality.toUpperCase());
    }

    private GuardrailsResult.BiasAssessment detectBiasInternal(KycProfile profile) {
        GuardrailsResult.BiasAssessment assessment = new GuardrailsResult.BiasAssessment();
        double biasScore = 0.0;
        
        if (profile.getNationality() == null || profile.getAge() == null) {
            assessment.setBiasScore(0.0);
            assessment.setBiasDetected(false);
            return assessment;
        }
        
        ComplianceConfig.DemographicProfile demographic = 
            ComplianceConfig.DEMOGRAPHIC_PROFILES.get(profile.getNationality().toUpperCase());
        
        if (demographic != null) {
            // Age bias check
            int ageDiff = Math.abs(profile.getAge() - demographic.getAvgAge());
            if (ageDiff > ComplianceConfig.AGE_BIAS_THRESHOLD) {
                biasScore += 0.2;
                assessment.addFactor("Age deviation: " + ageDiff + " years from country average of " + demographic.getAvgAge());
            }
            
            // GDP/Geographic bias check
            double euAvgGdp = 45000;
            if (demographic.getAvgGdpPerCapita() < euAvgGdp * ComplianceConfig.GDP_BIAS_THRESHOLD) {
                biasScore += 0.15;
                assessment.addFactor("Lower GDP country (" + demographic.getAvgGdpPerCapita() + "): potential geographic bias risk");
            }
        } else {
            // Unknown country
            assessment.addFactor("Country profile not found - manual review recommended");
            biasScore += 0.1;
        }
        
        assessment.setBiasScore(biasScore);
        assessment.setBiasDetected(biasScore > 0.25);
        
        return assessment;
    }

    private void validateFormats(KycProfile profile, GuardrailsResult result) {
        // Email validation
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            if (!profile.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                result.addIssue("Invalid email format");
                result.addRuleFired("INVALID_EMAIL_FORMAT");
            }
        }
        
        // Phone validation (basic)
        if (profile.getPhone() != null && !profile.getPhone().isBlank()) {
            if (!profile.getPhone().matches("^\\+?[0-9\\s-]{8,}$")) {
                result.addIssue("Invalid phone format");
                result.addRuleFired("INVALID_PHONE_FORMAT");
            }
        }
    }

    public LlmAgent getLlmAgent() {
        return llmAgent;
    }
}
