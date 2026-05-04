package com.dnb.kyc;

import com.dnb.kyc.agents.*;
import com.dnb.kyc.agents.guardrails.enrichment.EnrichmentGuardrailsAgent;
import com.google.adk.web.AdkWebServer;

/**
 * KYC ADK Dev UI Server
 *
 * Starts the Google ADK Dev UI at http://localhost:8888/dev-ui
 * Use this for interactive agent testing during development.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass=com.dnb.kyc.KycDevServer
 */
public class KycDevServer {

    public static void main(String[] args) {
        // Wire up agents manually (no Spring context needed here)
        AuditLoggerAgent auditLogger = new AuditLoggerAgent();
        GuardrailsAgent guardrailsAgent = new GuardrailsAgent(auditLogger);
        EnrichmentAgent enrichmentAgent = new EnrichmentAgent(auditLogger);
        EnrichmentGuardrailsAgent enrichmentGuardrailsAgent = new EnrichmentGuardrailsAgent(auditLogger);
        AnalysisAgent analysisAgent = new AnalysisAgent(auditLogger);

        KycOrchestrator orchestrator = new KycOrchestrator(
                guardrailsAgent,
                enrichmentAgent,
                enrichmentGuardrailsAgent,
                analysisAgent,
                auditLogger
        );

        // Override port so dev UI doesn't clash with the KYC REST API on 8080
        System.setProperty("server.port", "8888");

        // Start the ADK Dev UI with the coordinator agent
        // Dev UI will be at: http://localhost:8888/dev-ui
        AdkWebServer.start(
                orchestrator.getCoordinatorAgent(),
                guardrailsAgent.getLlmAgent(),
                enrichmentAgent.getLlmAgent(),
                analysisAgent.getLlmAgent(),
                auditLogger.getLlmAgent()
        );
    }
}
