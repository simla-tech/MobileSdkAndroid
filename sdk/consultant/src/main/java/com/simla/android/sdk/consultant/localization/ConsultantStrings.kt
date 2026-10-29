package com.simla.android.sdk.consultant.localization

import android.content.Context
import androidx.annotation.StringRes
import com.simla.android.sdk.consultant.R

internal class ConsultantStrings(context: Context) {
    private val resources = context.applicationContext.resources

    val now: String get() = get(R.string.crm_consultant_now)
    val you: String get() = get(R.string.crm_consultant_you)
    val agent: String get() = get(R.string.crm_consultant_agent)
    val productMessage: String get() = get(R.string.crm_consultant_product_message)
    val orderMessage: String get() = get(R.string.crm_consultant_order_message)
    val attachment: String get() = get(R.string.crm_consultant_attachment)
    val unsupportedMessage: String get() = get(R.string.crm_consultant_unsupported_message)
    val inStock: String get() = get(R.string.crm_consultant_in_stock)
    val outOfStock: String get() = get(R.string.crm_consultant_out_of_stock)
    val orderAddress: String get() = get(R.string.crm_consultant_order_address)
    val orderDiscount: String get() = get(R.string.crm_consultant_order_discount)
    val orderTotal: String get() = get(R.string.crm_consultant_order_total)
    val orderPayment: String get() = get(R.string.crm_consultant_order_payment)

    fun orderNumber(number: String): String = get(R.string.crm_consultant_order_number, number)

    fun uploadSizeError(isImage: Boolean, maxSizeMb: Long): String = get(
        if (isImage) R.string.crm_consultant_image_size_error else R.string.crm_consultant_file_size_error,
        maxSizeMb,
    )

    fun get(@StringRes id: Int, vararg args: Any): String = resources.getString(id, *args)
}
