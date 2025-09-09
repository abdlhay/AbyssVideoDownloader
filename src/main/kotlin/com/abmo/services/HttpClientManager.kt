package com.abmo.services

import com.abmo.common.Logger
import com.abmo.model.HttpResponse
import com.github.zhkl0228.impersonator.ImpersonatorFactory
import com.mashape.unirest.http.Unirest
import okhttp3.OkHttpClientFactory
import okhttp3.Request

class HttpClientManager {

    fun makeHttpRequest(url: String, headers: Map<String, String?>? = null, curlPath: String): HttpResponse? {
        Logger.debug("Initiating http request to $url")

        return if (isWindowsOS()) {
            makeHttpRequest(url, headers)
        } else {
            val api = ImpersonatorFactory.ios()
            val context = api.newSSLContext(null, null)
            val factory = OkHttpClientFactory.create(api)
            val client = factory.newHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            HttpResponse(body = response.body?.string(), statusCode = response.code)
        }
    }

    private fun isWindowsOS(): Boolean {
        val osName = System.getProperty("os.name").lowercase()
        return osName.contains("windows")
    }

    private fun makeRequestWithCurl(url: String, headers: Map<String, String?>?, curlPath: String): Response? {
        Logger.debug("Running on Linux distro, using curl-impersonate-chrome")

        if (!isCurlImpersonateAvailable()) {
            showInstallationInstructions()
            return null
        }

        return try {
            val command = buildCurlCommand(url, headers, curlPath)
            executeCurlCommand(command)
        } catch (e: Exception) {
            Logger.error("Error executing curl-impersonate-chrome: ${e.message}")
            null
        }
    }

    private fun makeHttpRequest(url: String, headers: Map<String, String?>?): Response? {
        Logger.debug("Running on Windows, using Unirest")

        return try {
            val response = Unirest.get(url)
                .headers(headers)
                .asString()

            Logger.debug("Received response with status ${response.status}", response.status !in 200..299)

            if (response.status !in 200..299) {
                Logger.error("HTTP request failed with status ${response.status}")
                return null
            }

            Response(
                body = response.body,
                statusCode = response.status
            )
        } catch (e: Exception) {
            Logger.error("Error making HTTP request with Unirest: ${e.message}")
            null
        }
    }

    private fun isCurlImpersonateAvailable(): Boolean {
        return try {
            val processBuilder = ProcessBuilder("curl-impersonate-chrome", "--version")
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun showInstallationInstructions() {
        Logger.error("curl-impersonate-chrome is not installed or not found in PATH")
        println("ERROR: curl-impersonate-chrome is required for Linux-based environments.")
        println("Please install it by following the instructions at:")
        println(CURL_IMPERSONATE_INSTALL_URL)
    }

    private fun buildCurlCommand(url: String, headers: Map<String, String?>?, curlPath: String): List<String> {
        val command = mutableListOf(
            curlPath,
            "-s",
            "-A", USER_AGENT,
            "-w", "%{http_code}",
            url
        )

        headers?.forEach { (key, value) ->
            if (value != null) {
                command.addAll(listOf("-H", "$key: $value"))
            }
        }

        return command
    }

    private fun executeCurlCommand(command: List<String>): Response? {
        val processBuilder = ProcessBuilder(command)
        val process = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val fullResponse = reader.readText()
        reader.close()

        val exitCode = process.waitFor()
        Logger.debug("curl-impersonate-chrome completed with exit code $exitCode", exitCode != 0)

        if (exitCode != 0) {
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val errorOutput = errorReader.readText()
            errorReader.close()
            Logger.error("curl-impersonate-chrome failed: $errorOutput")
            return null
        }

        val statusCode = try {
            val lastThreeChars = fullResponse.takeLast(3)
            lastThreeChars.toInt()
        } catch (e: Exception) {
            Logger.error("Failed to parse HTTP status code from curl response")
            return null
        }


        val responseBody = fullResponse.dropLast(3)

        Logger.debug("Received response with status $statusCode", statusCode !in 200..299)

        if (statusCode !in 200..299) {
            Logger.error("HTTP request failed with status $statusCode")
            return null
        }

        return Response(
            body = responseBody,
            statusCode = statusCode
        )
    }
}

data class Response(
    val body: String,
    val statusCode: Int
)