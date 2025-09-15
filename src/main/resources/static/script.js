let progressSocket = null;

/**
 * Toggle the visibility of the advanced options section
 */
function toggleAdvanced() {
    const section = document.getElementById("advancedSection");
    section.classList.toggle("hidden");
}

/**
 * Format bytes into human-readable format
 * @param {number} bytes - Number of bytes
 * @returns {string} Formatted size string
 */
function formatSize(bytes) {
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    if (!bytes || bytes === 0) return '0 B';
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + sizes[i];
}

/**
 * Initialize WebSocket connection for progress updates
 */
function initWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/progress`;

    progressSocket = new WebSocket(wsUrl);

    progressSocket.onopen = () => {
        console.log('WebSocket connected');
        document.getElementById('connectionStatus').textContent = 'Connected';
        document.getElementById('connectionStatus').className = 'text-green-500';
    };

    progressSocket.onmessage = (event) => {
        try {
            const progress = JSON.parse(event.data);
            updateProgressUI(progress);
        } catch (error) {
            console.error('Error parsing progress data:', error);
        }
    };

    progressSocket.onclose = () => {
        console.log('WebSocket disconnected');
        document.getElementById('connectionStatus').textContent = 'Disconnected';
        document.getElementById('connectionStatus').className = 'text-red-500';

        // Try to reconnect after 3 seconds
        setTimeout(initWebSocket, 3000);
    };

    progressSocket.onerror = (error) => {
        console.error('WebSocket error:', error);
        document.getElementById('connectionStatus').textContent = 'Error';
        document.getElementById('connectionStatus').className = 'text-red-500';
    };
}

/**
 * Format time in seconds to MM:SS format
 * @param {number} seconds - Time in seconds
 * @returns {string} Formatted time string
 */
function formatTime(seconds) {
    if (!seconds || seconds === 0) return '00:00';

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);

    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
}

/**
 * Format speed in bytes per second to human-readable format
 * @param {number} bytesPerSecond - Speed in bytes per second
 * @returns {string} Formatted speed string
 */
function formatSpeed(bytesPerSecond) {
    return formatSize(bytesPerSecond) + '/s';
}

/**
 * Update the progress UI with new progress data
 * @param {Object} progress - Progress data object
 */
function updateProgressUI(progress) {
    const {
        downloadedSegments = 0,
        totalSegments = 0,
        downloadedBytes = 0,
        mediaSize = 0,
        percent = 0,
        status = 'downloading',
        message = '',
        error = null,
        startTime = null
    } = progress;

    // Calculate elapsed time and download speed
    let elapsedTimeSeconds = 0;
    let downloadSpeedBps = 0;

    if (startTime && downloadedBytes > 0) {
        const currentTime = Date.now();
        elapsedTimeSeconds = (currentTime - startTime) / 1000;
        downloadSpeedBps = elapsedTimeSeconds > 0 ? (downloadedBytes / elapsedTimeSeconds) : 0;
    }

    // Update progress bar
    document.getElementById('progressBar').style.width = `${percent}%`;
    document.getElementById('progressPercent').textContent = `${percent.toFixed(1)}%`;

    // Update segments progress
    document.getElementById('segmentsProgress').textContent = `${downloadedSegments} / ${totalSegments}`;

    // Update bytes progress
    document.getElementById('bytesProgress').textContent = `${formatSize(downloadedBytes)} / ${formatSize(mediaSize)}`;

    // Update speed and elapsed time
    document.getElementById('downloadSpeed').textContent = formatSpeed(downloadSpeedBps);
    document.getElementById('timeElapsed').textContent = formatTime(elapsedTimeSeconds);

    // Update status based on backend status
    if (error) {
        document.getElementById('progressStatus').textContent = `Error: ${error}`;
        document.getElementById('progressStatus').className = 'text-red-500';
        resetDownloadButtons();
    } else if (status === 'completed') {
        document.getElementById('progressStatus').textContent = message || 'Download completed!';
        document.getElementById('progressStatus').className = 'text-green-500';
        document.getElementById('progressBar').className = 'bg-green-500 h-3 rounded-full progress-bar';
        resetDownloadButtons();
    } else if (status === 'error') {
        document.getElementById('progressStatus').textContent = message || 'Download failed!';
        document.getElementById('progressStatus').className = 'text-red-500';
        resetDownloadButtons();
    } else if (percent === 0) {
        document.getElementById('progressStatus').textContent = 'Starting download...';
        document.getElementById('progressStatus').className = 'text-gray-600';
    } else {
        document.getElementById('progressStatus').textContent = message || 'Downloading...';
        document.getElementById('progressStatus').className = 'text-blue-500';
    }

    // Show progress container if it's hidden
    if (percent > 0 || downloadedSegments > 0) {
        document.getElementById('progressContainer').classList.remove('hidden');
    }
}

/**
 * Download a specific video source
 * @param {string} url - Video URL
 * @param {string} resId - Resolution ID
 * @param {Object} data - Video metadata
 * @param {HTMLElement} buttonElement - Download button element
 * @param {string} outputFileName - Custom output filename
 */
async function downloadSource(url, resId, data, buttonElement, outputFileName) {
    const payload = {
        config: {
            url: url,
            resolution: resId,
            outputFile: outputFileName, // let backend decide if not set
            connections: parseInt(document.getElementById("connections").value) || 4
        },
        videoMetadata: data
    };

    try {
        // Disable the button and show loading state
        buttonElement.disabled = true;
        buttonElement.textContent = "Starting...";
        buttonElement.className = "bg-gray-500 text-white px-4 py-1 rounded cursor-not-allowed";

        // Reset progress UI
        updateProgressUI({
            downloadedSegments: 0,
            totalSegments: 0,
            downloadedBytes: 0,
            mediaSize: 0,
            percent: 0,
            status: 'starting'
        });

        const response = await fetch("/download", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        console.log("Response status:", response.status);
        console.log("Response headers:", response.headers);

        if (!response.ok) {
            const errorText = await response.text();
            console.log("Error response body:", errorText);
            throw new Error(`Failed to start download (${response.status}): ${errorText}`);
        }

        const result = await response.json();
        // currentDownloadId = result.downloadId || Date.now();

        // Update button to show download in progress
        buttonElement.textContent = "Downloading...";
        buttonElement.className = "bg-blue-500 text-white px-4 py-1 rounded cursor-not-allowed";

        console.log("Download started successfully:", result);

        // give a moment for WebSocket to receive initial progress
        setTimeout(() => {
            if (document.getElementById('progressContainer').classList.contains('hidden')) {
                // If no progress received yet show the progress container anyway
                document.getElementById('progressContainer').classList.remove('hidden');
            }
        }, 1000);

    } catch (err) {
        console.error("Download error:", err);

        // show error in progress UI
        updateProgressUI({
            downloadedSegments: 0,
            totalSegments: 0,
            downloadedBytes: 0,
            mediaSize: 0,
            percent: 0,
            status: 'error',
            error: err.message
        });

        // Reset button state
        buttonElement.disabled = false;
        buttonElement.textContent = "Download";
        buttonElement.className = "bg-green-500 hover:bg-green-600 text-white px-4 py-1 rounded";
    }
}

/**
 * Reset the progress UI to initial state
 */
function resetProgressUI() {
    // Reset all progress values to initial state
    document.getElementById('progressBar').style.width = '0%';
    document.getElementById('progressBar').className = 'bg-blue-500 h-3 rounded-full progress-bar';
    document.getElementById('progressPercent').textContent = '0%';
    document.getElementById('segmentsProgress').textContent = '0 / 0';
    document.getElementById('bytesProgress').textContent = '0 B / 0 B';
    document.getElementById('downloadSpeed').textContent = '0 B/s';
    document.getElementById('timeElapsed').textContent = '00:00';
    document.getElementById('progressStatus').textContent = 'Ready to download';
    document.getElementById('progressStatus').className = 'text-gray-600';

    // Hide the progress container
    document.getElementById('progressContainer').classList.add('hidden');

    console.log('Progress UI reset for new fetch');
}

/**
 * Reset all download buttons to their default state
 */
function resetDownloadButtons() {
    const downloadButtons = document.querySelectorAll('#sourcesTable button');
    downloadButtons.forEach(btn => {
        btn.disabled = false;
        btn.textContent = "Download";
        btn.className = "bg-green-500 hover:bg-green-600 text-white px-4 py-1 rounded";
    });
}

// Event listeners and initialization
window.onload = () => {
    // Load saved preferences from localStorage
    // document.getElementById("outputFileName").value = localStorage.getItem("outputFileName") || "";
    document.getElementById("connections").value = localStorage.getItem("connections") || "4";
    document.getElementById("headers").value = localStorage.getItem("headers") || "";

    // Initialize WebSocket connection
    initWebSocket();
};

// Form submission handler
document.getElementById("urlForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const url = document.getElementById("videoUrl").value;
    // const outputFileName = document.getElementById("outputFileName").value;
    const connections = document.getElementById("connections").value;
    const headers = document.getElementById("headers").value;

    // Save preferences
    localStorage.setItem("connections", connections);

    // Reset UI state for new fetch
    resetProgressUI();
    document.getElementById("sourcesContainer").classList.add("hidden");

    // Show spinner
    document.getElementById("fetchText").textContent = "Fetching...";
    document.getElementById("fetchSpinner").classList.remove("hidden");

    const formData = new URLSearchParams();
    formData.append("url", url);
    if (connections) formData.append("connections", connections);
    if (headers) formData.append("headers", headers);

    try {
        const res = await fetch("/fetch", { method: "POST", body: formData });

        // Hide spinner
        document.getElementById("fetchText").textContent = "Fetch";
        document.getElementById("fetchSpinner").classList.add("hidden");

        if (!res.ok) {
            alert("Failed to fetch metadata");
            return;
        }

        const data = await res.json();
        const sources = data.sources || [];
        const table = document.getElementById("sourcesTable");
        table.innerHTML = "";

        sources.forEach(src => {
            const row = document.createElement("tr");
            row.innerHTML = `
              <td class="border px-4 py-2">${src.label}</td>
              <td class="border px-4 py-2">${src.codec}</td>
              <td class="border px-4 py-2">${formatSize(src.size)}</td>
              <td class="border px-4 py-2">${src.type}</td>
              <td class="border px-4 py-2 text-center">
                <button
                  class="bg-green-500 hover:bg-green-600 text-white px-4 py-1 rounded download-btn">
                  Download
                </button>
              </td>
            `;

            // Get the button element
            const button = row.querySelector("button");

            // Attach click listener with closure capturing src & full data
            button.addEventListener("click", () => {
                const outputFileName = document.getElementById("outputFileName").value;
                downloadSource(url, src.label, data, button, outputFileName);
            });

            table.appendChild(row);
        });

        document.getElementById("sourcesContainer").classList.remove("hidden");
    } catch (error) {
        console.error("Error fetching metadata:", error);
        alert("Error fetching metadata");

        // Hide spinner
        document.getElementById("fetchText").textContent = "Fetch";
        document.getElementById("fetchSpinner").classList.add("hidden");
    }
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (progressSocket) {
        progressSocket.close();
    }
});