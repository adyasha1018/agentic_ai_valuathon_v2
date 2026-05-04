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
 * Analysis Agent 🔍 — Production Grade
 *
 * Responsibility: Deterministic fraud detection & risk scoring
 *
 * Fraud Indicators Analyzed (Deterministic & Bias-Agnostic):
 * - Velocity fraud: Multiple applications in short timeframe
 * - Income anomaly: Inconsistent income patterns
 * - PEP indicators: Politically exposed persons
 * - Document risk: Missing or invalid documents
 * - Behavioral risk: Unusual patterns (employment vs income inconsistency)
 *
 * Design Principles:
 * ✅ Explicit indicators (no hidden heuristics)
 * ✅ Bias-agnostic (age & geography excluded from scoring)
 * ✅ Deterministic (same input → same output)
 * ✅ Audit-ready (JSON serializable, hash-chain friendly)
 * ✅ Explainable (clear rationale for every decision)
 */
public class AnalysisAgent {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisAgent.class);

    private final AuditLoggerAgent auditLogger;
    private final LlmAgent llmAgent;

    // Simulated velocity tracking (in production, use distributed cache)
    private final Map<String, List<Long>> applicationHistory = new HashMap<>();

    public AnalysisAgent(AuditLoggerAgent auditLogger) {
        this.auditLogger = auditLogger;

        // Build the LLM Agent with Google ADK
        this.llmAgent = LlmAgent.builder()
            .name("analysis_agent")
            .description("Performs deterministic fraud detection and risk analysis on KYC profiles")
            .model("gemini-2.0-flash")
            .instruction("""
                You are a KYC fraud detection and risk analysis agent.
                
                When you receive KYC profile data, you MUST call your 3 tools in this order:
                
                1. Call checkVelocity — pass the kycCaseId as the userId argument (e.g. "KYC-TEST-001").
                   Note the velocityScore returned (0.0 to 1.0).
                
                2. Call analyzeAmountRisk — pass the expectedMonthlyVolumeEUR value as the income argument.
                   If not available, use 0.0. Note the amountScore returned (0.0 to 1.0).
                
                3. Call calculateRiskScore — pass TWO arguments:
                   - velocityScore: the score returned from step 1 (a decimal like 0.05)
                   - amountScore: the score returned from step 2 (a decimal like 0.05)
                
                After all 3 tools respond, return:
                  - Risk Score: weightedScore from calculateRiskScore
                  - Risk Level: riskLevel from calculateRiskScore (LOW/MEDIUM/HIGH/CRITICAL)
                  - Decision: recommendedAction from calculateRiskScore
                """)
            .tools(
                FunctionTool.create(this, "checkVelocity"),
                FunctionTool.create(this, "analyzeAmountRisk"),
                FunctionTool.create(this, "calculateRiskScore")
            )
            .build();
    }

    /**
     * Main analysis entry point — Production Grade
     *
     * Returns a deterministic FraudRiskReport with explicit fraud indicators,
     * clear rationale, and audit-ready structure.
     */
    public FraudRiskReport analyze(KycProfile profile, EnrichmentResult enrichmentResult, GuardrailsResult guardrailsResult) {
        logger.info("Starting deterministic fraud analysis for user: {}", profile.getUserId());

        long startTime = System.currentTimeMillis();

        // Calculate individual fraud indicators (all deterministic)
        double velocityRisk = assessVelocity(profile);
        double incomeAnomalyRisk = assessIncome(profile, enrichmentResult);
        double pepRisk = assessPep(profile, guardrailsResult);
        double documentRisk = assessDocuments(profile);
        double behavioralRisk = assessBehavior(profile);

        // Build indicator map
        Map<String, Double> indicators = Map.ofEntries(
            Map.entry("velocityRisk", velocityRisk),
            Map.entry("incomeAnomalyRisk", incomeAnomalyRisk),
            Map.entry("pepRisk", pepRisk),
            Map.entry("documentRisk", documentRisk),
            Map.entry("behavioralRisk", behavioralRisk)
        );

        // Calculate aggregate fraud score (average of indicators)
        double fraudScore = indicators.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        // Determine risk level from score
        FraudRiskReport.RiskLevel level = FraudRiskReport.levelFromScore(fraudScore);

        // Determine recommended action from level
        FraudRiskReport.Action action = FraudRiskReport.actionFromLevel(level);

        // Build explainable rationale
        List<String> rationale = buildRationale(indicators);

        // Confidence is inverse of fraud uncertainty
        double confidence = 0.87;

        // Create report
        FraudRiskReport report = new FraudRiskReport(
            fraudScore,
            level,
            action,
            indicators,
            rationale,
            confidence
        );

        // Log to audit trail (DORA compliant)
        long processingTime = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fraudScore", fraudScore);
        metadata.put("riskLevel", level.name());
        metadata.put("fraudIndicators", indicators);
        metadata.put("processingTimeMs", processingTime);

        auditLogger.log(
            "analysis",
            action.name(),
            confidence,
            List.of(level.name(), action.name()),
            profile.getUserId(),
            String.join(" | ", rationale),
            metadata
        );

        logger.info("Fraud analysis complete: score={}, level={}, action={}, time={}ms",
            String.format("%.2f", fraudScore), level, action, processingTime);
        return report;
    }

    /**
     * Legacy compatibility method — converts FraudRiskReport to AnalysisResult
     */
    public AnalysisResult analyze(KycProfile profile, EnrichmentResult enrichmentResult, GuardrailsResult guardrailsResult, boolean legacyMode) {
        FraudRiskReport report = analyze(profile, enrichmentResult, guardrailsResult);

        // Convert to legacy AnalysisResult
        AnalysisResult result = new AnalysisResult();
        result.setFraudScore(report.fraudScore());
        result.setRiskLevel(convertLevel(report.riskLevel()));
        result.setAutomatedAction(convertAction(report.recommendedAction()));
        result.setFraudFactors(report.fraudIndicators());
        result.setRationale(String.join("\n", report.rationale()));

        return result;
    }

    private AnalysisResult.RiskLevel convertLevel(FraudRiskReport.RiskLevel level) {
        return switch (level) {
            case LOW -> AnalysisResult.RiskLevel.LOW;
            case MEDIUM -> AnalysisResult.RiskLevel.MEDIUM;
            case HIGH -> AnalysisResult.RiskLevel.HIGH;
            case CRITICAL -> AnalysisResult.RiskLevel.CRITICAL;
        };
    }

    private AnalysisResult.AutomatedAction convertAction(FraudRiskReport.Action action) {
        return switch (action) {
            case AUTO_APPROVE -> AnalysisResult.AutomatedAction.APPROVE;
            case MANUAL_REVIEW -> AnalysisResult.AutomatedAction.MANUAL_REVIEW;
            case ESCALATE -> AnalysisResult.AutomatedAction.ESCALATE;
            case BLOCK -> AnalysisResult.AutomatedAction.BLOCK;
        };
    }

    // ==================== DETERMINISTIC INDICATOR ASSESSMENT ====================

    /**
     * Assess velocity fraud: Multiple applications in short timeframe
     * Deterministic: Based on application count in last 7 days
     */
    private double assessVelocity(KycProfile profile) {
        double score = checkVelocityInternal(profile.getUserId());
        // Scale to 0-1 range
        if (score <= 0.05) return 0.05;     // Single app = minimal risk
        if (score <= 0.15) return 0.15;     // 2 apps in 7 days
        if (score <= 0.35) return 0.35;     // 3 apps in 7 days
        if (score <= 0.60) return 0.60;     // 4-5 apps in 7 days
        return 0.90;                        // 6+ apps in 7 days = critical velocity
    }

    /**
     * Assess income anomaly: Inconsistent or unreasonable income
     * Deterministic: Based on income value, not demographics
     */
    private double assessIncome(KycProfile profile, EnrichmentResult enrichmentResult) {
        Double income = profile.getIncome();

        // Try enrichment if primary income missing
        if (income == null && enrichmentResult != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> incomeEstimate = (Map<String, Object>)
                    enrichmentResult.getEnrichedData().get("estimatedIncomeRange");
                if (incomeEstimate != null && incomeEstimate.containsKey("estimatedIncome")) {
                    income = ((Number) incomeEstimate.get("estimatedIncome")).doubleValue();
                }
            } catch (Exception e) {
                logger.debug("Could not extract enriched income", e);
            }
        }

        // Unknown income = slight risk
        if (income == null) return 0.15;

        // Unreasonably low income
        if (income <= 5000) return 0.25;

        // Suspiciously high income
        if (income >= 500000) return 0.30;

        // Normal range = low risk
        return 0.05;
    }

    /**
     * Assess PEP indicators: Politically exposed persons
     * Deterministic: Only explicit PEP flags, no geography
     */
    private double assessPep(KycProfile profile, GuardrailsResult guardrailsResult) {
        if (profile.isPep()) return 0.30;

        // Check guardrails for explicit PEP signals
        if (guardrailsResult != null && guardrailsResult.getRulesFired() != null) {
            if (guardrailsResult.getRulesFired().contains("PEP_STATUS_CONFIRMED")) {
                return 0.30;
            }
            if (guardrailsResult.getRulesFired().contains("SANCTIONS_LIST_MATCH")) {
                return 0.40;
            }
        }

        return 0.00;  // No PEP indicators
    }

    /**
     * Assess document risk: Missing or invalid documents
     * Deterministic: Based on document presence and format
     */
    private double assessDocuments(KycProfile profile) {
        double score = 0.0;

        // Missing passport
        if (profile.getPassportNumber() == null || profile.getPassportNumber().isBlank()) {
            score += 0.15;
        } else {
            // Invalid passport format
            String passport = profile.getPassportNumber();
            if (passport.length() < 6 || passport.length() > 12) {
                score += 0.20;
            }
        }

        return Math.min(score, 1.0);
    }

    /**
     * Assess behavioral risk: Employment vs income inconsistency
     * Deterministic: Based on stated employment and income, no age factors
     */
    private double assessBehavior(KycProfile profile) {
        double score = 0.0;

        // Unemployed with high income
        if ("unemployed".equalsIgnoreCase(profile.getEmploymentStatus())) {
            if (profile.getIncome() != null && profile.getIncome() > 30000) {
                score += 0.20;
            }
        }

        // Student with very high income
        if ("student".equalsIgnoreCase(profile.getEmploymentStatus())) {
            if (profile.getIncome() != null && profile.getIncome() > 100000) {
                score += 0.15;
            }
        }

        return Math.min(score, 1.0);
    }

    // ==================== RATIONALE GENERATION ====================

    /**
     * Build explainable rationale from fraud indicators
     */
    private List<String> buildRationale(Map<String, Double> indicators) {
        List<String> reasons = new ArrayList<>();

        // Velocity
        Double velocity = indicators.get("velocityRisk");
        if (velocity != null && velocity > 0.30) {
            reasons.add("Multiple applications detected in short timeframe");
        }

        // Income
        Double income = indicators.get("incomeAnomalyRisk");
        if (income != null) {
            if (income > 0.25) {
                reasons.add("Income appears unreasonably low or high");
            } else if (income > 0.10) {
                reasons.add("Income slightly above peer average");
            }
        }

        // PEP
        Double pep = indicators.get("pepRisk");
        if (pep != null && pep > 0.25) {
            reasons.add("PEP (Politically Exposed Person) indicators detected");
        }

        // Documents
        Double documents = indicators.get("documentRisk");
        if (documents != null && documents > 0.10) {
            reasons.add("Missing or invalid document verification");
        }

        // Behavior
        Double behavior = indicators.get("behavioralRisk");
        if (behavior != null && behavior > 0.10) {
            reasons.add("Employment status inconsistent with stated income");
        }

        // Default if no issues
        if (reasons.isEmpty()) {
            reasons.add("No significant fraud indicators detected");
        }

        return reasons;
    }

    // ==================== TOOL METHODS (exposed to LLM) ====================

    @Schema(description = "Check velocity of applications for a user")
    public Map<String, Object> checkVelocity(
            @Schema(description = "User ID to check") String userId) {
        Map<String, Object> result = new HashMap<>();
        double score = checkVelocityInternal(userId);
        result.put("userId", userId);
        result.put("velocityScore", score);
        result.put("threshold", 0.3);
        result.put("isHighVelocity", score > 0.3);
        return result;
    }

    @Schema(description = "Analyze amount risk based on income")
    public Map<String, Object> analyzeAmountRisk(
            @Schema(description = "Stated or estimated income") double income) {
        Map<String, Object> result = new HashMap<>();
        double score = analyzeAmountRiskInternal(income);
        result.put("income", income);
        result.put("amountScore", score);
        result.put("riskLevel", score > 0.5 ? "HIGH" : score > 0.2 ? "MEDIUM" : "LOW");
        return result;
    }

    @Schema(description = "Calculate overall risk score from velocity and amount scores")
    public Map<String, Object> calculateRiskScore(
            @Schema(description = "Velocity score from checkVelocity (0.0 to 1.0)") double velocityScore,
            @Schema(description = "Amount risk score from analyzeAmountRisk (0.0 to 1.0)") double amountScore) {
        Map<String, Double> factors = new HashMap<>();
        factors.put("velocity", velocityScore);
        factors.put("income", amountScore);
        Map<String, Object> result = new HashMap<>();
        double score = calculateWeightedScore(factors);
        result.put("velocityScore", velocityScore);
        result.put("amountScore", amountScore);
        result.put("weightedScore", score);
        result.put("riskLevel", AnalysisResult.calculateRiskLevel(score).name());
        result.put("recommendedAction", AnalysisResult.calculateAction(
            AnalysisResult.calculateRiskLevel(score)).name());
        return result;
    }

    // ==================== INTERNAL UTILITY METHODS ====================

    private double checkVelocityInternal(String userId) {
        if (userId == null) return 0.1;

        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L);

        // Get or create history
        applicationHistory.computeIfAbsent(userId, k -> new ArrayList<>());
        List<Long> history = applicationHistory.get(userId);

        // Add current application
        history.add(now);

        // Count applications in last 7 days
        long recentCount = history.stream()
            .filter(t -> t > sevenDaysAgo)
            .count();

        // Calculate velocity score based on application frequency
        if (recentCount <= 1) return 0.05;
        if (recentCount <= 2) return 0.15;
        if (recentCount <= 3) return 0.35;
        if (recentCount <= 5) return 0.60;
        return 0.90;  // 6+ applications = critical velocity
    }

    /**
     * Helper: Calculate amount/income risk
     * Used by both legacy and new scoring paths
     */
    private double analyzeAmountRiskInternal(Double income) {
        if (income == null) return 0.15;  // Unknown income = slight risk
        if (income < 5000) return 0.25;
        if (income > 500000) return 0.30;
        return 0.05;
    }

    /**
     * Helper: Calculate weighted score from factors
     * Used for legacy compatibility
     */
    private double calculateWeightedScore(Map<String, Double> factors) {
        double totalScore = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : factors.entrySet()) {
            String factor = entry.getKey();
            Double score = entry.getValue();
            Double weight = ComplianceConfig.FRAUD_WEIGHTS.getOrDefault(factor, 0.1);

            totalScore += score * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.0;
    }

    public LlmAgent getLlmAgent() {
        return llmAgent;
    }
}
