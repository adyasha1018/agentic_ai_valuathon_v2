// Guardrails Agent - Input validation and safety checks

const REQUIRED_FIELDS = [
    'full_name',
    'date_of_birth',
    'nationality',
    'document_type',
    'document_number'
];

const DOCUMENT_TYPES = ['passport', 'national_id', 'drivers_license'];

export class GuardrailsAgent {
    /**
     * Validate KYC input data
     * @param {object} data 
     * @returns {object}
     */
    validate(data) {
        const errors = [];

        // Check required fields
        for (const field of REQUIRED_FIELDS) {
            if (!data[field]) {
                errors.push(`Missing required field: ${field}`);
            }
        }

        // Validate document type
        if (data.document_type && !DOCUMENT_TYPES.includes(data.document_type)) {
            errors.push(`Invalid document type: ${data.document_type}`);
        }

        // Validate date format
        if (data.date_of_birth && !this._isValidDate(data.date_of_birth)) {
            errors.push('Invalid date of birth format');
        }

        if (errors.length > 0) {
            return { valid: false, errors };
        }

        return { valid: true, data };
    }

    /**
     * Check if date string is valid
     * @param {string} dateStr 
     * @returns {boolean}
     */
    _isValidDate(dateStr) {
        const regex = /^\d{4}-\d{2}-\d{2}$/;
        if (!regex.test(dateStr)) return false;
        const date = new Date(dateStr);
        return date instanceof Date && !isNaN(date);
    }
}

export default GuardrailsAgent;
