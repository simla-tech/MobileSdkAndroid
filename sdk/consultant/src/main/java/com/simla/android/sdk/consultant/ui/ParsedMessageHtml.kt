package com.simla.android.sdk.consultant.ui

internal data class ParsedMessageHtml(
    val plainText: String,
    val links: List<ParsedMessageHtmlLink>,
)
