package com.dnb.kyc.util;

import com.dnb.kyc.model.AuditEntry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ✅ Cryptographic Hash Utility
 * 
 * Provides SHA-256 hash chain enforcement for audit trail integrity.
 * Used by AuditLoggerAgent to create deterministic, replayable hash chains.
 */
public class HashUtil {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Calculate SHA-256 hash of an audit entry with previous hash.
     * 
     * This creates a cryptographic link between consecutive entries,
     * making the entire audit trail tamper-evident.
     */
    public static String hashWithPrevious(AuditEntry entry, String previousHash) {
        String contentToHash = String.join("|",
                entry.eventId(),
                entry.timestamp().toString(),
                entry.workflow(),
                entry.agent(),
                String.valueOf(entry.aiAssisted()),
                String.valueOf(entry.finalDecisionByAi()),
                entry.model(),
                entry.kycStatus(),
                entry.riskLevel(),
                String.valueOf(entry.decisionAllowed()),
                String.valueOf(entry.rejectAllowed()),
                String.valueOf(entry.humanReviewRequired()),
                String.join(",", entry.allowedActions()),
                String.join(",", entry.policyRefs()),
                previousHash
        );
        
        return calculateSHA256(contentToHash);
    }

    /**
     * Calculate raw SHA-256 hash of a string.
     */
    public static String calculateSHA256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify that a hash is correctly formatted (64 hex chars).
     */
    public static boolean isValidHash(String hash) {
        return hash != null && hash.matches("^[a-f0-9]{64}$");
    }
}
