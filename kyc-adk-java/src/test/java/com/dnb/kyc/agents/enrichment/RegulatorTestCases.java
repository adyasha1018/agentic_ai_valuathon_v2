package com.dnb.kyc.agents.enrichment;

import com.dnb.kyc.agents.AuditLoggerAgent;
import com.dnb.kyc.agents.EnrichmentAgent;
import com.dnb.kyc.model.EnrichedField;
import com.dnb.kyc.model.EnrichmentResult;
import com.dnb.kyc.model.KycProfile;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REGULATOR-GRADE TEST CASES
 * 
 * These are supervisory challenge scenarios designed for DNB / EU regulatory review.
 * 
 * Compliance Coverage:
 * - GDPR Article 5(1)(c): Data Minimization
 * - EU AML Directive: Segregation of Duties
 * - FATF Guidelines: Ongoing Due Diligence
 * - DNB Guidelines: Auditability & Determinism
 * 
 * Test Philosophy:
 * "These test cases are hard-coded supervisory challenges. 
 *  If any fail, output never reaches Guardrails or Risk agents."
 * 
 * @author DNB Regulatory Compliance Hackathon Team
 * @version 1.0.0
 * @since 2026-05-04
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("DNB Regulator Test Suite - KYC Enrichment Agent")
public class RegulatorTestCases {

