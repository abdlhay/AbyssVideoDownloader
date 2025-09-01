package com.abmo.util

import com.abmo.common.Logger

/**
 * Parses a string into video IDs/URLs with their respective resolutions.
 *
 * This function processes a string containing video IDs or URLs, optionally followed
 * by a resolution specifier ("h", "m", "l"). Multiple entries can be separated by commas.
 * If no resolution is specified, it defaults to "h" (high).
 *
 * Example Input:
 * - "id1 h,id2 l,http://example.com m"
 *
 * Example Output:
 * - [("id1", "h"), ("id2", "l"), ("http://example.com", "m")]
 *
 * @receiver The input string containing video IDs/URLs and optional resolutions.
 * @return A list of pairs, where each pair contains a video ID/URL and its resolution.
 *         Defaults to "h" for missing or invalid resolutions.
 */
fun String.parseVideoIdOrUrlWithResolution(): List<Pair<String, String>> {
    val results = mutableListOf<Pair<String, String>>()
    val segments = this.split(",")

    for (segment in segments) {
        val parts = segment.trim().split(" ")
        val videoIdOrUrl = parts.firstOrNull()?.trim()
        val resolution = parts.getOrNull(1)?.lowercase()?.takeIf { it in listOf("h", "m", "l") } ?: "h"

        if (!videoIdOrUrl.isNullOrEmpty()) {
            results.add(videoIdOrUrl to resolution)
        } else {
            Logger.error("Invalid format. Ensure video ID or URL is specified.")
        }
    }

    return results
}


/**
 * Finds and returns the value associated with the given key in a JSON string.
 *
 * @param key The key to search for.
 * @return The corresponding value as a String, or null if not found.
 */
fun String.findValueByKey(key: String): String? {
    val regex = """"$key"\s*:\s*("[^"\\]*(?:\\.[^"\\]*)*"|[^\s,}]+)""".toRegex()
    val matchResult = regex.find(this)
    return matchResult?.groupValues?.get(1)?.let {
        if (it.startsWith("\"") && it.endsWith("\"")) {
            it.substring(1, it.length - 1)
        } else {
            it
        }
    }
}

fun String.replaceLast(oldValue: String, newValue: String): String {
    val lastIndex = this.lastIndexOf(oldValue)
    return if (lastIndex == -1) {
        this
    } else {
        this.substring(0, lastIndex) + newValue + this.substring(lastIndex + oldValue.length)
    }
}

fun String.between(start: String, end: String): String {
    val startIndex = this.indexOf(start)
    val endIndex = this.lastIndexOf(end)
    return this.substring(startIndex, endIndex)
}