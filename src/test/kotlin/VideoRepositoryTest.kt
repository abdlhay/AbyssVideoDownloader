import com.abmo.di.koinModule
import com.abmo.services.VideoDownloader
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class VideoDownloaderIntegrationTest : KoinComponent {

    private val videoDownloader: VideoDownloader by inject()

    @ParameterizedTest
    @MethodSource("videoUrlsAndSlugs")
    fun `test video metadata extraction from real URLs returns correct slug`(url: String, expectedSlug: String) {
//        assumeTrue(System.getenv("CI") != "true", "Skipping test in CI environment")

        val headers = mapOf("Referer" to "https://abyss.to/")

        try {
            println("testing URL: $url")
            val result = videoDownloader.getVideoMetaData(url, headers, "curl-impersonate-chrome")
            println("result: $result")

            if (result?.sources.isNullOrEmpty()) {
                fail("empty source list returned")
            }

            assertNotNull(result, "video metadata should not be null for URL: $url")
            assertEquals(expectedSlug, result.slug, "Expected slug '$expectedSlug' for URL: $url")
        } catch (e: Exception) {
            println("Error testing URL $url: ${e.message}")
            throw e
        }
    }

    companion object {
        @JvmStatic
        fun videoUrlsAndSlugs(): Stream<Arguments> {
            val videoIDList = listOf(
                "hY_y1CqB0", "IHkd0Mws_", "JZMRhKMkP", "2xvPq9YUT", "CibObsG69",
                "cAlc2yA_P", "2xvPq9YUT", "Kj1HAeAde", "ZHO0R7ZkR", "GZr_NbnAwvD", "hpXFDLHDj",
                "vG3vP922G", "jW0HhYs6y"
            )
            return Stream.of(*videoIDList.map {
                    videoId -> Arguments.of("https://abysscdn.com/?v=$videoId", videoId)
            }.toTypedArray())
        }

        @JvmStatic
        @BeforeAll
        fun setUp() {
            startKoin {
                modules(koinModule)
            }
        }
    }
}