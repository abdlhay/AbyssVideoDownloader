package com.abmo.model.dto

import com.abmo.model.Ads
import com.abmo.model.Source
import com.abmo.model.Video

data class VideoDto(
    val ads: Ads? = Ads(),
    val doNotSaveCookies: Boolean? = false,
    val domain: String? = null,
    val fullscreenOrientationLock: String? = null,
    val height: String? = null,
    val id: String? = null,
    val image: String? = null,
    val isOnlyTunnel: Boolean? = false,
    val md5_id: Int? = 0,
    val pipIcon: String? = null,
    val preload: String? = null,
    val preview: Boolean? = false,
    val slug: String? = null,
    val sourcesEncrypted: String? = null,
    val tracker: Tracker? = Tracker(),
    val user_id: Int? = 0,
    val width: String? = null
)


fun VideoDto.toVideo(sources: List<Source?>?): Video {
    return Video(
        ads = ads,
        doNotSaveCookies = doNotSaveCookies,
        domain = domain,
        fullscreenOrientationLock = fullscreenOrientationLock,
        height = height,
        id =  id,
        md5_id = md5_id,
        pipIcon = pipIcon,
        preload = preload,
        slug = slug,
        sources = sources
    )
}