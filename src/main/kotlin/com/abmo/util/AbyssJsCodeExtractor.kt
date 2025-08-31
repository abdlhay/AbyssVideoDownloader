package com.abmo.util

import com.abmo.common.Logger

class AbyssJsCodeExtractor {

    companion object {
        private val FUNCTION_WITH_SHIFT_PUSH_REGEX = Regex(
            """\(function\s*\([^)]*\)\s*\{(?:[^{}]++|\{(?:[^{}]++|\{[^{}]*+})*+})*+}(?:\([^)]*\))?\s*;?\)""",
            RegexOption.DOT_MATCHES_ALL
        )

        private val SELF_ASSIGNING_FUNCTION_REGEX = Regex(
            """function\s+(\w+)\s*\(\)\s*\{\s*var\s+(\w+)\s*=\s*\[.*?];\s*\1\s*=\s*function\s*\(\)\s*\{\s*return\s+\2;\s*};\s*return\s+\1\(\);\s*}""",
            RegexOption.DOT_MATCHES_ALL
        )

        private val VARIABLE_ASSIGNMENT_REGEX = Regex("""var\s+\*?([a-zA-Z0-9_]+)\s*=\s*\*?([a-zA-Z0-9_]+);""")
    }

    fun getCompleteJsCode(jsResponse: String?): String? {

        val relevantSection = jsResponse?.between("JSON", "...")

        if (jsResponse.isNullOrBlank()) {
            Logger.error("Empty response received")
            return null
        }

        val varName = extractVariableName(jsResponse)
        if (varName == null) {
            Logger.error("Variable name not found")
            return null
        }

        if (relevantSection == null) {
            Logger.error("relevantSection wasn't found")
            return null
        }

        val functionVariableNamesPair = extractVarNamesFromFunction(jsResponse)
        if (functionVariableNamesPair == null || functionVariableNamesPair.toList().isEmpty()) {
            Logger.error("failed to retrieve variable pair from the function")
            return null
        }

        return buildCompleteCode(
            jsResponse = jsResponse,
            varName = varName
        )
    }

    private fun buildCompleteCode(
        jsResponse: String,
        varName: String
    ): String = buildString {
        extractShiftPushFunctions(jsResponse)?.let { appendLine(it) }
        extractSelfAssigningFunction(jsResponse)?.let { appendLine(it) }
        extractFunction(jsResponse, varName)?.let { appendLine(it) }
        val videoObject = getVideoMetadataObject(jsResponse)
        appendLine(buildVarDeclaration(videoObject, varName))
        appendLine(videoObject)
        appendLine("var b = Object.assign({}, ${getVideoMetaDataVariableName(videoObject)});")
        appendLine("b.sourcesEncrypted = ${getEncryptedSourceVariableName(videoObject)};")
        appendLine("java.lang.System.out.println(JSON.stringify(b))")
    }

    // This is the worst shit I’ve written for extraction but who cares ~ it works :V
    private fun getVideoMetadataObject(jsCode: String): String {
        val result = jsCode.between("JSON", "...")
            .replaceLast("," ,"")
            .replace("...{", "")
            .split(",_")
            .filterNot {
                it.contains("JSON") || it.contains("window")
            }.map { "_$it" }
        val shortestEntity = result.minBy { it.length }
        return result.filterNot { it == shortestEntity }.toString()
            .substringAfter("[").substringBeforeLast("]")
    }

    private fun getEncryptedSourceVariableName(videoObject: String): String {
        return videoObject.substringBefore("=")
    }

    private fun getVideoMetaDataVariableName(videoObject: String): String {
        return videoObject.between(",","={").replace(",", "").trim()
    }

    private fun buildVarDeclaration(videoObject: String, functionName: String): String {
        val varName = "_" + videoObject.substringAfter("=")
            .substringAfter("_").substringBefore("(")
        return "var $varName = $functionName"
    }

    private fun extractVariableName(jsCode: String): String? =
        VARIABLE_ASSIGNMENT_REGEX.find(jsCode)?.groupValues?.get(2)

    private fun extractShiftPushFunctions(jsCode: String): String? =
        FUNCTION_WITH_SHIFT_PUSH_REGEX.findAll(jsCode)
            .map { it.value }
            .filter { functionCode ->
                containsPattern(functionCode, "shift") && containsPattern(functionCode, "push")
            }
            .firstOrNull()

    private fun containsPattern(code: String, pattern: String): Boolean =
        code.contains(Regex("""\b$pattern\b|\['$pattern']|\.$pattern\("""))

    private fun extractSelfAssigningFunction(jsCode: String): String? =
        SELF_ASSIGNING_FUNCTION_REGEX.find(jsCode)?.value

    private fun extractFunction(jsCode: String, functionName: String): String? {
        val pattern = Regex(
            """function\s+($functionName)\s*\([^)]*\)\s*\{((?:[^{}]++|\{(?:[^{}]++|\{[^{}]*+})*+})*+)}"""
        )
        return pattern.find(jsCode)?.value
    }

    private fun extractVarNamesFromFunction(code: String): Pair<String, String>? {
        val regex = Regex(
            """function\s+[A-Za-z_$][\w$]*\s*\(\s*\)\s*\{\s*var\s+([A-Za-z_$][\w$]*)\s*=\s*([A-Za-z_$][\w$]*)\s*;\s*try""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

        val match = regex.find(code)
        return match?.destructured?.let { (lhs, rhs) -> lhs to rhs }
    }

}











