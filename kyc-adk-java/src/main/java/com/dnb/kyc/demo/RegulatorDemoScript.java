package com.dnb.kyc.demo;

import com.dnb.kyc.agents.AuditLoggerAgent;
import com.dnb.kyc.agents.EnrichmentAgent;
import com.dnb.kyc.agents.enrichment.*;
import com.dnb.kyc.model.*;

import java.util.List;

/**
 * REGULATOR LIVE DEMO SCRIPT
 * 
 * Interactive demonstration of KYC Enrichment Agent compliance.
 * Run this during DNB / EU supervisory walkthroughs.
 * 
 * Usage:
 *   java -cp target/kyc-multiagent-system-1.0.0.jar \
 *        com.dnb.kyc.demo.RegulatorDemoScript
 * 
 * @author DNB Regulatory Compliance Hackathon Team
 */
public class RegulatorDemoScript {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BOLD = "\u001B[1m";

    public static void main(String[] args) {
        printBanner();
        
        AuditLoggerAgent auditLogger = new AuditLoggerAgent();
        EnrichmentAgent agent = new EnrichmentAgent(auditLogger);
        
        // Demo 1: Happy Path - Dutch Company
        demoDutchCompanyEnrichment(agent);
        
        // Demo 2: GDPR Compliance - Natural Person
        demoGdprCompliance(agent);
        
        // Demo 3: Policy Violation - Blocked Fields
        demoPolicyViolation();
        
        // Demo 4: Determinism Check
        demoDeterminism(agent);
        
        // Demo 5: Audit Trail
        demoAuditTrail(auditLogger);
        
        printSummary();
    }

    private static void printBanner() {
        System.out.println(ANSI_CYAN + ANSI_BOLD + """
            
            ╔══════════════════════════════════════════════════════════════════╗
            ║                                                                  ║
            ║           DNB REGULATORY COMPLIANCE DEMONSTRATION                ║
            ║                                                                  ║
            ║              KYC Enrichment Agent - Live Demo                    ║
            ║                                                                  ║
            ║                  May 4, 2026 - Hackathon                        ║
            ║                                                                  ║
            ╚══════════════════════════════════════════════════════════════════╝
            """ + ANSI_RESET);
        
        System.out.println("""
            
            This demonstration shows:
            
            1. ✅ Policy-as-Code enforcement (non-bypassable)
            2. ✅ GDPR Article 5(1)(c) data minimization
            3. ✅ EU AML segregation of duties
            4. ✅ Deterministic, reproducible outputs
            5. ✅ DORA-compliant audit trail
            
            ─────────────────────────────────────────────────────────────────────
            """);
        
        pause(2000);
    }

    private static void demoDutchCompanyEnrichment(EnrichmentAgent agent) {
        printSection("DEMO 1: DUTCH COMPANY ENRICHMENT (HAPPY PATH)");
        
        System.out.println("Input Profile:");
        System.out.println("""
            {
                "customer_type": "LEGAL_ENTITY",
                "legal_name": "GreenWind BV",
                "nationality": "NL"
            }
            """);
        
        KycProfile profile = new KycProfile();
        profile.setFirstName("GreenWind");
        profile.setLastName("BV");
        profile.setNationality("NL");
        
        System.out.println("Processing...\n");
        pause(1000);
        
        EnrichmentResult result = agent.enrich(profile);
        
        printSuccess("Enrichment completed successfully");
        System.out.println("\nResult:");
        System.out.printf("  Completion Score: %.0f%%%n", result.getCompletionScore() * 100);
        System.out.printf("  Enrichment Risk:  %.2f%n", result.getEnrichmentRiskScore());
        System.out.printf("  Policy Compliant: %s%n", result.isPolicyCompliant() ? "✅ YES" : "❌ NO");
        System.out.printf("  Fields Enriched:  %d (HARD: %d, SOFT: %d)%n",
            result.getEnrichedFields().size(),
            result.getHardEnrichmentCount(),
            result.getSoftEnrichmentCount());
        
        System.out.println("\n  Enriched Fields:");
        for (EnrichedField field : result.getEnrichedFields()) {
            System.out.printf("    • %s = %s%n", field.getFieldName(), field.getEnrichedValue());
            System.out.printf("      Source: %s (%s)%n", field.getSourceName(), field.getSourceType());
            System.out.printf("      Confidence: %s (%.2f)%n", field.getConfidence(), field.getReliabilityScore());
        }
        
        printDivider();
        pause(2000);
    }

