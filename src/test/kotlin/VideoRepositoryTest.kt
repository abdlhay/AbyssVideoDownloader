import com.abmo.di.koinModule
import com.abmo.services.VideoDownloader
import com.abmo.util.toJson
import org.junit.jupiter.api.Assumptions.assumeTrue
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
        assumeTrue(System.getenv("CI") != "true", "Skipping test in CI environment")

        val headers = mapOf("Referer" to "https://abyss.to/")

        try {
            println("testing URL: $url")
            val result = videoDownloader.getVideoMetaData(url, headers, "curl-impersonate-chrome")
            println("result: ${result?.toJson()}")

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
        fun videoUrlsAndSlugs(): Stream<Arguments> = Stream.of(
            Arguments.of("https://abysscdn.com/?v=hY_y1CqB0", "hY_y1CqB0"),
            Arguments.of("https://abysscdn.com/?v=IHkd0Mws_", "IHkd0Mws_"),
            Arguments.of("https://abysscdn.com/?v=JZMRhKMkP", "JZMRhKMkP"),
            Arguments.of("https://abysscdn.com/?v=2xvPq9YUT", "2xvPq9YUT"),
            Arguments.of("https://abysscdn.com/?v=CibObsG69", "CibObsG69"),
            Arguments.of("https://abysscdn.com/?v=cAlc2yA_P", "cAlc2yA_P"),
            Arguments.of("https://abysscdn.com/?v=2xvPq9YUT", "2xvPq9YUT"),
            Arguments.of("https://abysscdn.com/?v=Kj1HAeAde", "Kj1HAeAde"),
            Arguments.of("https://abysscdn.com/?v=ZHO0R7ZkR", "ZHO0R7ZkR"),
            Arguments.of("https://abysscdn.com/?v=GZr_NbnAwvD", "GZr_NbnAwvD"),
            Arguments.of("https://abysscdn.com/?v=hpXFDLHDj", "hpXFDLHDj"),
            Arguments.of("https://abysscdn.com/?v=vG3vP922G", "vG3vP922G"),
            Arguments.of("https://abysscdn.com/?v=jW0HhYs6y", "jW0HhYs6y")
        )

        @JvmStatic
        @BeforeAll
        fun setUp() {
            startKoin {
                modules(koinModule)
            }
        }
    }
}