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

internal val ConsultantIcons.SupportAgent: ImageVector
    get() {
        if (_supportAgent != null) {
            return _supportAgent!!
        }
        _supportAgent = materialIcon(name = "Outlined.SupportAgent") {
            materialPath {
                moveTo(21.0f, 12.22f)
                curveTo(21.0f, 6.73f, 16.74f, 3.0f, 12.0f, 3.0f)
                curveToRelative(-4.69f, 0.0f, -9.0f, 3.65f, -9.0f, 9.28f)
                curveTo(2.4f, 12.62f, 2.0f, 13.26f, 2.0f, 14.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(-6.1f)
                curveToRelative(0.0f, -3.87f, 3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
                verticalLineTo(19.0f)
                horizontalLineToRelative(-8.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-1.22f)
                curveToRelative(0.59f, -0.31f, 1.0f, -0.92f, 1.0f, -1.64f)
                verticalLineToRelative(-2.3f)
                curveTo(22.0f, 13.14f, 21.59f, 12.53f, 21.0f, 12.22f)
                close()
            }
            materialPath {
                moveTo(9.0f, 13.0f)
                moveToRelative(-1.0f, 0.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, true, true, 2.0f, 0.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, true, true, -2.0f, 0.0f)
            }
            materialPath {
                moveTo(15.0f, 13.0f)
                moveToRelative(-1.0f, 0.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, true, true, 2.0f, 0.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, true, true, -2.0f, 0.0f)
            }
            materialPath {
                moveTo(18.0f, 11.03f)
                curveTo(17.52f, 8.18f, 15.04f, 6.0f, 12.05f, 6.0f)
                curveToRelative(-3.03f, 0.0f, -6.29f, 2.51f, -6.03f, 6.45f)
                curveToRelative(2.47f, -1.01f, 4.33f, -3.21f, 4.86f, -5.89f)
                curveTo(12.19f, 9.19f, 14.88f, 11.0f, 18.0f, 11.03f)
                close()
            }
        }
        return _supportAgent!!
    }

private var _supportAgent: ImageVector? = null

