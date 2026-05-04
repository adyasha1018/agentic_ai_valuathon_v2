package com.dnb.kyc.config;

import java.util.*;

/**
 * Compliance Configuration
 * 
 * Centralized compliance rules for KYC processing.
 * Contains PEP lists, risk thresholds, demographic data, and validation rules.
 */
public final class ComplianceConfig {

    private ComplianceConfig() {
        // Prevent instantiation
    }

    // ==================== REQUIRED FIELDS ====================
    public static final List<String> REQUIRED_FIELDS = List.of(
        "firstName",
        "lastName",
        "nationality",
        "age"
    );

    // ==================== PEP HIGH-RISK COUNTRIES ====================
    public static final Set<String> PEP_COUNTRIES = Set.of(
        "KP", // North Korea
        "IR", // Iran
        "SY", // Syria
        "CU"  // Cuba
    );

    // ==================== RISK LEVEL THRESHOLDS ====================
    public static final double RISK_LOW = 0.25;
    public static final double RISK_MEDIUM = 0.60;
    public static final double RISK_HIGH = 0.85;
    public static final double RISK_CRITICAL = 1.0;

    // ==================== DEMOGRAPHIC PROFILES BY COUNTRY ====================
    public static final Map<String, DemographicProfile> DEMOGRAPHIC_PROFILES;
    
    static {
        Map<String, DemographicProfile> profiles = new HashMap<>();
        profiles.put("NL", new DemographicProfile(63000, 45, "EUR", "LOW"));
        profiles.put("DE", new DemographicProfile(56000, 47, "EUR", "LOW"));
        profiles.put("FR", new DemographicProfile(47000, 42, "EUR", "LOW"));
        profiles.put("GB", new DemographicProfile(49000, 40, "GBP", "LOW"));
        profiles.put("US", new DemographicProfile(76000, 38, "USD", "LOW"));
        profiles.put("RO", new DemographicProfile(15000, 42, "RON", "MEDIUM"));
        profiles.put("BG", new DemographicProfile(13000, 44, "BGN", "MEDIUM"));
        profiles.put("PL", new DemographicProfile(18000, 42, "PLN", "MEDIUM"));
        profiles.put("HU", new DemographicProfile(19000, 43, "HUF", "MEDIUM"));
        profiles.put("UA", new DemographicProfile(5000, 41, "UAH", "HIGH"));
        profiles.put("RU", new DemographicProfile(12000, 40, "RUB", "HIGH"));
        profiles.put("CN", new DemographicProfile(12500, 39, "CNY", "MEDIUM"));
        profiles.put("IN", new DemographicProfile(2500, 29, "INR", "MEDIUM"));
        profiles.put("BR", new DemographicProfile(9000, 34, "BRL", "MEDIUM"));
        DEMOGRAPHIC_PROFILES = Collections.unmodifiableMap(profiles);
    }

    // ==================== FRAUD INDICATOR WEIGHTS ====================
    public static final Map<String, Double> FRAUD_WEIGHTS;
    
    static {
        Map<String, Double> weights = new HashMap<>();
        weights.put("velocity", 0.25);       // Multiple applications in short time
        weights.put("amount", 0.20);         // Transaction amount vs income
        weights.put("document", 0.20);       // Document verification
        weights.put("behavior", 0.15);       // Behavioral patterns
        weights.put("pep", 0.20);            // Politically Exposed Person
        FRAUD_WEIGHTS = Collections.unmodifiableMap(weights);
    }

    // ==================== BIAS THRESHOLDS ====================
    public static final double AGE_BIAS_THRESHOLD = 10.0;      // Years from country average
    public static final double GDP_BIAS_THRESHOLD = 0.5;       // 50% below EU average

    // ==================== INNER CLASS: Demographic Profile ====================
    public static class DemographicProfile {
        private final double avgGdpPerCapita;
        private final int avgAge;
        private final String currency;
        private final String riskLevel;

        public DemographicProfile(double avgGdpPerCapita, int avgAge, String currency, String riskLevel) {
            this.avgGdpPerCapita = avgGdpPerCapita;
            this.avgAge = avgAge;
            this.currency = currency;
            this.riskLevel = riskLevel;
        }

        public double getAvgGdpPerCapita() { return avgGdpPerCapita; }
        public int getAvgAge() { return avgAge; }
        public String getCurrency() { return currency; }
        public String getRiskLevel() { return riskLevel; }
    }
}
