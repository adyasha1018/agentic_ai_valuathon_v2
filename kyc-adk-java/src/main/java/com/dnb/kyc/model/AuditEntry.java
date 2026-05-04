package com.dnb.kyc.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ✅ Audit Entry Record (Immutable)
 * 
 * DORA/DNB/GDPR-compliant audit entry.
 * Each entry is cryptographically linked to the previous via SHA-256.
 * 
 * This record forms the immutable, auditable backbone of the KYC system.
 * No identity data is stored (GDPR Article 5(1)(c) - Data Minimization).
 */
public record AuditEntry(
        String eventId,
        Instant timestamp,
        String workflow,
        String agent,
        boolean aiAssisted,
        boolean finalDecisionByAi,
        String model,
        boolean piiUsed,
        String kycStatus,
        String riskLevel,
        List<Map<String, Object>> flags,
        boolean decisionAllowed,
        boolean rejectAllowed,
        boolean humanReviewRequired,
        List<String> allowedActions,
        List<String> policyRefs,
        String previousHash,
        String currentHash
) {
    /**
     * Compact JSON representation for logging/export
     */
    @Override
    public String toString() {
        return "{" +
                "eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", agent='" + agent + '\'' +
                ", kycStatus='" + kycStatus + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", humanReviewRequired=" + humanReviewRequired +
                ", hash=" + (currentHash != null ? currentHash.substring(0, 8) + "..." : "null") +
                '}';
    }
}

