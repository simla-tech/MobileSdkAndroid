package com.simla.android.sdk.consultant.ui

internal const val DEFAULT_CONTACT_PHONE_COUNTRY_CODE = "7"
internal const val DEFAULT_CONTACT_PHONE_PLACEHOLDER = "+7-___-___-__-__"

private const val DEFAULT_CONTACT_PHONE_NUMBER_TEMPLATE = "###-###-##-##"
private const val DEFAULT_CONTACT_PHONE_MIN_NUMBER_LENGTH = 8
private const val DEFAULT_CONTACT_PHONE_MAX_NUMBER_LENGTH = 10

internal fun normalizeContactPhoneInput(
    value: String,
    countryCode: String = DEFAULT_CONTACT_PHONE_COUNTRY_CODE,
): String {
    val countryDigits = countryCode.filter(Char::isDigit)
    val digits = value.filter(Char::isDigit)

    if (countryDigits.isEmpty() || digits.isEmpty()) {
        return ""
    }

    val numberDigits = when {
        digits.startsWith(countryDigits) -> digits.drop(countryDigits.length)
        countryDigits == DEFAULT_CONTACT_PHONE_COUNTRY_CODE &&
                digits.length == DEFAULT_CONTACT_PHONE_MAX_NUMBER_LENGTH + 1 &&
                digits.startsWith("8") -> digits.drop(1)

        else -> digits
    }.take(DEFAULT_CONTACT_PHONE_MAX_NUMBER_LENGTH)

    return countryDigits + numberDigits
}

internal fun formatContactPhoneDisplay(
    phone: String,
    countryCode: String = DEFAULT_CONTACT_PHONE_COUNTRY_CODE,
): String {
    val normalized = normalizeContactPhoneInput(phone, countryCode)
    val countryDigits = countryCode.filter(Char::isDigit)
    if (normalized.isEmpty() || countryDigits.isEmpty()) {
        return ""
    }

    val numberDigits = normalized.removePrefix(countryDigits)
    if (numberDigits.isEmpty()) {
        return "+$countryDigits-"
    }

    return "+$countryDigits-${applyPhoneTemplate(numberDigits)}"
}

internal fun contactPhoneValueForSubmit(
    phone: String,
    countryCode: String = DEFAULT_CONTACT_PHONE_COUNTRY_CODE,
): String? {
    val normalized = normalizeContactPhoneInput(phone, countryCode)
    val countryDigits = countryCode.filter(Char::isDigit)
    val numberLength = normalized.removePrefix(countryDigits).length

    return normalized.takeIf {
        numberLength in DEFAULT_CONTACT_PHONE_MIN_NUMBER_LENGTH..DEFAULT_CONTACT_PHONE_MAX_NUMBER_LENGTH
    }
}

private fun applyPhoneTemplate(numberDigits: String): String {
    val builder = StringBuilder()
    var digitIndex = 0

    for (char in DEFAULT_CONTACT_PHONE_NUMBER_TEMPLATE) {
        if (char == '#') {
            if (digitIndex >= numberDigits.length) {
                break
            }
            builder.append(numberDigits[digitIndex])
            digitIndex += 1
        } else if (digitIndex < numberDigits.length) {
            builder.append(char)
        }
    }

    return builder.toString().trimEnd('-')
}
