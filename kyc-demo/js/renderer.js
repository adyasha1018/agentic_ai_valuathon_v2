/**
 * Result visualization
 */

/**
 * Render results to the DOM
 * @param {object|array} data - Data to render
 * @param {string} containerId - Target container ID
 */
export function render(data, containerId = 'results') {
    const container = document.getElementById(containerId);
    
    if (!container) {
        console.error(`Container #${containerId} not found`);
        return;
    }

    // Clear previous content
    container.innerHTML = '';

    if (data.error) {
        renderError(container, data.error);
        return;
    }

    if (Array.isArray(data)) {
        renderList(container, data);
    } else if (typeof data === 'object') {
        renderObject(container, data);
    } else {
        container.textContent = String(data);
    }
}

/**
 * Render error message
 */
function renderError(container, message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error';
    errorDiv.textContent = `Error: ${message}`;
    container.appendChild(errorDiv);
}

/**
 * Render array as list
 */
function renderList(container, items) {
    const ul = document.createElement('ul');
    ul.className = 'results-list';
    
    items.forEach(item => {
        const li = document.createElement('li');
        li.textContent = typeof item === 'object' ? JSON.stringify(item) : String(item);
        ul.appendChild(li);
    });
    
    container.appendChild(ul);
}

/**
 * Render object as key-value pairs
 */
function renderObject(container, obj) {
    const dl = document.createElement('dl');
    dl.className = 'results-object';
    
    Object.entries(obj).forEach(([key, value]) => {
        const dt = document.createElement('dt');
        dt.textContent = key;
        
        const dd = document.createElement('dd');
        dd.textContent = typeof value === 'object' ? JSON.stringify(value) : String(value);
        
        dl.appendChild(dt);
        dl.appendChild(dd);
    });
    
    container.appendChild(dl);
}

export default { render };
