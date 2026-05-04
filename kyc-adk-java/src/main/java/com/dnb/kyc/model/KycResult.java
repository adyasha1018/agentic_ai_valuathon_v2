package com.dnb.kyc.model;

import java.util.*;

/**
 * Complete KYC Processing Result
 * 
 * Aggregates results from all agents in the pipeline.
 */
public class KycResult {

    public enum Status {
        APPROVED,
        MANUAL_REVIEW,
        ESCALATED,
        REJECTED
    }

    private Status status;
    private GuardrailsResult guardrailsResult;
    private EnrichmentResult enrichmentResult;
    private AnalysisResult analysisResult;
    private FraudRiskReport fraudRiskReport;  // ★ Production-grade fraud report
    private List<AuditEntry> auditTrail;
    private long processingTimeMs;
    private String sessionId;
    private String summary;

    public KycResult() {
        this.auditTrail = new ArrayList<>();
    }

    // Getters and Setters
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public GuardrailsResult getGuardrailsResult() { return guardrailsResult; }
    public void setGuardrailsResult(GuardrailsResult guardrailsResult) { this.guardrailsResult = guardrailsResult; }

    public EnrichmentResult getEnrichmentResult() { return enrichmentResult; }
    public void setEnrichmentResult(EnrichmentResult enrichmentResult) { this.enrichmentResult = enrichmentResult; }

    public AnalysisResult getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(AnalysisResult analysisResult) { this.analysisResult = analysisResult; }

    public FraudRiskReport getFraudRiskReport() { return fraudRiskReport; }
    public void setFraudRiskReport(FraudRiskReport fraudRiskReport) { this.fraudRiskReport = fraudRiskReport; }

    public List<AuditEntry> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<AuditEntry> auditTrail) { this.auditTrail = auditTrail; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    // Convert AnalysisResult action to KycResult status
    public static Status fromAnalysisAction(AnalysisResult.AutomatedAction action) {
        return switch (action) {
            case APPROVE -> Status.APPROVED;
            case MANUAL_REVIEW -> Status.MANUAL_REVIEW;
            case ESCALATE -> Status.ESCALATED;
            case BLOCK -> Status.REJECTED;
        };
    }

    // Convert FraudRiskReport action to KycResult status
    public static Status fromFraudAction(FraudRiskReport.Action action) {
        return switch (action) {
            case AUTO_APPROVE -> Status.APPROVED;
            case MANUAL_REVIEW -> Status.MANUAL_REVIEW;
            case ESCALATE -> Status.ESCALATED;
            case BLOCK -> Status.REJECTED;
        };
    }
}
