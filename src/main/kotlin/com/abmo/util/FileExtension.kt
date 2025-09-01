package com.abmo.util

import com.abmo.common.Logger
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths

fun isValidPath(filePath: String?): Boolean {
    if (filePath.isNullOrBlank()) return false

    return try {
        val outputFile = File(filePath).canonicalFile
        val outputDir = outputFile.parentFile

        // check for invalid file path
        if (filePath.endsWith("/") || filePath.endsWith("\\")) {
            Logger.error("Invalid File path.")
            return false
        }

        // check if the output directory exists and is a directory
        outputDir?.let {
            if (!it.exists()) {
                Logger.error("Output directory does not exist: ${it.absolutePath}")
                return false
            }
            if (!it.isDirectory) {
                Logger.error("Output path is not a directory: ${it.absolutePath}")
                return false
            }
        }

        // check if file name is valid
        if (outputFile.name.isBlank()) {
            Logger.error("No valid file name specified.")
            return false
        }

        // check if file extension is .mp4 (not about sure about restricting extension here but mostly source uses mp4)
        if (!outputFile.name.endsWith(".mp4", ignoreCase = true)) {
            Logger.error("File must have a .mp4 extension: ${outputFile.name}")
            return false
        }

        // check if file already exists
        if (outputFile.exists()) {
            Logger.error("File already exists: ${outputFile.absolutePath}")
            return false
        }

        true // all checks passed
    } catch (e: FileNotFoundException) {
        Logger.error("Unable to access path: ${e.message}")
        false
    } catch (e: SecurityException) {
        Logger.error("Error: Access denied to path: $filePath. ${e.message}")
        false
    }
}

fun getDownloadsFolder(): File {
    val home = System.getProperty("user.home")
    val downloads = Paths.get(home, "Downloads").toFile()
    return if (downloads.exists() && downloads.isDirectory) {
        downloads
    } else {
        File(home) // fallback to home folder
    }
}


fun String?.sanitizeFileName(): String? {
    if (isNullOrEmpty()) return null
    val cleaned = replace(Regex("""[<>:"/\\|?*\u0000-\u001F]"""), "_")

    val ext = ".mp4"
    val withExtension = if (cleaned.lowercase().endsWith(ext)) cleaned else "$cleaned$ext"

    val maxLength = 100
    return if (withExtension.length > maxLength) {
        withExtension.take(maxLength - ext.length) + ext
    } else {
        withExtension
    }
}