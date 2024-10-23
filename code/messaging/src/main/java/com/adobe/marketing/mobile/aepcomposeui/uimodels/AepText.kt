/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.uimodels

/**
 * Data class representing a text element in the UI.
 *
 * @property content The content of the text.
 * @property color The color of the text.
 * @property align The alignment of the text (e.g., left, right, center).
 * @property font The font styling of the text, represented by an [AepFont] object.
 */
data class AepText(
    val content: String,
    val color: AepColor? = null,
    val align: String? = null,
    val font: AepFont? = null
)