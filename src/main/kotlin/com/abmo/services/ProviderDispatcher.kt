package com.abmo.services

import com.abmo.providers.*
import com.abmo.executor.JavaScriptExecutor
import com.abmo.util.getHost
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProviderDispatcher: KoinComponent {

    private val javaScriptExecutor: JavaScriptExecutor by inject()

    // still this isn't an efficient and clean way to map domains to a provider
    // it will become a mess once more hosts are added
    /**
     * Retrieves the appropriate provider for the given URL.
     *
     * This method examines the host part of the URL and returns an instance of the corresponding
     * provider based on the defined mappings. If the URL's host does not match any known providers,
     * it returns a default provider (AbyssToProvider).
     *
     * @param url The URL for which to find the corresponding provider.
     * @return An instance of the Provider that matches the URL's host.
     */
    fun getProviderForUrl(url: String): Provider {
        url.getHost().apply {
           return when {
                contains("tvphim") -> TvphimProvider(javaScriptExecutor)
                equals("sieutamphim.com") -> SieutamphimProvider()
                equals("phimbet.biz") -> PhimbetProvider()
                contains("motchill") || equals("subnhanh.win") -> MotchillProvider()
                equals("animet3.biz") -> Animet3Provider()
                equals("tvhayw.org") -> TvhaywProvider()

                else -> AbyssToProvider()
            }
        }
    }

}