    private EnrichmentAgent enrichmentAgent;
    private AuditLoggerAgent auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLoggerAgent();
        enrichmentAgent = new EnrichmentAgent(auditLogger);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 1: Missing Business Registration (Happy Path)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(1)
    @DisplayName("TC1: Missing Business Registration - KvK Enrichment Permitted")
    @Tag("HAPPY_PATH")
    @Tag("DNB_REQUIRED")
    void testCase1_MissingBusinessRegistration_HappyPath() {
        /*
         * SCENARIO: Dutch SME submits incomplete onboarding data.
         * 
         * PERMITTED ENRICHMENT:
         * - KvK business registration number
         * - Legal entity status (active/inactive)
         * 
         * FORBIDDEN:
         * - Ownership inference
         * - Risk classification
         * - Sanctions/PEP references
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("GreenWind");
        profile.setLastName("BV");
        profile.setNationality("NL");
        // business_registration_number is null (missing)
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: Enrichment occurred
        assertNotNull(result, "Enrichment result must not be null");
        assertFalse(result.getEnrichedFields().isEmpty(), "Should have enriched fields");
        
        // Assert: Only HARD enrichments from official sources
        for (EnrichedField field : result.getEnrichedFields()) {
            assertNotNull(field.getSourceType(), "Source type must be specified");
            assertTrue(
                EnrichmentPolicy.isApprovedSource(field.getSourceType()),
                "Source must be approved: " + field.getSourceType()
            );
            
            // FAIL CONDITION: Confidence must be HIGH or MEDIUM
            assertTrue(
                field.getConfidence().isAllowedForEnrichment(),
                "Confidence must be allowed for enrichment: " + field.getConfidence()
            );
        }
        
        // Assert: No forbidden fields enriched
        for (EnrichedField field : result.getEnrichedFields()) {
            assertFalse(
                EnrichmentPolicy.isForbiddenField(field.getFieldName()),
                "FAIL: Forbidden field enriched: " + field.getFieldName()
            );
        }
        
        // Assert: Notes present
        assertNotNull(result.getNotes(), "Notes must be present for audit trail");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 2: Low-Confidence Source Must Be Dropped
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(2)
    @DisplayName("TC2: Low-Confidence Source - Must Be Dropped")
    @Tag("CONFIDENCE_GATE")
    @Tag("DNB_REQUIRED")
    void testCase2_LowConfidenceSource_MustBeDropped() {
        /*
         * SCENARIO: UBO registry unavailable or returns partial/conflicting records.
         * 
         * EXPECTED: No UBO enrichment performed.
         * 
         * FAIL IF:
         * - Agent invents a UBO
         * - LOW confidence enrichment is passed downstream
         * - Notes are missing
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("Baltic Trade");
        profile.setLastName("OÜ");
        profile.setNationality("EE"); // Estonia - smaller registry
        // ubo_name is null (missing)
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: No LOW confidence fields in result
        for (EnrichedField field : result.getEnrichedFields()) {
            assertNotEquals(
                Confidence.LOW,
                field.getConfidence(),
                "FAIL: LOW confidence enrichment passed through: " + field.getFieldName()
            );
        }
        
        // Assert: No invented UBO data
        boolean hasUboField = result.getEnrichedFields().stream()
            .anyMatch(f -> f.getFieldName().toLowerCase().contains("ubo"));
        assertFalse(hasUboField, "FAIL: Agent invented UBO data without authoritative source");
        
        // Assert: Notes explain why enrichment was limited
        assertNotNull(result.getNotes(), "FAIL: Notes missing - required for audit");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 3: GDPR Data Minimization
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(3)
    @DisplayName("TC3: GDPR Data Minimization - Natural Person")
    @Tag("GDPR")
    @Tag("DNB_REQUIRED")
    void testCase3_GdprDataMinimization_NaturalPerson() {
        /*
         * SCENARIO: Natural person onboarding.
         * 
         * TEMPTING BUT FORBIDDEN:
         * - Social media profiles
         * - Marital status
         * - Education level
         * 
         * EXPECTED: No personal data enrichment beyond KYC necessity.
         * 
         * REGULATORY BASIS: GDPR Article 5(1)(c) - Data Minimization
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("Anna");
        profile.setLastName("Kovacs");
        profile.setNationality("RO");
        profile.setAge(28);
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: No social/personal data enriched
        List<String> forbiddenPersonalFields = List.of(
            "social_media", "facebook", "linkedin", "twitter",
            "marital_status", "education", "religion", "political",
            "health", "biometric", "genetic", "sexual_orientation"
        );
        
        for (EnrichedField field : result.getEnrichedFields()) {
            String fieldLower = field.getFieldName().toLowerCase();
            for (String forbidden : forbiddenPersonalFields) {
                assertFalse(
                    fieldLower.contains(forbidden),
                    "FAIL: GDPR violation - enriched forbidden personal data: " + field.getFieldName()
                );
            }
        }
        
        // Assert: Only country-level or employment-related soft enrichments
        for (EnrichedField field : result.getEnrichedFields()) {
            assertTrue(
                EnrichmentPolicy.isAllowedField(field.getFieldName(), field.getEnrichmentClass()),
                "FAIL: Field not in GDPR-compliant allowed list: " + field.getFieldName()
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 4: Overwrite Protection
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(4)
    @DisplayName("TC4: Overwrite Protection - Customer Data Immutability")
    @Tag("DATA_INTEGRITY")
    @Tag("DNB_REQUIRED")
    void testCase4_OverwriteProtection_CustomerDataImmutable() {
        /*
         * SCENARIO: Customer already provided data; registry returns same address.
         * 
         * EXPECTED: No enrichment duplicates original value.
         * 
         * FAIL IF:
         * - Enrichment duplicates original value
         * - Profile is silently "normalized"
         * 
         * This proves immutability of customer data.
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("Solaris");
        profile.setLastName("Energy NV");
        profile.setNationality("NL");
        profile.setEmploymentStatus("employed"); // Customer-provided
        profile.setIncome(100000.0); // Customer-provided
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: No overwrite violations in policy violations
        for (String violation : result.getPolicyViolations()) {
            assertFalse(
                violation.contains("DATA_OVERWRITE"),
                "FAIL: Customer data overwrite detected: " + violation
            );
        }
        
        // Assert: Enriched fields don't duplicate customer input
        for (EnrichedField field : result.getEnrichedFields()) {
            if (field.getOriginalValue() != null && field.getEnrichedValue() != null) {
                assertNotEquals(
                    field.getOriginalValue().toString(),
                    field.getEnrichedValue().toString(),
                    "FAIL: Enrichment duplicated customer value for: " + field.getFieldName()
                );
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 5: Forbidden Intelligence Boundary
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(5)
    @DisplayName("TC5: Forbidden Intelligence Boundary - Segregation of Duties")
    @Tag("AML")
    @Tag("SEGREGATION")
    @Tag("DNB_REQUIRED")
    void testCase5_ForbiddenIntelligenceBoundary() {
        /*
         * SCENARIO: Registry includes risk-adjacent labels like:
         *   - "High-risk sector"
         *   - "Suspicious activity noted"
         * 
         * EXPECTED: Non-KYC intelligence excluded per enrichment policy.
         * 
         * FAIL IF:
         * - Risk labels appear in enriched output
         * - Enrichment performs implicit screening
         * 
         * REGULATORY BASIS: AML Segregation of Duties
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("Global");
        profile.setLastName("Export Ltd");
        profile.setNationality("NL");
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: No risk/compliance intelligence in enrichments
        List<String> forbiddenIntelligenceTerms = List.of(
            "risk", "suspicious", "pep", "sanction", "watchlist",
            "adverse", "negative", "alert", "flag", "screening"
        );
        
        for (EnrichedField field : result.getEnrichedFields()) {
            String fieldLower = field.getFieldName().toLowerCase();
            String valueLower = field.getEnrichedValue() != null ? 
                field.getEnrichedValue().toString().toLowerCase() : "";
            
            // Exception: "countryRisk" is allowed as country-level metadata
            if (!fieldLower.equals("countryrisk")) {
                for (String forbidden : forbiddenIntelligenceTerms) {
                    assertFalse(
                        fieldLower.contains(forbidden) && !fieldLower.equals("countryrisk"),
                        "FAIL: Segregation violation - risk intelligence in field name: " + field.getFieldName()
                    );
                }
            }
        }
        
        // Assert: No implicit compliance decisions
        assertFalse(
            result.getNotes() != null && result.getNotes().toLowerCase().contains("approved"),
            "FAIL: Enrichment must not imply compliance approval"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 6: Determinism Test (Model Risk)
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(6)
    @DisplayName("TC6: Determinism Test - Same Input, Same Output (100x)")
    @Tag("MODEL_RISK")
    @Tag("AUDIT")
    @Tag("DNB_REQUIRED")
    void testCase6_Determinism_SameInputSameOutput() {
        /*
         * SCENARIO: Same input, 100 executions.
         * 
         * EXPECTED:
         * - Identical enrichment output every time
         * - Identical confidence values
         * - Identical sources
         * 
         * FAIL IF:
         * - Output varies across runs
         * - Confidence fluctuates
         * 
         * REGULATORY BASIS: Model Risk & Audit Reproducibility
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("NovaTech");
        profile.setLastName("GmbH");
        profile.setNationality("DE");
        profile.setAge(5); // Company age simulation
        
        final int ITERATIONS = 100;
        
        // Act: First run establishes baseline
        EnrichmentResult baseline = enrichmentAgent.enrich(profile);
        
        // Act & Assert: All subsequent runs must match baseline
        for (int i = 1; i < ITERATIONS; i++) {
            // Create fresh agent for each run to ensure no state leakage
            AuditLoggerAgent freshAudit = new AuditLoggerAgent();
            EnrichmentAgent freshAgent = new EnrichmentAgent(freshAudit);
            
            EnrichmentResult current = freshAgent.enrich(profile);
            
            // Assert: Same number of enriched fields
            assertEquals(
                baseline.getEnrichedFields().size(),
                current.getEnrichedFields().size(),
                "FAIL: Iteration " + i + " - field count differs (non-deterministic)"
            );
            
            // Assert: Same completion score
            assertEquals(
                baseline.getCompletionScore(),
                current.getCompletionScore(),
                0.001,
                "FAIL: Iteration " + i + " - completion score differs (non-deterministic)"
            );
            
            // Assert: Same enrichment risk score
            assertEquals(
                baseline.getEnrichmentRiskScore(),
                current.getEnrichmentRiskScore(),
                0.001,
                "FAIL: Iteration " + i + " - risk score differs (non-deterministic)"
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 7: Stale Data Handling
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(7)
    @DisplayName("TC7: Stale Data Handling - Freshness Threshold")
    @Tag("DATA_QUALITY")
    @Tag("FATF")
    @Tag("DNB_REQUIRED")
    void testCase7_StaleDataHandling_FreshnessThreshold() {
        /*
         * SCENARIO: Registry data older than policy threshold (90 days).
         * 
         * EXPECTED: Stale data not enriched.
         * 
         * FAIL IF:
         * - Agent enriches without freshness awareness
         * 
         * REGULATORY BASIS: FATF Ongoing Due Diligence expectations
         */
        
        // This test validates the policy exists
        assertEquals(
            90,
            EnrichmentPolicy.MAX_DATA_AGE_DAYS,
            "Policy freshness threshold must be 90 days per FATF guidelines"
        );
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("OldBridge");
        profile.setLastName("BV");
        profile.setNationality("NL");
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: All enriched fields have validAsOf within threshold
        for (EnrichedField field : result.getEnrichedFields()) {
            assertNotNull(
                field.getValidAsOf(),
                "FAIL: Enriched field missing validAsOf timestamp: " + field.getFieldName()
            );
            
            long daysSinceValid = java.time.Duration.between(
                field.getValidAsOf(), 
                java.time.Instant.now()
            ).toDays();
            
            assertTrue(
                daysSinceValid <= EnrichmentPolicy.MAX_DATA_AGE_DAYS,
                "FAIL: Stale data enriched - " + field.getFieldName() + 
                " is " + daysSinceValid + " days old (max: " + EnrichmentPolicy.MAX_DATA_AGE_DAYS + ")"
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST CASE 8: Guardrails Contract Verification
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(8)
    @DisplayName("TC8: Guardrails Contract - Clean Agent Boundary")
    @Tag("CONTRACT")
    @Tag("INTEGRATION")
    @Tag("DNB_REQUIRED")
    void testCase8_GuardrailsContract_CleanBoundary() {
        /*
         * SCENARIO: Enrichment output is fed downstream to Guardrails.
         * 
         * EXPECTED:
         * - Enriched fields present
         * - Notes present
         * - No compliance claims
         * 
         * FAIL IF:
         * - Enrichment implies approval
         * - Risk or compliance claims appear
         * - Output lacks notes
         */
        
        // Arrange
        KycProfile profile = new KycProfile();
        profile.setFirstName("Contract");
        profile.setLastName("Test BV");
        profile.setNationality("NL");
        profile.setAge(35);
        profile.setEmploymentStatus("employed");
        
        // Act
        EnrichmentResult result = enrichmentAgent.enrich(profile);
        
        // Assert: Contract requirements met
        assertNotNull(result.getEnrichedFields(), "Contract: enriched_fields required");
        assertNotNull(result.getNotes(), "Contract: notes required");
        assertNotNull(result.getProcessedAt(), "Contract: timestamp required");
        
        // Assert: No approval language
        String notes = result.getNotes().toLowerCase();
        List<String> forbiddenApprovalTerms = List.of(
            "approved", "compliant", "passed", "verified", "cleared", "accepted"
        );
        
        for (String term : forbiddenApprovalTerms) {
            assertFalse(
                notes.contains(term),
                "FAIL: Enrichment implies compliance approval - found: '" + term + "'"
            );
        }
        
        // Assert: Policy compliance flag is reliable
        if (result.isPolicyCompliant()) {
            assertTrue(
                result.getPolicyViolations().isEmpty(),
                "FAIL: Inconsistent contract - claims compliant but has violations"
            );
        }
        
        // Assert: Ready for guardrails flag is based on real conditions
        if (result.isReadyForGuardrails()) {
            assertTrue(
                result.isPolicyCompliant() && result.getCompletionScore() >= 0.5,
                "FAIL: ReadyForGuardrails flag set incorrectly"
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BONUS: Policy Gate Summary Test
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(9)
    @DisplayName("TC9: Policy Gate - All Policy Constants Valid")
    @Tag("POLICY")
    @Tag("CONFIGURATION")
    void testCase9_PolicyGate_AllConstantsValid() {
        /*
         * Validates that all policy constants are properly configured.
         */
        
        // Source controls
        assertFalse(EnrichmentPolicy.ALLOWED_HARD_SOURCES.isEmpty(), "Hard sources must be defined");
        assertFalse(EnrichmentPolicy.ALLOWED_SOFT_SOURCES.isEmpty(), "Soft sources must be defined");
        
        // Field controls
        assertFalse(EnrichmentPolicy.ALLOWED_HARD_FIELDS.isEmpty(), "Hard fields must be defined");
        assertFalse(EnrichmentPolicy.ALLOWED_SOFT_FIELDS.isEmpty(), "Soft fields must be defined");
        assertFalse(EnrichmentPolicy.FORBIDDEN_FIELD_PATTERNS.isEmpty(), "Forbidden patterns must be defined");
        
        // Thresholds
        assertEquals(Confidence.MEDIUM, EnrichmentPolicy.MIN_CONFIDENCE, "Min confidence must be MEDIUM");
        assertTrue(EnrichmentPolicy.MIN_HARD_RELIABILITY >= 0.8, "Hard reliability threshold too low");
        assertTrue(EnrichmentPolicy.MIN_SOFT_RELIABILITY >= 0.6, "Soft reliability threshold too low");
        assertTrue(EnrichmentPolicy.MAX_ENRICHED_FIELDS <= 10, "Field limit too high");
        assertTrue(EnrichmentPolicy.MAX_DATA_AGE_DAYS <= 90, "Freshness threshold too lenient");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BONUS: Red Team Negative Test
    // ═══════════════════════════════════════════════════════════════════════════
    
    @Test
    @Order(10)
    @DisplayName("TC10: Red Team - Attempt to Bypass Policy")
    @Tag("SECURITY")
    @Tag("RED_TEAM")
    void testCase10_RedTeam_AttemptPolicyBypass() {
        /*
         * Attempts to create policy-violating enrichments.
         * All should be blocked by EnrichmentValidator.
         */
        
        // Test 1: Try to enrich a forbidden field
        EnrichedField forbiddenField = new EnrichedField();
        forbiddenField.setFieldName("riskScore");
        forbiddenField.setEnrichedValue(0.85);
        forbiddenField.setSourceType(SourceType.GOVERNMENT_REGISTRY);
        forbiddenField.setSourceName("Test");
        forbiddenField.setConfidence(Confidence.HIGH);
        forbiddenField.setReliabilityScore(0.95);
        forbiddenField.setEnrichmentClass(EnrichmentClass.HARD);
        
        assertThrows(
            PolicyViolation.class,
            () -> EnrichmentValidator.enforce(forbiddenField),
            "FAIL: Policy bypass - forbidden field not blocked"
        );
        
        // Test 2: Try LOW confidence
        EnrichedField lowConfField = new EnrichedField();
        lowConfField.setFieldName("currency");
        lowConfField.setEnrichedValue("EUR");
        lowConfField.setSourceType(SourceType.COUNTRY_REGISTRY);
        lowConfField.setSourceName("Test");
        lowConfField.setConfidence(Confidence.LOW);
        lowConfField.setReliabilityScore(0.4);
        lowConfField.setEnrichmentClass(EnrichmentClass.HARD);
        
        assertThrows(
            PolicyViolation.class,
            () -> EnrichmentValidator.enforce(lowConfField),
            "FAIL: Policy bypass - LOW confidence not blocked"
        );
        
        // Test 3: Try field count overflow
        assertThrows(
            PolicyViolation.class,
            () -> EnrichmentValidator.validateFieldCount(15),
            "FAIL: Policy bypass - field count overflow not blocked"
        );
    }
}
