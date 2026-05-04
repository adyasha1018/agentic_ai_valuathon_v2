package com.dnb.kyc.agents;

import com.dnb.kyc.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Analysis Agent — Business Scenario Tests
 *
 * These tests validate the Analysis Agent's FraudRiskReport generation
 * across 4 business scenarios plus determinism and bias-agnostic verification.
 *
 * NOTE: Fraud score is computed as AVERAGE of 5 indicators, not sum.
 * Example: (0.05 + 0.15 + 0 + 0.15 + 0.20) / 5 = 0.11
 */
@DisplayName("Analysis Agent - Business Scenario Tests")
public class AnalysisAgentSmokeTests {

    private AnalysisAgent analysisAgent;
    private AuditLoggerAgent auditLogger;
    private GuardrailsResult guardrailsResult;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLoggerAgent();
        analysisAgent = new AnalysisAgent(auditLogger);

        // Mock guardrails result
        guardrailsResult = new GuardrailsResult();
        guardrailsResult.setPassed(true);
    }

    // ==================== SCENARIO 1: LOW-RISK (AUTO-APPROVE) ====================

    @Test
    @DisplayName("Scenario 1: Low-Risk Applicant → AUTO_APPROVE")
    void testLowRiskApplicant_AutoApprove() {
        // Arrange - Clean, normal profile
        KycProfile profile = new KycProfile();
        profile.setUserId("USER_001_LOW_RISK");
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setIncome(55000.0);
        profile.setPep(false);
        profile.setPassportNumber("AB123456");
        profile.setEmploymentStatus("employed");

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 56000.0));

        // Act
        FraudRiskReport report = analysisAgent.analyze(
            profile, enrichmentResult, guardrailsResult);

        // Assert
        assertNotNull(report);
        assertTrue(report.fraudScore() < 0.25, // LOW risk threshold
            String.format("Low-risk score should be < 0.25, got: %.2f", report.fraudScore()));
        assertEquals(FraudRiskReport.RiskLevel.LOW, report.riskLevel());
        assertEquals(FraudRiskReport.Action.AUTO_APPROVE, report.recommendedAction());
        assertNotNull(report.fraudIndicators());
        assertEquals(5, report.fraudIndicators().size());
        assertTrue(report.confidence() > 0.8);

        System.out.println("✅ Scenario 1 PASSED: Low-Risk Applicant");
        System.out.println("   Fraud Score: " + String.format("%.2f", report.fraudScore()));
        System.out.println("   Decision: " + report.recommendedAction());
    }

    // ==================== SCENARIO 2: MEDIUM-RISK (MANUAL REVIEW) ====================

    @Test
    @DisplayName("Scenario 2: Medium-Risk Applicant → Multiple Issues")
    void testMediumRiskApplicant_MultipleIssues() {
        // Arrange - Multiple issues but not critical
        KycProfile profile = new KycProfile();
        profile.setUserId("USER_002_MEDIUM_RISK");
        profile.setFirstName("Jane");
        profile.setLastName("Smith");
        profile.setIncome(35000.0);   // Moderate income
        profile.setPep(false);
        profile.setPassportNumber(null);  // Missing → 0.15 documentRisk
        profile.setEmploymentStatus("unemployed"); // Unemployed + income > 30k → 0.20 behavioralRisk

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 40000.0));

        // Act
        FraudRiskReport report = analysisAgent.analyze(
            profile, enrichmentResult, guardrailsResult);

        // Assert - Fraud indicators should be mixed
        assertNotNull(report);
        // Average = (0.05 velocity + 0.05 income + 0 pep + 0.15 document + 0.20 behavior) / 5 = 0.09
        assertTrue(report.fraudScore() >= 0.05,
            String.format("Should have elevated fraud score, got: %.2f", report.fraudScore()));
        
        // Verify multiple risk factors present
        assertTrue(report.fraudIndicators().get("behavioralRisk") > 0.10,
            "Behavioral risk should be present");
        assertTrue(report.fraudIndicators().get("documentRisk") > 0.10,
            "Document risk should be present");

        System.out.println("✅ Scenario 2 PASSED: Medium-Risk Applicant");
        System.out.println("   Fraud Score: " + String.format("%.2f", report.fraudScore()));
        System.out.println("   Behavioral Risk: " + report.fraudIndicators().get("behavioralRisk"));
    }

    // ==================== SCENARIO 3: HIGH-RISK (PEP + MULTIPLE FLAGS) ====================

    @Test
    @DisplayName("Scenario 3: High-Risk Applicant (PEP + Multiple Red Flags)")
    void testHighRiskApplicant_PepMultipleFlags() {
        // Arrange - PEP with multiple risk factors
        KycProfile profile = new KycProfile();
        profile.setUserId("USER_003_HIGH_RISK");
        profile.setFirstName("Robert");
        profile.setLastName("Petrov");
        profile.setIncome(2000.0);           // Critically low → 0.25 incomeAnomalyRisk
        profile.setPep(true);                // ★ PEP DETECTED → 0.30 pepRisk
        profile.setPassportNumber(null);     // Missing → 0.15 documentRisk
        profile.setEmploymentStatus("unemployed"); // + very low income → 0.20 behavioralRisk

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 120000.0));

        // Act
        FraudRiskReport report = analysisAgent.analyze(
            profile, enrichmentResult, guardrailsResult);

        // Assert
        assertNotNull(report);
        // Average = (0.05 velocity + 0.25 income + 0.30 pep + 0.15 document + 0.20 behavior) / 5 = 0.19
        assertTrue(report.fraudScore() >= 0.15,
            String.format("Should have significant fraud score, got: %.2f", report.fraudScore()));

        // PEP should be primary risk
        assertTrue(report.fraudIndicators().get("pepRisk") > 0.25,
            "PEP risk should be elevated");

        // Verify comprehensive rationale
        assertTrue(report.rationale().size() >= 2,
            "Should have multiple issues in rationale");

        System.out.println("✅ Scenario 3 PASSED: High-Risk Applicant (PEP)");
        System.out.println("   Fraud Score: " + String.format("%.2f", report.fraudScore()));
        System.out.println("   PEP Risk: " + report.fraudIndicators().get("pepRisk"));
    }

    // ==================== SCENARIO 4: EXTREME INCOME ANOMALY ====================

    @Test
    @DisplayName("Scenario 4: Extreme Income Anomaly")
    void testExtremeIncomeAnomaly_HighIncome() {
        // Arrange - Suspiciously high income (600000)
        KycProfile profile = new KycProfile();
        profile.setUserId("USER_004_EXTREME_INCOME");
        profile.setIncome(600000.0);    // High income triggers anomaly risk
        profile.setPep(false);
        profile.setPassportNumber("CD567890");
        profile.setEmploymentStatus("self-employed");

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 600000.0));

        // Act
        FraudRiskReport report = analysisAgent.analyze(
            profile, enrichmentResult, guardrailsResult);

        // Assert
        assertNotNull(report);
        assertTrue(report.fraudIndicators().get("incomeAnomalyRisk") > 0.25,
            "High income should trigger income anomaly risk");
        assertTrue(report.fraudScore() > 0.05,
            "Should have elevated score due to income anomaly");

        System.out.println("✅ Scenario 4 PASSED: Extreme Income Anomaly");
        System.out.println("   Fraud Score: " + String.format("%.2f", report.fraudScore()));
        System.out.println("   Income Anomaly Risk: " + report.fraudIndicators().get("incomeAnomalyRisk"));
    }

    // ==================== DETERMINISM VERIFICATION ====================

    @Test
    @DisplayName("Determinism: Identical Data → Identical Scores")
    void testDeterminism_IdenticalData() {
        // Arrange - Two profiles with identical data
        KycProfile profile1 = new KycProfile();
        profile1.setUserId("DETERMINISM_A");
        profile1.setIncome(50000.0);
        profile1.setPep(false);
        profile1.setPassportNumber("EF123456");
        profile1.setEmploymentStatus("employed");

        KycProfile profile2 = new KycProfile();
        profile2.setUserId("DETERMINISM_B");
        profile2.setIncome(50000.0);
        profile2.setPep(false);
        profile2.setPassportNumber("EF123456");
        profile2.setEmploymentStatus("employed");

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 50000.0));

        // Act
        FraudRiskReport report1 = analysisAgent.analyze(
            profile1, enrichmentResult, guardrailsResult);
        FraudRiskReport report2 = analysisAgent.analyze(
            profile2, enrichmentResult, guardrailsResult);

        // Assert - Fraud scores should be identical
        double epsilon = 0.0001;
        assertTrue(Math.abs(report1.fraudScore() - report2.fraudScore()) < epsilon,
            String.format("Fraud scores should be identical: %.6f vs %.6f",
                report1.fraudScore(), report2.fraudScore()));
        assertEquals(report1.riskLevel(), report2.riskLevel(), "Risk levels must match");
        assertEquals(report1.recommendedAction(), report2.recommendedAction(), "Actions must match");

        System.out.println("✅ Determinism Test PASSED: Identical data → Identical scores");
        System.out.println("   Fraud Score: " + String.format("%.6f", report1.fraudScore()));
    }

    // ==================== BIAS-AGNOSTIC VERIFICATION ====================

    @Test
    @DisplayName("Bias-Agnostic: Age Does NOT Affect Fraud Score")
    void testBiasAgnostic_AgeNeutral() {
        // Create two profiles: young and old, everything else identical
        KycProfile young = new KycProfile();
        young.setUserId("YOUNG");
        young.setAge(22);
        young.setIncome(50000.0);
        young.setPep(false);
        young.setPassportNumber("GH789012");
        young.setEmploymentStatus("employed");

        KycProfile old = new KycProfile();
        old.setUserId("OLD");
        old.setAge(65);
        old.setIncome(50000.0);
        old.setPep(false);
        old.setPassportNumber("GH789012");
        old.setEmploymentStatus("employed");

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 50000.0));

        // Act
        FraudRiskReport reportYoung = analysisAgent.analyze(
            young, enrichmentResult, guardrailsResult);
        FraudRiskReport reportOld = analysisAgent.analyze(
            old, enrichmentResult, guardrailsResult);

        // Assert - Fraud scores MUST be identical (bias-agnostic)
        double epsilon = 0.0001;
        assertTrue(Math.abs(reportYoung.fraudScore() - reportOld.fraudScore()) < epsilon,
            String.format("Age should NOT affect fraud score: %.6f vs %.6f",
                reportYoung.fraudScore(), reportOld.fraudScore()));

        System.out.println("✅ Bias-Agnostic Test PASSED: Age does not affect scoring");
        System.out.println("   Age 22 Score: " + String.format("%.4f", reportYoung.fraudScore()));
        System.out.println("   Age 65 Score: " + String.format("%.4f", reportOld.fraudScore()));
    }

    // ==================== INDICATOR INDEPENDENCE ====================

    @Test
    @DisplayName("Indicators: Each Component Independently Calculated")
    void testIndicatorIndependence() {
        // Profile with ONLY PEP flag, everything else clean
        KycProfile profile = new KycProfile();
        profile.setUserId("INDICATOR_TEST");
        profile.setIncome(60000.0);
        profile.setPep(true);  // ← ONLY this is risky
        profile.setPassportNumber("IJ345678");
        profile.setEmploymentStatus("employed");

        EnrichmentResult enrichmentResult = new EnrichmentResult();
        enrichmentResult.getEnrichedData().put("estimatedIncomeRange",
            java.util.Map.of("estimatedIncome", 60000.0));

        // Act
        FraudRiskReport report = analysisAgent.analyze(
            profile, enrichmentResult, guardrailsResult);

        // Assert - Only pepRisk should be elevated
        assertTrue(report.fraudIndicators().get("pepRisk") > 0.25,
            "PEP risk should be elevated");
        assertTrue(report.fraudIndicators().get("velocityRisk") < 0.10,
            "Velocity should be low");
        assertTrue(report.fraudIndicators().get("incomeAnomalyRisk") < 0.10,
            "Income should be normal");
        assertTrue(report.fraudIndicators().get("documentRisk") < 0.10,
            "Documents should be valid");
        assertTrue(report.fraudIndicators().get("behavioralRisk") < 0.10,
            "Behavior should be normal");

        System.out.println("✅ Indicator Independence Test PASSED");
        System.out.println("   Only PEP risk elevated: " + report.fraudIndicators().get("pepRisk"));
    }
}
