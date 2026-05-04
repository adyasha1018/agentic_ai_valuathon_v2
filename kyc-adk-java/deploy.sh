#!/bin/bash
# ================================================================
# KYC Multi-Agent System - Google Cloud Run Deployment Script
# ================================================================
# 
# This script deploys the KYC Multi-Agent System to Google Cloud Run.
# 
# Prerequisites:
# 1. Google Cloud SDK installed and configured
# 2. Docker installed (for local builds)
# 3. Project ID and region configured
# 4. Required APIs enabled (Cloud Run, Artifact Registry, Vertex AI)
#
# Usage:
#   ./deploy.sh                    # Deploy with defaults
#   ./deploy.sh --project my-proj  # Specify project
#   ./deploy.sh --region us-east1  # Specify region
#   ./deploy.sh --with-ui          # Deploy with ADK Dev UI
# ================================================================

set -e

# ======================== CONFIGURATION ========================
# These can be overridden via environment variables or CLI flags

PROJECT_ID="${GOOGLE_CLOUD_PROJECT:-}"
REGION="${GOOGLE_CLOUD_LOCATION:-us-central1}"
SERVICE_NAME="${SERVICE_NAME:-kyc-multiagent-system}"
ARTIFACT_REGISTRY="${ARTIFACT_REGISTRY:-gcr.io}"
WITH_UI=false
ALLOW_UNAUTHENTICATED=false

# ======================== PARSE ARGUMENTS ========================
while [[ $# -gt 0 ]]; do
    case $1 in
        --project)
            PROJECT_ID="$2"
            shift 2
            ;;
        --region)
            REGION="$2"
            shift 2
            ;;
        --service-name)
            SERVICE_NAME="$2"
            shift 2
            ;;
        --with-ui)
            WITH_UI=true
            shift
            ;;
        --allow-unauthenticated)
            ALLOW_UNAUTHENTICATED=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --project PROJECT_ID        Google Cloud Project ID"
            echo "  --region REGION             Deployment region (default: us-central1)"
            echo "  --service-name NAME         Cloud Run service name"
            echo "  --with-ui                   Deploy with ADK Dev UI"
            echo "  --allow-unauthenticated     Allow public access"
            echo "  --help                      Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# ======================== VALIDATION ========================
if [ -z "$PROJECT_ID" ]; then
    echo "ERROR: Project ID is required."
    echo "Set GOOGLE_CLOUD_PROJECT environment variable or use --project flag."
    exit 1
fi

echo "================================================================"
echo "KYC Multi-Agent System - Cloud Run Deployment"
echo "================================================================"
echo "Project:      $PROJECT_ID"
echo "Region:       $REGION"
echo "Service:      $SERVICE_NAME"
echo "With UI:      $WITH_UI"
echo "================================================================"

# ======================== SET PROJECT ========================
echo ""
echo ">>> Setting Google Cloud project..."
gcloud config set project "$PROJECT_ID"

# ======================== ENABLE APIS ========================
echo ""
echo ">>> Enabling required APIs..."
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    aiplatform.googleapis.com \
    --quiet

# ======================== BUILD & PUSH IMAGE ========================
IMAGE_URL="${ARTIFACT_REGISTRY}/${PROJECT_ID}/${SERVICE_NAME}:latest"

echo ""
echo ">>> Building and pushing container image..."
echo "    Image: $IMAGE_URL"

# Option 1: Build with Cloud Build (recommended for production)
gcloud builds submit \
    --tag "$IMAGE_URL" \
    --timeout=20m \
    .

# Option 2: Local build (uncomment if preferred)
# docker build -t "$IMAGE_URL" .
# docker push "$IMAGE_URL"

# ======================== DEPLOY TO CLOUD RUN ========================
echo ""
echo ">>> Deploying to Cloud Run..."

DEPLOY_CMD="gcloud run deploy $SERVICE_NAME \
    --image=$IMAGE_URL \
    --platform=managed \
    --region=$REGION \
    --memory=1Gi \
    --cpu=1 \
    --timeout=300 \
    --concurrency=100 \
    --min-instances=0 \
    --max-instances=10 \
    --set-env-vars=GOOGLE_CLOUD_PROJECT=$PROJECT_ID \
    --set-env-vars=GOOGLE_CLOUD_LOCATION=$REGION \
    --set-env-vars=GOOGLE_GENAI_USE_VERTEXAI=true"

if [ "$ALLOW_UNAUTHENTICATED" = true ]; then
    DEPLOY_CMD="$DEPLOY_CMD --allow-unauthenticated"
else
    DEPLOY_CMD="$DEPLOY_CMD --no-allow-unauthenticated"
fi

eval "$DEPLOY_CMD"

# ======================== GET SERVICE URL ========================
echo ""
echo ">>> Getting service URL..."
SERVICE_URL=$(gcloud run services describe "$SERVICE_NAME" \
    --platform=managed \
    --region="$REGION" \
    --format='value(status.url)')

echo ""
echo "================================================================"
echo "DEPLOYMENT SUCCESSFUL!"
echo "================================================================"
echo ""
echo "Service URL: $SERVICE_URL"
echo ""
echo "API Endpoints:"
echo "  Health:    $SERVICE_URL/api/health"
echo "  Process:   POST $SERVICE_URL/api/kyc/process"
echo "  Audit:     GET $SERVICE_URL/api/audit-trail"
echo "  Stats:     GET $SERVICE_URL/api/compliance/stats"
echo ""
echo "Test with:"
echo "  curl $SERVICE_URL/api/health"
echo ""
if [ "$ALLOW_UNAUTHENTICATED" = false ]; then
    echo "NOTE: Service requires authentication."
    echo "Get identity token: gcloud auth print-identity-token"
    echo "Use: curl -H \"Authorization: Bearer \$(gcloud auth print-identity-token)\" $SERVICE_URL/api/health"
fi
echo "================================================================"
