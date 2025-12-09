package com.abmo.model

data class SimpleVideo(
    val slug: String? = null,
    val md5_id: Int? = null,
    val label: String? = null,
    val size: Long? = null,
    var range: Range? = null,
    val url: String? = null,
    val path: String? = null,
    val resId: Int? = null
)