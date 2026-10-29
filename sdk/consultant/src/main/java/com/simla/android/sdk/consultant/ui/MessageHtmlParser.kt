package com.simla.android.sdk.consultant.ui

internal fun parseMessageHtml(source: String): ParsedMessageHtml {
    if (source.isEmpty()) {
        return ParsedMessageHtml(plainText = "", links = emptyList())
    }

    val builder = StringBuilder(source.length)
    val links = mutableListOf<ParsedMessageHtmlLink>()
    var searchStart = 0

    MESSAGE_HTML_ANCHOR_REGEX.findAll(source).forEach { match ->
        val range = match.range
        if (range.first > searchStart) {
            appendMessageFragmentWithDetectedLinks(
                builder = builder,
                links = links,
                fragment = source.substring(searchStart, range.first),
            )
        }

        val url = decodeHtmlEntities(match.groupValues[2]).trim()
        val linkLabel = normalizeMessageHtmlFragment(match.groupValues[3])
        val start = builder.length
        builder.append(linkLabel)
        if (url.isNotBlank() && linkLabel.isNotEmpty()) {
            links += ParsedMessageHtmlLink(
                start = start,
                end = builder.length,
                url = url,
            )
        }

        searchStart = range.last + 1
    }

    if (searchStart < source.length) {
        appendMessageFragmentWithDetectedLinks(
            builder = builder,
            links = links,
            fragment = source.substring(searchStart),
        )
    }

    return ParsedMessageHtml(
        plainText = builder.toString(),
        links = links,
    )
}

internal fun normalizeMessageHtmlFragment(fragment: String): String {
    if (fragment.isEmpty()) {
        return ""
    }

    return decodeHtmlEntities(
        fragment
            .replace(MESSAGE_HTML_BREAK_REGEX, "\n")
            .replace(MESSAGE_HTML_BLOCK_CLOSE_REGEX, "\n")
            .replace(MESSAGE_HTML_TAG_REGEX, ""),
    )
}

internal fun appendMessageFragmentWithDetectedLinks(
    builder: StringBuilder,
    links: MutableList<ParsedMessageHtmlLink>,
    fragment: String,
) {
    val normalized = normalizeMessageHtmlFragment(fragment)
    if (normalized.isEmpty()) {
        return
    }

    var searchStart = 0
    MESSAGE_PLAIN_LINK_REGEX.findAll(normalized).forEach { match ->
        val range = match.range
        if (range.first > searchStart) {
            builder.append(normalized.substring(searchStart, range.first))
        }

        val rawLink = match.value
        val trimmedLink = rawLink.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
        val start = builder.length
        builder.append(trimmedLink)
        val end = builder.length
        if (trimmedLink.isNotEmpty()) {
            links += ParsedMessageHtmlLink(
                start = start,
                end = end,
                url = normalizeDetectedMessageUrl(trimmedLink),
            )
        }

        if (trimmedLink.length < rawLink.length) {
            builder.append(rawLink.substring(trimmedLink.length))
        }

        searchStart = range.last + 1
    }

    if (searchStart < normalized.length) {
        builder.append(normalized.substring(searchStart))
    }
}

internal fun normalizeDetectedMessageUrl(url: String): String {
    return if (MESSAGE_URL_SCHEME_REGEX.containsMatchIn(url)) {
        url
    } else {
        "https://$url"
    }
}

internal fun decodeHtmlEntities(source: String): String {
    if (!source.contains('&')) {
        return source
    }

    return MESSAGE_HTML_ENTITY_REGEX.replace(source) { match ->
        val token = match.groupValues[1]
        when {
            token.equals("amp", ignoreCase = true) -> "&"
            token.equals("lt", ignoreCase = true) -> "<"
            token.equals("gt", ignoreCase = true) -> ">"
            token.equals("quot", ignoreCase = true) -> "\""
            token.equals("apos", ignoreCase = true) -> "'"
            token.equals("nbsp", ignoreCase = true) -> "\u00A0"
            token.startsWith("#x", ignoreCase = true) -> {
                token.substring(2).toIntOrNull(16)?.let(::codePointToString) ?: match.value
            }

            token.startsWith("#") -> {
                token.substring(1).toIntOrNull()?.let(::codePointToString) ?: match.value
            }

            else -> match.value
        }
    }
}

internal fun codePointToString(codePoint: Int): String {
    return runCatching { String(Character.toChars(codePoint)) }.getOrDefault("")
}
