package com.dnb.kyc.model;

import java.util.*;

/**
 * Result from Guardrails Agent validation
 */
public class GuardrailsResult {
    
    private boolean passed;
    private List<String> issues;
    private BiasAssessment biasAssessment;
    private List<String> rulesFired;

    public GuardrailsResult() {
        this.issues = new ArrayList<>();
        this.rulesFired = new ArrayList<>();
    }

    // Getters and Setters
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }
    public void addIssue(String issue) { this.issues.add(issue); }

    public BiasAssessment getBiasAssessment() { return biasAssessment; }
    public void setBiasAssessment(BiasAssessment biasAssessment) { this.biasAssessment = biasAssessment; }

    public List<String> getRulesFired() { return rulesFired; }
    public void setRulesFired(List<String> rulesFired) { this.rulesFired = rulesFired; }
    public void addRuleFired(String rule) { this.rulesFired.add(rule); }

    // Inner class for bias assessment
    public static class BiasAssessment {
        private boolean biasDetected;
        private double biasScore;
        private List<String> factors;

        public BiasAssessment() {
            this.factors = new ArrayList<>();
        }

        public boolean isBiasDetected() { return biasDetected; }
        public void setBiasDetected(boolean biasDetected) { this.biasDetected = biasDetected; }

        public double getBiasScore() { return biasScore; }
        public void setBiasScore(double biasScore) { this.biasScore = biasScore; }

        public List<String> getFactors() { return factors; }
        public void setFactors(List<String> factors) { this.factors = factors; }
        public void addFactor(String factor) { this.factors.add(factor); }
    }
}
