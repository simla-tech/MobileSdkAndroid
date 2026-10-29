package com.simla.android.sdk.consultant.ui

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineConsultantScreenDirectionTest {
    @Test
    fun rtl_text_uses_rtl_text_direction() {
        assertEquals(
            TextDirection.ContentOrRtl,
            composeTextDirectionFor("مستشار طويل الاسم"),
        )
    }

    @Test
    fun ltr_text_uses_ltr_text_direction() {
        assertEquals(
            TextDirection.ContentOrLtr,
            composeTextDirectionFor("Consultant"),
        )
    }

    @Test
    fun rtl_text_aligns_physically_right() {
        assertEquals(
            TextAlign.Right,
            bubbleTextAlignFor(ConsultantMessageTextDirection.RTL),
        )
    }

    @Test
    fun ltr_text_aligns_physically_left() {
        assertEquals(
            TextAlign.Left,
            bubbleTextAlignFor(ConsultantMessageTextDirection.LTR),
        )
    }

    @Test
    fun rtl_meta_is_anchored_to_logical_end() {
        assertEquals(
            Alignment.BottomStart,
            bubbleMetaAlignmentFor(ConsultantMessageTextDirection.RTL),
        )
    }

    @Test
    fun ltr_meta_is_anchored_to_logical_end() {
        assertEquals(
            Alignment.BottomEnd,
            bubbleMetaAlignmentFor(ConsultantMessageTextDirection.LTR),
        )
    }
}
