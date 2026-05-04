package com.dnb.kyc.agents.guardrails.enrichment;

import com.dnb.kyc.model.EnrichmentResult;

/**
 * Input model for Enrichment Guardrails Agent.
 * 
 * Contains the enrichment result to be validated along with
 * contextual metadata for policy evaluation.
 */
public class EnrichmentGuardrailsInput {

    private String kycCaseId;

    /**
     * Output of EnrichmentAgent to be validated
     */
    private EnrichmentResult enrichmentResult;

    /**
     * Contextual metadata (non-risk factors)
     */
    private String jurisdiction;           // e.g. "NL", "DE", "FR"
    private String legalEntityType;        // INDIVIDUAL | COMPANY
    private String enrichmentAgentVersion; // for audit trace

    // Constructors
    public EnrichmentGuardrailsInput() {}

    public EnrichmentGuardrailsInput(String kycCaseId, EnrichmentResult enrichmentResult) {
        this.kycCaseId = kycCaseId;
        this.enrichmentResult = enrichmentResult;
    }

    public EnrichmentGuardrailsInput(String kycCaseId, EnrichmentResult enrichmentResult, 
            String jurisdiction, String legalEntityType, String enrichmentAgentVersion) {
        this.kycCaseId = kycCaseId;
        this.enrichmentResult = enrichmentResult;
        this.jurisdiction = jurisdiction;
        this.legalEntityType = legalEntityType;
        this.enrichmentAgentVersion = enrichmentAgentVersion;
    }

    // Getters and Setters
    public String getKycCaseId() {
        return kycCaseId;
    }

    public void setKycCaseId(String kycCaseId) {
        this.kycCaseId = kycCaseId;
    }

    public EnrichmentResult getEnrichmentResult() {
        return enrichmentResult;
    }

    public void setEnrichmentResult(EnrichmentResult enrichmentResult) {
        this.enrichmentResult = enrichmentResult;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getLegalEntityType() {
        return legalEntityType;
    }

    public void setLegalEntityType(String legalEntityType) {
        this.legalEntityType = legalEntityType;
    }

    public String getEnrichmentAgentVersion() {
        return enrichmentAgentVersion;
    }

    public void setEnrichmentAgentVersion(String enrichmentAgentVersion) {
        this.enrichmentAgentVersion = enrichmentAgentVersion;
    }
}
