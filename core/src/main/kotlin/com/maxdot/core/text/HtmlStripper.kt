package com.maxdot.core.text

/** Converts (X)HTML book content into plain text with paragraph breaks. */
object HtmlStripper {

    private val ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "nbsp" to " ", "mdash" to "—", "ndash" to "–", "hellip" to "...",
        "rsquo" to "'", "lsquo" to "'", "rdquo" to "\"", "ldquo" to "\"",
        "shy" to "", "copy" to "©", "eacute" to "é", "egrave" to "è",
        "agrave" to "à", "ccedil" to "ç", "ouml" to "ö", "uuml" to "ü",
        "auml" to "ä", "aelig" to "æ", "oelig" to "œ",
    )

    fun strip(html: String): String {
        var text = html
            .replace(Regex("(?is)<(head|style|script)\\b.*?</\\1>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|h[1-6]|li|blockquote|tr)>"), "\n\n")
            .replace(Regex("(?is)<[^>]*>"), "")

        text = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(text) { m ->
            val body = m.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") ->
                    body.drop(2).toIntOrNull(16)?.let { codePointToString(it) } ?: ""
                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.let { codePointToString(it) } ?: ""
                else -> ENTITIES[body.lowercase()] ?: m.value
            }
        }
        return text
    }

    private fun codePointToString(cp: Int): String =
        if (Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else ""
}
