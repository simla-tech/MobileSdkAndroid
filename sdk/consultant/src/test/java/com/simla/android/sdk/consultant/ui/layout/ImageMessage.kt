package com.simla.android.sdk.consultant.ui.layout

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import com.simla.android.sdk.consultant.ui.ConsultantImageBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantImageBubbleLayoutRules
import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import com.simla.android.sdk.consultant.ui.imageBubbleContentMaxWidth
import com.simla.android.sdk.consultant.ui.imageMessageLayoutSourceSizePx
import com.simla.android.sdk.consultant.ui.imageMetaOverlayAlignment
import com.simla.android.sdk.consultant.ui.imageMetaOverlayRequiredWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageMessageTest {
    // Проверяет горизонтальное изображение: bubble сохраняет aspect ratio картинки и укладывается в MAX_BUBBLE_WIDTH.
    @Test
    fun horizontal_image_message_preserves_image_ratio_inside_max_size() {
        val result = resolveImage(imageWidthPx = 1024, imageHeightPx = 285)

        assertEquals(280, result.imageWidthPx)
        assertEquals(78, result.imageHeightPx)
        assertEquals(ContentScale.Fit, result.contentScale)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
    }

    // Проверяет вертикальное изображение: bubble сохраняет aspect ratio картинки и укладывается в MAX_BUBBLE_HEIGHT.
    @Test
    fun vertical_image_message_preserves_image_ratio_inside_max_size() {
        val result = resolveImage(imageWidthPx = 132, imageHeightPx = 500)

        assertEquals(74, result.imageWidthPx)
        assertEquals(280, result.imageHeightPx)
        assertEquals(ContentScale.Fit, result.contentScale)
    }

    // Проверяет квадратное изображение: bubble остается квадратным после масштабирования.
    @Test
    fun square_image_message_preserves_square_ratio() {
        val result = resolveImage(imageWidthPx = 1024, imageHeightPx = 1024)

        assertEquals(280, result.imageWidthPx)
        assertEquals(280, result.imageHeightPx)
        assertEquals(ContentScale.Fit, result.contentScale)
    }

    // Checks a 500x500 QR image on a high-density screen: the bubble uses available width
    // and keeps the same size before and after the bitmap dimensions are available.
    // Проверяет маленькое изображение: bubble увеличивается минимум до ширины author/meta.
    @Test
    fun square_qr_image_message_uses_available_width_without_resize_jump() {
        val maxContentSizePx = 936
        val loadingResult = resolveImage(
            imageWidthPx = maxContentSizePx,
            imageHeightPx = maxContentSizePx,
            maxBubbleContentWidthPx = maxContentSizePx,
            maxBubbleContentHeightPx = maxContentSizePx,
        )
        val loadedSourceSizePx = imageMessageLayoutSourceSizePx(
            sourceSizePx = 500,
            densityScale = 3f,
            fallbackSizePx = maxContentSizePx,
        )
        val loadedResult = resolveImage(
            imageWidthPx = loadedSourceSizePx,
            imageHeightPx = loadedSourceSizePx,
            maxBubbleContentWidthPx = maxContentSizePx,
            maxBubbleContentHeightPx = maxContentSizePx,
        )

        assertEquals(1500, loadedSourceSizePx)
        assertEquals(maxContentSizePx, loadingResult.bubbleContentWidthPx)
        assertEquals(maxContentSizePx, loadingResult.imageWidthPx)
        assertEquals(maxContentSizePx, loadingResult.imageHeightPx)
        assertEquals(loadingResult, loadedResult)
        assertEquals(ContentScale.Fit, loadedResult.contentScale)
    }

    @Test
    fun tiny_image_message_grows_to_fit_author_and_meta() {
        val result = resolveImage(
            imageWidthPx = 40,
            imageHeightPx = 40,
            authorWidthPx = 96,
            metaWidthPx = 51,
        )

        assertEquals(96, result.bubbleContentWidthPx)
        assertEquals(96, result.imageWidthPx)
        assertEquals(96, result.imageHeightPx)
        assertEquals(ContentScale.Fit, result.contentScale)
    }

    // Проверяет, что при изменении пропорций bubble выбирается centerCrop, чтобы картинка заполняла всю область.
    @Test
    fun tiny_image_message_grows_to_fit_padded_meta_overlay() {
        // Проверяет image-01: bubble учитывает полный размер meta-overlay с padding, чтобы timestamp не обрезался внутри картинки.
        val metaOverlayWidth = imageMetaOverlayRequiredWidth(
            metaContentWidthPx = 134,
            overlayEdgePaddingPx = 11,
            overlayHorizontalPaddingPx = 6,
        )
        val result = resolveImage(
            imageWidthPx = 40,
            imageHeightPx = 40,
            authorWidthPx = 67,
            metaWidthPx = metaOverlayWidth,
            metaHeightPx = 44,
            maxBubbleContentWidthPx = 772,
            maxBubbleContentHeightPx = 772,
        )

        assertEquals(168, metaOverlayWidth)
        assertEquals(168, result.bubbleContentWidthPx)
        assertEquals(168, result.imageWidthPx)
        assertEquals(168, result.imageHeightPx)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(ContentScale.Fit, result.contentScale)
    }

    // Проверяет, что при изменении пропорций bubble выбирается centerCrop, чтобы картинка заполняла всю область.
    @Test
    fun image_message_uses_center_crop_when_bubble_ratio_differs_from_image_ratio() {
        val result = resolveImage(
            imageWidthPx = 20,
            imageHeightPx = 300,
            authorWidthPx = 96,
            metaWidthPx = 51,
        )

        assertEquals(96, result.imageWidthPx)
        assertEquals(280, result.imageHeightPx)
        assertEquals(ContentScale.Crop, result.contentScale)
    }

    // Проверяет, что если изображение ниже meta после расчета, meta переносится под картинку без overlay.
    @Test
    fun too_low_image_message_moves_meta_below_image() {
        val result = resolveImage(
            imageWidthPx = 120,
            imageHeightPx = 8,
            metaHeightPx = 16,
            maxBubbleContentWidthPx = 280,
            maxBubbleContentHeightPx = 8,
        )

        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
    }

    // Проверяет, что meta-overlay в image-message всегда прижат к правому нижнему краю картинки.
    @Test
    fun image_message_meta_overlay_is_always_on_right_edge() {
        assertEquals(Alignment.BottomEnd, imageMetaOverlayAlignment())
    }

    // Проверяет, что image-message использует лимит bubble с image-padding, а не text-padding.
    @Test
    fun image_message_content_width_uses_image_bubble_padding() {
        assertEquals(
            312,
            imageBubbleContentMaxWidth(
                maxBubbleWidthPx = 320,
                imageBubblePaddingPx = 4,
            ),
        )
    }

    private fun resolveImage(
        imageWidthPx: Int,
        imageHeightPx: Int,
        authorWidthPx: Int = 0,
        metaWidthPx: Int = 51,
        metaHeightPx: Int = 16,
        maxBubbleContentWidthPx: Int = 280,
        maxBubbleContentHeightPx: Int = 280,
    ) = ConsultantImageBubbleLayoutRules.resolve(
        ConsultantImageBubbleLayoutInput(
            imageWidthPx = imageWidthPx,
            imageHeightPx = imageHeightPx,
            authorWidthPx = authorWidthPx,
            metaWidthPx = metaWidthPx,
            metaHeightPx = metaHeightPx,
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
            maxBubbleContentHeightPx = maxBubbleContentHeightPx,
        ),
    )
}
