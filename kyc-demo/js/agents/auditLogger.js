// Audit Logger - DORA compliance logging

const DORA_CONFIG = {
    log_retention_days: 365,
    audit_level: 'detailed',
    encryption_required: true
};

export class AuditLogger {
    constructor() {
        this.logs = [];
    }

    /**
     * Log an audit event
     * @param {string} eventType 
     * @param {string} requestId 
     * @param {object} data 
     */
    log(eventType, requestId, data = null) {
        const entry = {
            timestamp: new Date().toISOString(),
            event_type: eventType,
            request_id: requestId,
            data: DORA_CONFIG.audit_level === 'detailed' ? data : null
        };
        this.logs.push(entry);
        this._persistLog(entry);
    }

    /**
     * Persist log entry to storage
     * @param {object} entry 
     */
    _persistLog(entry) {
        // In production, would write to secure, immutable storage
        console.log('[AUDIT]', JSON.stringify(entry));
    }

    /**
     * Retrieve audit logs, optionally filtered by request ID
     * @param {string} requestId 
     * @returns {Array}
     */
    getLogs(requestId = null) {
        if (requestId) {
            return this.logs.filter(log => log.request_id === requestId);
        }
        return this.logs;
    }

    /**
     * Export DORA compliance report for date range
     * @param {string} startDate 
     * @param {string} endDate 
     * @returns {object}
     */
    exportComplianceReport(startDate, endDate) {
        return {
            report_type: 'DORA_compliance',
            start_date: startDate,
            end_date: endDate,
            total_events: this.logs.length,
            retention_policy_days: DORA_CONFIG.log_retention_days,
            encryption_enabled: DORA_CONFIG.encryption_required
        };
    }
}

export default AuditLogger;