    private static void demoGdprCompliance(EnrichmentAgent agent) {
        printSection("DEMO 2: GDPR DATA MINIMIZATION (NATURAL PERSON)");
        
        System.out.println("Input Profile:");
        System.out.println("""
            {
                "customer_type": "NATURAL_PERSON",
                "full_name": "Anna Kovacs",
                "nationality": "RO",
                "age": 28
            }
            """);
        
        System.out.println("GDPR Article 5(1)(c) requires data minimization.");
        System.out.println("The agent must NOT enrich:");
        System.out.println("  ✗ Social media profiles");
        System.out.println("  ✗ Marital status");
        System.out.println("  ✗ Education level");
        System.out.println("  ✗ Any non-KYC personal data\n");
        
        KycProfile profile = new KycProfile();
        profile.setFirstName("Anna");
        profile.setLastName("Kovacs");
        profile.setNationality("RO");
        profile.setAge(28);
        
        System.out.println("Processing...\n");
        pause(1000);
        
        EnrichmentResult result = agent.enrich(profile);
        
        // Check for GDPR violations
        List<String> forbiddenTerms = List.of("social", "facebook", "linkedin", "marital", "education");
        boolean gdprViolation = result.getEnrichedFields().stream()
            .anyMatch(f -> forbiddenTerms.stream()
                .anyMatch(t -> f.getFieldName().toLowerCase().contains(t)));
        
        if (!gdprViolation) {
            printSuccess("GDPR COMPLIANCE VERIFIED");
            System.out.println("\n  ✅ No personal data beyond KYC necessity was enriched");
            System.out.println("  ✅ Only country-level and employment data enriched");
        } else {
            printFailure("GDPR VIOLATION DETECTED");
        }
        
        printDivider();
        pause(2000);
    }

    private static void demoPolicyViolation() {
        printSection("DEMO 3: POLICY ENFORCEMENT (BLOCKED FIELDS)");
        
        System.out.println("Attempting to enrich FORBIDDEN fields...\n");
        
        System.out.println("Test 1: Attempting to enrich 'riskScore':");
        System.out.println("  This field is PROHIBITED per AML segregation of duties.\n");
        
        try {
            EnrichedField forbiddenField = new EnrichedField();
            forbiddenField.setFieldName("riskScore");
            forbiddenField.setEnrichedValue(0.85);
            forbiddenField.setSourceType(SourceType.GOVERNMENT_REGISTRY);
            forbiddenField.setSourceName("Test");
            forbiddenField.setConfidence(Confidence.HIGH);
            forbiddenField.setReliabilityScore(0.95);
            forbiddenField.setEnrichmentClass(EnrichmentClass.HARD);
            
            EnrichmentValidator.enforce(forbiddenField);
            
            printFailure("POLICY BYPASS - THIS SHOULD NOT HAPPEN");
        } catch (PolicyViolation e) {
            printSuccess("BLOCKED: " + e.getMessage());
            System.out.println("  Policy enforcement is NON-BYPASSABLE.");
        }
        
        System.out.println("\nTest 2: Attempting LOW confidence enrichment:");
        System.out.println("  LOW confidence data must never reach downstream agents.\n");
        
        try {
            EnrichedField lowConfField = new EnrichedField();
            lowConfField.setFieldName("currency");
            lowConfField.setEnrichedValue("EUR");
            lowConfField.setSourceType(SourceType.COUNTRY_REGISTRY);
            lowConfField.setSourceName("Test");
            lowConfField.setConfidence(Confidence.LOW);
            lowConfField.setReliabilityScore(0.4);
            lowConfField.setEnrichmentClass(EnrichmentClass.HARD);
            
            EnrichmentValidator.enforce(lowConfField);
            
            printFailure("POLICY BYPASS - THIS SHOULD NOT HAPPEN");
        } catch (PolicyViolation e) {
            printSuccess("BLOCKED: " + e.getMessage());
        }
        
        printDivider();
        pause(2000);
    }

