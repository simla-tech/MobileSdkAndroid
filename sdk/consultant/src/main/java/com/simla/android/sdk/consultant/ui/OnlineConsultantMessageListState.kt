package com.simla.android.sdk.consultant.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver

internal class OnlineConsultantMessageListState internal constructor(
    val lazyListState: LazyListState,
) {
    internal val imageDimensionsByUrl = mutableStateMapOf<String, Pair<Int, Int>>()

    internal companion object {
        private const val FIRST_VISIBLE_ITEM_INDEX = "firstVisibleItemIndex"
        private const val FIRST_VISIBLE_ITEM_SCROLL_OFFSET = "firstVisibleItemScrollOffset"

        val Saver: Saver<OnlineConsultantMessageListState, Any> = mapSaver(
            save = { state ->
                mapOf(
                    FIRST_VISIBLE_ITEM_INDEX to state.lazyListState.firstVisibleItemIndex,
                    FIRST_VISIBLE_ITEM_SCROLL_OFFSET to state.lazyListState.firstVisibleItemScrollOffset,
                )
            },
            restore = { restored ->
                OnlineConsultantMessageListState(
                    lazyListState = LazyListState(
                        firstVisibleItemIndex = restored[FIRST_VISIBLE_ITEM_INDEX] as? Int ?: 0,
                        firstVisibleItemScrollOffset = restored[FIRST_VISIBLE_ITEM_SCROLL_OFFSET] as? Int
                            ?: 0,
                    ),
                )
            },
        )
    }
}
