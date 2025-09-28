package com.abmo.util

import com.abmo.common.Constants.DEFAULT_CONCURRENT_DOWNLOAD_LIMIT
import com.abmo.common.Logger
import java.io.File
import kotlin.system.exitProcess

/**
 * A class for parsing command-line arguments.
 *
 * @param args The array of command-line arguments.
 */
class CliArguments(private val args: Array<String>) {

    /**
     * Extracts headers from command-line arguments in the format "--header key:value".
     *
     * @return A map of header names to their values, or null if no headers are found.
     */
    fun getHeaders(): Map<String, String>? {
        val headers = mutableMapOf<String, String>()

        for (i in args.indices) {
            if (args[i] in arrayOf("--header", "-H") && i + 1 < args.size) {
                val (key, value) = args[i + 1].split(":", limit = 2).map { it.trim() }
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    headers[key] = value
                } else {
                    Logger.error("Invalid header format. Use 'Header-Name: Header-Value'")
                }
            }
        }

        return headers.ifEmpty { null }
    }

    /**
     * Retrieves the output file name from command-line arguments.
     *
     * @return The output file path as a String, or null if not specified.
     */
    fun getOutputFileName(): String? {
        val index = args.indexOf("-o")
        if (index != -1 && index + 1 < args.size) {
            val filePath = args[index + 1]
            return  filePath
        }
        return null
    }

    /**
     * Retrieves the number of parallel connections from command-line arguments.
     *
     * @return The number of connections, constrained between 1 and 10.
     *         Returns the default value if not specified.
     */
    fun getParallelConnections(): Int {
        val maxConnections = 10
        val minConnections = 1
        val connectionArgIndex = args.indexOfFirst { it == "--connections" || it == "-c"}
        if (connectionArgIndex != -1 && connectionArgIndex + 1 < args.size) {
            val connectionValue = args[connectionArgIndex + 1].toIntOrNull()
            if (connectionValue != null) {
                return connectionValue.coerceIn(minConnections, maxConnections)
            }
        }

        return DEFAULT_CONCURRENT_DOWNLOAD_LIMIT
    }

    /**
     * Checks if the verbose flag is enabled in the command-line arguments.
     *
     * @return true if "--verbose" is present, false otherwise.
     */
    fun isVerboseEnabled() = args.contains("--verbose")


    /**
     * Parses video IDs, URLs, or file input with associated resolutions.
     * @return A list of pairs, where each pair contains a video ID/URL and its resolution.
     *         If no resolution is provided, defaults to "h" (high).
     */
    fun getVideoIdsOrUrlsWithResolutions(): List<Pair<String, String>> {
        if (args.isEmpty()) {
            Logger.error("No arguments provided. A video ID, URL, or file path is required.")
            exitProcess(0)
        }

        val relevantArgs = args.filterNot { arg ->
            arg.startsWith("-o") || arg.startsWith("-H") || arg.startsWith("--header") ||
                    arg.startsWith("-c") || arg.startsWith("--connection") || arg.startsWith("--retry")
        }

        if (relevantArgs.isEmpty()) {
            Logger.error("No valid video IDs or URLs provided.")
            exitProcess(0)
        }


        val lastArg = relevantArgs.first()

        return when {
            File(lastArg).exists() -> {
                File(lastArg).readLines().flatMap { it.parseVideoIdOrUrlWithResolution() }
            }
            lastArg.contains(",") -> {
                lastArg.split(",").flatMap { it.trim().parseVideoIdOrUrlWithResolution() }
            }
            else -> {
                args.joinToString(" ").trim().parseVideoIdOrUrlWithResolution()
            }
        }
    }

    /**
     * Retrieves the number of download attempts from command-line arguments
     * 
     * @return Number of attempts to download a video.
     *         Returns null if the argument is not provided
     */
    fun getRetriesCount(): Int? {
        val retriesArgIndex = args.indexOf("--retry")
        if (retriesArgIndex == -1)
            return null

        // Validate arg
        if (retriesArgIndex + 1 > args.size) {
            Logger.error("No retries count provided.")
            exitProcess(0)
        }

        val retriesCount = args[retriesArgIndex + 1].toIntOrNull()
        if (retriesCount == null) {
            Logger.error("No valid retries count provided.")
            exitProcess(0)
        }

        return retriesCount
    }
}