package com.dnb.kyc.model;

import java.util.List;
import java.util.Map;

/**
 * Fraud Risk Report — Deterministic, Audit-Ready Output
 *
 * This record represents the structured output from the Analysis Agent.
 * It is designed to be:
 * - Serializable to JSON
 * - Hash-chain friendly for audit logging
 * - Replayable for regulator review
 * - Clear for compliance officers
 *
 * Fraud indicators are explicit and deterministic (no hidden heuristics).
 */
public record FraudRiskReport(
        double fraudScore,
        RiskLevel riskLevel,
        Action recommendedAction,
        Map<String, Double> fraudIndicators,
        List<String> rationale,
        double confidence
) {

    public enum RiskLevel {
        LOW,        // 0.00-0.25: Auto-approve
        MEDIUM,     // 0.25-0.60: Manual review
        HIGH,       // 0.60-0.85: Escalate
        CRITICAL    // 0.85-1.00: Block
    }

    public enum Action {
        AUTO_APPROVE,   // Risk level LOW
        MANUAL_REVIEW,  // Risk level MEDIUM
        ESCALATE,       // Risk level HIGH
        BLOCK           // Risk level CRITICAL
    }

    /**
     * Helper: Calculate RiskLevel from fraud score
     */
    public static RiskLevel levelFromScore(double score) {
        if (score < 0.25) return RiskLevel.LOW;
        if (score < 0.60) return RiskLevel.MEDIUM;
        if (score < 0.85) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    /**
     * Helper: Calculate Action from RiskLevel
     */
    public static Action actionFromLevel(RiskLevel level) {
        return switch (level) {
            case LOW -> Action.AUTO_APPROVE;
            case MEDIUM -> Action.MANUAL_REVIEW;
            case HIGH -> Action.ESCALATE;
            case CRITICAL -> Action.BLOCK;
        };
    }

    /**
     * Check if fraud score is acceptable
     */
    public boolean isAcceptable() {
        return riskLevel == RiskLevel.LOW || riskLevel == RiskLevel.MEDIUM;
    }

    /**
     * Check if report requires escalation
     */
    public boolean requiresEscalation() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }
}
