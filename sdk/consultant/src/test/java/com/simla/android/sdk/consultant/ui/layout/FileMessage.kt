package com.simla.android.sdk.consultant.ui.layout

import com.simla.android.sdk.consultant.ui.ConsultantFileBubbleLayoutInput
import com.simla.android.sdk.consultant.ui.ConsultantFileBubbleLayoutRules
import com.simla.android.sdk.consultant.ui.ConsultantMeasuredMeta
import com.simla.android.sdk.consultant.ui.ConsultantMessageMetaPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class FileMessageTest {
    // Проверяет, что ширина file-блока считается по имени файла, если оно шире строки с размером файла.
    @Test
    fun file_message_width_uses_file_name_when_name_is_wider() {
        val result = resolveFile(
            fileNameWidthPx = 120,
            fileSizeWidthPx = 40,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(170, result.bubbleContentWidthPx)
        assertEquals(170, result.fileContentWidthPx)
        assertEquals(120, result.fileTextColumnWidthPx)
    }

    // Проверяет, что ширина file-блока считается по строке размера файла, если она шире имени файла.
    @Test
    fun file_message_width_uses_file_size_when_size_is_wider() {
        val result = resolveFile(
            fileNameWidthPx = 48,
            fileSizeWidthPx = 100,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(213, result.bubbleContentWidthPx)
        assertEquals(213, result.fileContentWidthPx)
        assertEquals(163, result.fileTextColumnWidthPx)
    }

    // Проверяет, что meta поднимается на строку размера файла, если рядом с размером есть место.
    @Test
    fun file_message_partially_overlaps_meta_with_size_line_when_there_is_free_space() {
        val result = resolveFile(
            fileNameWidthPx = 180,
            fileSizeWidthPx = 40,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(230, result.bubbleContentWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.INLINE_OVERLAY, result.metaPlacement)
        assertEquals(4, result.metaOverlayOffsetYPx)
    }

    // Проверяет, что meta остается снизу, если на строке размера файла не хватает места.
    @Test
    fun file_message_places_meta_below_when_size_line_has_no_room() {
        val result = resolveFile(
            fileNameWidthPx = 180,
            fileSizeWidthPx = 150,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 190,
        )

        assertEquals(190, result.bubbleContentWidthPx)
        assertEquals(190, result.fileContentWidthPx)
        assertEquals(ConsultantMessageMetaPlacement.BELOW_TEXT, result.metaPlacement)
    }

    // Проверяет, что общий размер bubble увеличивается до ширины author, если имя автора шире file-блока.
    @Test
    fun file_message_grows_to_fit_author() {
        val result = resolveFile(
            authorWidthPx = 240,
            fileNameWidthPx = 120,
            fileSizeWidthPx = 40,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 280,
        )

        assertEquals(240, result.bubbleContentWidthPx)
        assertEquals(170, result.fileContentWidthPx)
        assertEquals(240, result.authorWidthPx)
    }

    // Проверяет, что file-блок и bubble не выходят за MAX_BUBBLE_WIDTH контейнера.
    @Test
    fun file_message_respects_max_bubble_content_width() {
        val result = resolveFile(
            authorWidthPx = 260,
            fileNameWidthPx = 300,
            fileSizeWidthPx = 240,
            metaWidthPx = 51,
            maxBubbleContentWidthPx = 180,
        )

        assertEquals(180, result.bubbleContentWidthPx)
        assertEquals(180, result.fileContentWidthPx)
        assertEquals(180, result.authorWidthPx)
    }

    private fun resolveFile(
        authorWidthPx: Int = 0,
        fileNameWidthPx: Int,
        fileSizeWidthPx: Int,
        metaWidthPx: Int,
        maxBubbleContentWidthPx: Int,
    ) = ConsultantFileBubbleLayoutRules.resolve(
        ConsultantFileBubbleLayoutInput(
            authorWidthPx = authorWidthPx,
            fileNameWidthPx = fileNameWidthPx,
            fileSizeWidthPx = fileSizeWidthPx,
            iconWidthPx = 42,
            iconTextSpacingPx = 8,
            fileSizeLineHeightPx = 16,
            fileSizeLineBaselinePx = 12,
            meta = ConsultantMeasuredMeta(
                contentWidthPx = metaWidthPx,
                inlineStartSpacingPx = 12,
                heightPx = 16,
                hasStatusIcon = true,
            ),
            metaWidthPx = metaWidthPx,
            maxBubbleContentWidthPx = maxBubbleContentWidthPx,
        ),
    )
}
