/**
 * Pipeline orchestration & backend detection
 */

import { render } from './renderer.js';

// Backend configuration
const BACKEND_URL = 'http://localhost:3000';

/**
 * Check if backend is available
 * @returns {Promise<boolean>}
 */
async function isBackendAvailable() {
    try {
        const response = await fetch(`${BACKEND_URL}/health`, {
            method: 'GET',
            timeout: 5000
        });
        return response.ok;
    } catch (error) {
        console.warn('Backend unavailable, using local agents fallback');
        return false;
    }
}

/**
 * Load appropriate agent based on backend availability
 * @param {string} agentName 
 * @returns {Promise<object>}
 */
async function loadAgent(agentName) {
    const backendAvailable = await isBackendAvailable();
    
    if (backendAvailable) {
        // Use backend API
        return {
            execute: async (input) => {
                const response = await fetch(`${BACKEND_URL}/agents/${agentName}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(input)
                });
                return response.json();
            }
        };
    } else {
        // Fallback to local JavaScript agent
        const module = await import(`./agents/${agentName}.js`);
        return module.default;
    }
}

/**
 * Run the pipeline
 * @param {object} input 
 */
async function runPipeline(input) {
    try {
        const results = [];
        // Add pipeline steps here
        render(results);
    } catch (error) {
        console.error('Pipeline error:', error);
        render({ error: error.message });
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('Pipeline application initialized');
});

export { runPipeline, loadAgent, isBackendAvailable };
