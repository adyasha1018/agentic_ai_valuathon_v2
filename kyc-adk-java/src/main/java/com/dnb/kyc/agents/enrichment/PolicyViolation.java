package com.dnb.kyc.agents.enrichment;

/**
 * Runtime exception thrown when enrichment policy is violated.
 * 
 * Policy violations are non-bypassable and result in:
 * 1. Immediate rejection of the enriched field
 * 2. Audit logging of the violation
 * 3. Potential escalation to compliance team
 * 
 * Common violations:
 * - Unapproved enrichment source
 * - Overwrite of customer-provided data
 * - Low confidence enrichment
 * - GDPR data minimization violation
 * - Forbidden compliance intelligence enrichment
 */
public class PolicyViolation extends RuntimeException {

    private final String violationType;
    private final String fieldName;
    private final String sourceInfo;

    public PolicyViolation(String message) {
        super(message);
        this.violationType = "GENERAL";
        this.fieldName = null;
        this.sourceInfo = null;
    }

    public PolicyViolation(String message, String violationType) {
        super(message);
        this.violationType = violationType;
        this.fieldName = null;
        this.sourceInfo = null;
    }

    public PolicyViolation(String message, String violationType, String fieldName, String sourceInfo) {
        super(message);
        this.violationType = violationType;
        this.fieldName = fieldName;
        this.sourceInfo = sourceInfo;
    }

    public String getViolationType() {
        return violationType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getSourceInfo() {
        return sourceInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PolicyViolation[").append(violationType).append("]: ").append(getMessage());
        if (fieldName != null) {
            sb.append(" | Field: ").append(fieldName);
        }
        if (sourceInfo != null) {
            sb.append(" | Source: ").append(sourceInfo);
        }
        return sb.toString();
    }
}
