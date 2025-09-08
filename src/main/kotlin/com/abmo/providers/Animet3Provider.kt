package com.abmo.providers

import com.abmo.util.fetchDocument
import com.abmo.util.getParameter
import com.abmo.util.toJsoupDocument
import com.mashape.unirest.http.Unirest


class Animet3Provider: Provider {

    override fun getVideoID(url: String): String? {
        val regex = Regex("""MovieId\s*=\s*'(\d+)',\s*EpisodeId\s*=\s*'(\d+)',""")
        val matchResult = regex.find(url.fetchDocument().html()) ?: return null

        val movieId = matchResult.groupValues[1]
        val episodeId = matchResult.groupValues[2]

        val response = Unirest.post("https://anime11.site/ajax/player_hrx")
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .body("id=$movieId&ep=$episodeId&sv=hrx-0")
            .asString().body

        val videoID = response.toJsoupDocument()
            .select("iframe")
            .attr("src").getParameter("v")

        return videoID
    }

}