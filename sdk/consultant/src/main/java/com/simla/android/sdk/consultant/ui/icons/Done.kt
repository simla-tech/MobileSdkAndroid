/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simla.android.sdk.consultant.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

internal val ConsultantIcons.Done: ImageVector
    get() {
        if (_done != null) {
            return _done!!
        }
        _done = materialIcon(name = "Outlined.Done") {
            materialPath {
                moveTo(9.0f, 16.2f)
                lineTo(4.8f, 12.0f)
                lineToRelative(-1.4f, 1.4f)
                lineTo(9.0f, 19.0f)
                lineTo(21.0f, 7.0f)
                lineToRelative(-1.4f, -1.4f)
                lineTo(9.0f, 16.2f)
                close()
            }
        }
        return _done!!
    }

private var _done: ImageVector? = null

