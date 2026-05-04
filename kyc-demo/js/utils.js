// Helpers

/**
 * Generate a unique request ID
 * @returns {string} UUID
 */
export function generateRequestId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

/**
 * Format date to ISO string
 * @param {Date} date 
 * @returns {string}
 */
export function formatDate(date) {
    return date.toISOString();
}

/**
 * Validate date format (YYYY-MM-DD)
 * @param {string} dateStr 
 * @returns {boolean}
 */
export function isValidDate(dateStr) {
    const regex = /^\d{4}-\d{2}-\d{2}$/;
    if (!regex.test(dateStr)) return false;
    const date = new Date(dateStr);
    return date instanceof Date && !isNaN(date);
}

/**
 * Deep clone an object
 * @param {object} obj 
 * @returns {object}
 */
export function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
}

/**
 * Delay execution
 * @param {number} ms 
 * @returns {Promise}
 */
export function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Format risk score as percentage
 * @param {number} score 
 * @returns {string}
 */
export function formatRiskScore(score) {
    return `${(score * 100).toFixed(1)}%`;
}
