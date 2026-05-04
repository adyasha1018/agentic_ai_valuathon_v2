package com.dnb.kyc.agents.guardrails.enrichment;

/**
 * Exception thrown when enrichment guardrails are violated.
 * 
 * This is a hard stop that blocks the KYC pipeline from proceeding.
 */
public class EnrichmentGuardrailsViolation extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final EnrichmentGuardrailsOutput guardrailsOutput;

    public EnrichmentGuardrailsViolation(String message) {
        super(message);
        this.guardrailsOutput = null;
    }

    public EnrichmentGuardrailsViolation(String message, EnrichmentGuardrailsOutput output) {
        super(message);
        this.guardrailsOutput = output;
    }

    public EnrichmentGuardrailsViolation(String message, Throwable cause) {
        super(message, cause);
        this.guardrailsOutput = null;
    }

    public EnrichmentGuardrailsOutput getGuardrailsOutput() {
        return guardrailsOutput;
    }
}
