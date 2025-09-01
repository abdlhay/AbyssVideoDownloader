package com.abmo.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Fetches the HTML document from the URL represented by the string.
 *
 * @return The parsed `Document` object representing the HTML content.
 * @throws IllegalArgumentException if the URL is malformed or cannot be accessed.
 * @throws Exception if an I/O error occurs while attempting to retrieve the document.
 */
fun String.fetchDocument(): Document = Jsoup.connect(this).get()

/**
 * Parses the string as an HTML document using Jsoup.
 *
 * @return The parsed `Document` object representing the HTML content.
 */
fun String.toJsoupDocument(): Document = Jsoup.parse(this)
