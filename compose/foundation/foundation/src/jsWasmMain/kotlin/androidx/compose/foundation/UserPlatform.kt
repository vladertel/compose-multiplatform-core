/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import kotlinx.browser.window

internal enum class UserPlatform {
    Android,
    IOS,
    Linux,
    MacOS,
    Windows,
    Unknown;

    companion object {
        /**
         * In a browser, user platform can be obtained from different places:
         * - we attempt to use not-deprecated but experimental option first (not available in all browsers)
         * - then we attempt to use a deprecated option
         * - if both above return an empty string, we attempt to get `Platform` from `userAgent`
         *
         * Note: a client can spoof these values, so it's okay only for non-critical use cases.
         */
        val Current: UserPlatform by lazy {
            val platformInfo = getNavigatorInfo().takeIf {
                it.isNotEmpty()
            } ?: window.navigator.userAgent

            when {
                platformInfo.contains("Android", true) -> Android
                platformInfo.contains("iPhone", true) -> IOS
                platformInfo.contains("iOS", true) -> IOS
                platformInfo.contains("iPad", true) -> IOS
                platformInfo.contains("Linux", true) -> Linux
                platformInfo.contains("Mac", true) -> MacOS
                platformInfo.contains("Win", true) -> Windows
                else -> Unknown
            }
        }
    }
}

/**
 * A string identifying the platform on which the user's browser is running; for example:
 * "MacIntel", "Win32", "Linux x86_64", "Linux x86_64".
 * See https://developer.mozilla.org/en-US/docs/Web/API/Navigator/platform - deprecated
 *
 * A string containing the platform brand. For example, "Windows".
 * See https://developer.mozilla.org/en-US/docs/Web/API/NavigatorUAData/platform - new API,
 * but not supported in all browsers
 */
internal fun getNavigatorInfo(): String =
    js("navigator.userAgentData ? navigator.userAgentData.platform : navigator.platform") as String

