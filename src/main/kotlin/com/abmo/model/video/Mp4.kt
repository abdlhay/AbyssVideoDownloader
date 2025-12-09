package com.abmo.model.video

import com.abmo.model.SimpleVideo
import com.google.gson.annotations.SerializedName

data class Mp4(
    val domains: List<String?>? = null,
    @SerializedName("fristDatas")
    val firstDatas: List<FirstData?>? = null,
    val sources: List<Source?>? = null,
    val slug: String? = null,
    val md5_id: Int? = null
)


fun Mp4.toSimpleVideo(resolution: String): SimpleVideo {
    val source = sources?.find { it?.label == resolution }
    return SimpleVideo(
        slug = slug,
        md5_id = md5_id,
        label = source?.label,
        size = source?.size,
        url = buildSegmentUrl(domains?.firstOrNull(), source?.sub),
        path = source?.path,
        resId = source?.res_id
    )
}


private fun buildSegmentUrl(domain: String?, subdomain: String?): String {
    return "https://$subdomain.${domain?.substringAfter(".")}"
}