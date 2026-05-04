// Analysis Agent - Risk analysis and scoring

const RISK_THRESHOLDS = {
    low: 0.3,
    medium: 0.6,
    high: 0.8
};

export class AnalysisAgent {
    /**
     * Analyze enriched data and generate risk assessment
     * @param {object} data 
     * @returns {object}
     */
    analyze(data) {
        const riskScore = this._calculateRiskScore(data);
        const riskLevel = this._determineRiskLevel(riskScore);

        return {
            status: 'completed',
            risk_score: riskScore,
            risk_level: riskLevel,
            recommendation: this._getRecommendation(riskLevel),
            factors: this._getRiskFactors(data)
        };
    }

    /**
     * Calculate overall risk score
     * @param {object} data 
     * @returns {number}
     */
    _calculateRiskScore(data) {
        let score = 0.0;
        const weights = {
            nationality_risk: 0.4,
            document_verified: 0.3,
            age_factor: 0.3
        };

        // Nationality risk
        score += (data.nationality_risk || 0.5) * weights.nationality_risk;

        // Document verification
        if (!data.document_verified) {
            score += 0.5 * weights.document_verified;
        }

        // Age factor (placeholder)
        score += 0.2 * weights.age_factor;

        return Math.round(Math.min(score, 1.0) * 100) / 100;
    }

    /**
     * Determine risk level from score
     * @param {number} score 
     * @returns {string}
     */
    _determineRiskLevel(score) {
        if (score <= RISK_THRESHOLDS.low) return 'low';
        if (score <= RISK_THRESHOLDS.medium) return 'medium';
        if (score <= RISK_THRESHOLDS.high) return 'high';
        return 'critical';
    }

    /**
     * Get recommendation based on risk level
     * @param {string} riskLevel 
     * @returns {string}
     */
    _getRecommendation(riskLevel) {
        const recommendations = {
            low: 'approve',
            medium: 'review',
            high: 'enhanced_due_diligence',
            critical: 'reject'
        };
        return recommendations[riskLevel] || 'review';
    }

    /**
     * Get list of risk factors
     * @param {object} data 
     * @returns {Array}
     */
    _getRiskFactors(data) {
        const factors = [];
        if ((data.nationality_risk || 0) > 0.5) {
            factors.push('high_risk_nationality');
        }
        if (!data.document_verified) {
            factors.push('unverified_document');
        }
        return factors;
    }
}

export default AnalysisAgent;
