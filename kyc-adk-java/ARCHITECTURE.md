# Architecture

## Project Structure

```
├── README.md                 # Full documentation
├── ARCHITECTURE.md          # This file
├── requirements.txt         # Python dependencies
├── config.py               # Compliance rules
├── api_server.py           # Flask API (REST interface)
├── orchestrator.py         # Agent coordinator
├── agents/                 # 4 Specialized agents
│   ├── __init__.py
│   ├── guardrails_agent.py
│   ├── enrichment_agent.py
│   ├── analysis_agent.py
│   └── audit_logger.py     # DORA compliance
```

## Components

### API Server (`api_server.py`)
Flask-based REST API that exposes KYC endpoints for external integration.

### Orchestrator (`orchestrator.py`)
Coordinates the workflow between specialized agents, managing the KYC pipeline.

### Agents

#### Guardrails Agent
Validates input data and enforces safety constraints.

#### Enrichment Agent
Augments customer data with additional information from external sources.

#### Analysis Agent
Performs risk analysis and generates compliance scores.

#### Audit Logger
Ensures DORA (Digital Operational Resilience Act) compliance through comprehensive logging.
