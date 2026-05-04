package com.dnb.kyc.model;

import java.util.*;

/**
 * Result from Analysis Agent fraud detection
 */
public class AnalysisResult {
    
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AutomatedAction {
        APPROVE, MANUAL_REVIEW, ESCALATE, BLOCK
    }

    private double fraudScore;
    private RiskLevel riskLevel;
    private AutomatedAction automatedAction;
    private Map<String, Double> fraudFactors;
    private String rationale;
    private List<String> redFlags;

    public AnalysisResult() {
        this.fraudFactors = new HashMap<>();
        this.redFlags = new ArrayList<>();
    }

    // Getters and Setters
    public double getFraudScore() { return fraudScore; }
    public void setFraudScore(double fraudScore) { this.fraudScore = fraudScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public AutomatedAction getAutomatedAction() { return automatedAction; }
    public void setAutomatedAction(AutomatedAction automatedAction) { this.automatedAction = automatedAction; }

    public Map<String, Double> getFraudFactors() { return fraudFactors; }
    public void setFraudFactors(Map<String, Double> fraudFactors) { this.fraudFactors = fraudFactors; }
    public void addFraudFactor(String factor, double score) { this.fraudFactors.put(factor, score); }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public List<String> getRedFlags() { return redFlags; }
    public void setRedFlags(List<String> redFlags) { this.redFlags = redFlags; }
    public void addRedFlag(String flag) { this.redFlags.add(flag); }

    // Helper to determine risk level from score
    public static RiskLevel calculateRiskLevel(double score) {
        if (score < 0.25) return RiskLevel.LOW;
        if (score < 0.60) return RiskLevel.MEDIUM;
        if (score < 0.85) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    // Helper to determine action from risk level
    public static AutomatedAction calculateAction(RiskLevel level) {
        return switch (level) {
            case LOW -> AutomatedAction.APPROVE;
            case MEDIUM -> AutomatedAction.MANUAL_REVIEW;
            case HIGH -> AutomatedAction.ESCALATE;
            case CRITICAL -> AutomatedAction.BLOCK;
        };
    }
}
