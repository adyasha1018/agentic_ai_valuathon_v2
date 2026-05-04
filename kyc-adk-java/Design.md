flowchart TD
    USER(["👤 User / Dev UI\nhttp://localhost:8888"])
    INPUT["📥 KYC JSON Input\n{kycCaseId, firstName, nationality,\ndateOfBirth, expectedMonthlyVolumeEUR...}"]

    subgraph COORD ["🔀 kyc_coordinator  —  SequentialAgent  (runs all agents in fixed order)"]
        direction TB

        subgraph G ["🛡️ guardrails_agent  —  Step 1: Compliance Gate"]
            G1["validateRequiredFields(profileJson)\n→ checks firstName, lastName, nationality, age"]
            G2["checkPepStatus(countryCode)\n→ flags KP, IR, SY, CU as high-risk"]
            G3["detectBias(dateOfBirth, nationality)\n→ age & GDP bias check"]
            G1 --> G2 --> G3
        end

        subgraph E ["📊 kyc_enrichment_agent  —  Step 2: Data Completion"]
            E1["lookupCountryData(countryCode)\n→ GDP, currency, risk tier"]
            E2["estimateIncomeRange(countryCode, employment)\n→ expected income band"]
            E3["verifyEmployment(employer, countryCode)\n→ company register check"]
            E1 --> E2 --> E3
        end

        subgraph A ["🔍 analysis_agent  —  Step 3: Fraud & Risk Scoring"]
            A1["checkVelocity(kycCaseId)\n→ multiple apps in 7 days?\nscore: 0.05 → 0.90"]
            A2["analyzeAmountRisk(expectedMonthlyVolumeEUR)\n→ volume vs income model\n>500k EUR = HIGH"]
            A3["calculateRiskScore(velocityScore, amountScore)\n→ weighted score\n→ LOW / MEDIUM / HIGH / CRITICAL\n→ APPROVE / MANUAL_REVIEW / ESCALATE / BLOCK"]
            A1 --> A2 --> A3
        end

        subgraph AU ["🔐 audit_logger_agent  —  Step 4: Immutable Audit Trail"]
            AU1["logAuditEvent(eventId, agentName, score,\nrulesFired, userId, regulatorNote...)\n→ SHA-256 hash chain\n→ No raw PII\n→ DORA / DNB / GDPR Article-22 compliant\n→ finalDecisionByAi = false"]
        end

        subgraph S ["📋 kyc_summary_agent  —  Step 5: Final Report"]
            S1["Reads full conversation history\n(no tools called)\n→ Structured summary output"]
        end

        G --> E --> A --> AU --> S
    end

    OUTPUT["✅ KYC PROCESSING COMPLETE\n━━━━━━━━━━━━━━━━━━━━━\n🛡️ GUARDRAILS  : PASS / FAIL\n📊 ENRICHMENT  : fields enriched\n🔍 ANALYSIS    : Score | Risk | Decision\n🔐 AUDIT       : LOGGED — event ID\n━━━━━━━━━━━━━━━━━━━━━\nFINAL DECISION : APPROVE / MANUAL_REVIEW\n               ESCALATE / BLOCK"]

    USER --> INPUT --> COORD --> OUTPUT

    style G fill:#fff3e0,stroke:#f57c00,color:#000
    style E fill:#e8f5e9,stroke:#388e3c,color:#000
    style A fill:#e3f2fd,stroke:#1976d2,color:#000
    style AU fill:#fce4ec,stroke:#c62828,color:#000
    style S fill:#f3e5f5,stroke:#7b1fa2,color:#000
    style COORD fill:#f5f5f5,stroke:#616161,color:#000
    style OUTPUT fill:#e8f5e9,stroke:#2e7d32,color:#000
    style USER fill:#e3f2fd,stroke:#1565c0,color:#000
    style INPUT fill:#fff8e1,stroke:#f9a825,color:#000