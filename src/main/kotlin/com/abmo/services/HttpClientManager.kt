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


    private fun makeHttpRequest(url: String, headers: Map<String, String?>?): HttpResponse? {
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

            HttpResponse(
                body = response.body,
                statusCode = response.status
            )
        } catch (e: Exception) {
            Logger.error("Error making HTTP request with Unirest: ${e.message}")
            null
        }
    }
}