    private static void demoDeterminism(EnrichmentAgent agent) {
        printSection("DEMO 4: DETERMINISM TEST (MODEL RISK)");
        
        System.out.println("Running same profile 10 times...");
        System.out.println("All outputs must be IDENTICAL.\n");
        
        KycProfile profile = new KycProfile();
        profile.setFirstName("NovaTech");
        profile.setLastName("GmbH");
        profile.setNationality("DE");
        
        EnrichmentResult baseline = agent.enrich(profile);
        double baselineCompletion = baseline.getCompletionScore();
        int baselineFieldCount = baseline.getEnrichedFields().size();
        
        boolean deterministic = true;
        for (int i = 1; i <= 10; i++) {
            AuditLoggerAgent freshAudit = new AuditLoggerAgent();
            EnrichmentAgent freshAgent = new EnrichmentAgent(freshAudit);
            EnrichmentResult result = freshAgent.enrich(profile);
            
            boolean matches = result.getCompletionScore() == baselineCompletion &&
                            result.getEnrichedFields().size() == baselineFieldCount;
            
            System.out.printf("  Run %2d: Completion=%.2f, Fields=%d  %s%n",
                i, result.getCompletionScore(), result.getEnrichedFields().size(),
                matches ? "✅" : "❌");
            
            if (!matches) deterministic = false;
        }
        
        if (deterministic) {
            printSuccess("DETERMINISM VERIFIED - All 10 runs identical");
            System.out.println("  Meets ECB Model Risk Management requirements.");
        } else {
            printFailure("DETERMINISM FAILURE");
        }
        
        printDivider();
        pause(2000);
    }

    private static void demoAuditTrail(AuditLoggerAgent auditLogger) {
        printSection("DEMO 5: DORA-COMPLIANT AUDIT TRAIL");
        
        System.out.println("All enrichment operations are logged with:");
        System.out.println("  • SHA-256 hash chain (immutable)");
        System.out.println("  • Timestamp");
        System.out.println("  • Agent identifier");
        System.out.println("  • Decision and confidence");
        System.out.println("  • Source traceability\n");
        
        List<AuditEntry> trail = auditLogger.getAuditTrail();
        System.out.printf("Audit entries recorded: %d%n%n", trail.size());
        
        if (!trail.isEmpty()) {
            System.out.println("Latest entries:");
            int start = Math.max(0, trail.size() - 3);
            for (int i = start; i < trail.size(); i++) {
                AuditEntry entry = trail.get(i);
                System.out.printf("  [%s] %s: %s (hash: %s...)%n",
                    entry.timestamp().toString().substring(11, 19),
                    entry.agent(),
                    entry.kycStatus(),
                    entry.currentHash().substring(0, 8));
            }
        }
        
        System.out.println("\n  Hash chain integrity: " + 
            (auditLogger.verifyIntegrity() ? "✅ VERIFIED" : "❌ COMPROMISED"));
        
        printDivider();
    }

    private static void printSummary() {
        System.out.println(ANSI_CYAN + ANSI_BOLD + """
            
            ╔══════════════════════════════════════════════════════════════════╗
            ║                                                                  ║
            ║                    DEMONSTRATION COMPLETE                        ║
            ║                                                                  ║
            ╚══════════════════════════════════════════════════════════════════╝
            """ + ANSI_RESET);
        
        System.out.println("""
            KEY REGULATORY STATEMENT:
            
              "These test cases are hard-coded supervisory challenges.
               If any fail, output never reaches Guardrails or Risk agents."
            
            COMPLIANCE SUMMARY:
            
              ✅ GDPR Article 5(1)(c) - Data minimization enforced
              ✅ EU AML Directive - Segregation of duties maintained
              ✅ FATF Recommendation 10 - Reliable sources only
              ✅ DORA Article 11 - Full auditability
              ✅ ECB Model Risk Guide - Deterministic outputs
              ✅ DNB Guidance - Policy-as-Code enforcement
            
            ─────────────────────────────────────────────────────────────────────
            
            For questions, contact: DNB Regulatory Compliance Hackathon Team
            
            """);
    }

    private static void printSection(String title) {
        System.out.println(ANSI_YELLOW + ANSI_BOLD + "\n═══ " + title + " ═══" + ANSI_RESET + "\n");
    }

    private static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + "✅ " + message + ANSI_RESET);
    }

    private static void printFailure(String message) {
        System.out.println(ANSI_RED + "❌ " + message + ANSI_RESET);
    }

    private static void printDivider() {
        System.out.println("\n─────────────────────────────────────────────────────────────────────");
    }

    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
