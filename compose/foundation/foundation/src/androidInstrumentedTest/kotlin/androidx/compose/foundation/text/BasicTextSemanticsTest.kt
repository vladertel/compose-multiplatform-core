/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextSemanticsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun semanticsTextChanges_String() {
        var text by mutableStateOf("before")
        rule.setContent {
            BasicText(text)
        }
        rule.onNodeWithText("before").assertExists()
        text = "after"
        rule.onNodeWithText("after").assertExists()
    }

    @Test
    fun semanticsTextChanges_AnnotatedString() {
        var text by mutableStateOf("before")
        rule.setContent {
            BasicText(AnnotatedString(text))
        }
        rule.onNodeWithText("before").assertExists()
        text = "after"
        rule.onNodeWithText("after").assertExists()
    }

    // regression test for b/376479686
    @Test
    fun inlineContentSemantics_matchesInMergedSemantics() {
        val testContentDescription = "Red Box"

        rule.setContent {
            val id = "inline"
            val inlineContent =
                InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)) {
                    Box(
                        modifier = Modifier
                            .semantics {
                                this.contentDescription = testContentDescription
                                this.role = Role.Image
                            }
                            .background(Color.Red)
                            .fillMaxSize()
                    )
                }
            val inlineContentMap = mapOf(id to inlineContent)
            val text = buildAnnotatedString {
                append("before text - ")
                appendInlineContent(id)
                append(" - after text")
            }
            BasicText(text, inlineContent = inlineContentMap)
        }

        val textInteraction = rule.onNode(
            matcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
            // This test is specifically for talkback functionality,
            // so using the merged tree that is seen in prod is required.
            // Do not change this to true, even if the test failure recommends it.
            useUnmergedTree = false,
        ).assertExists()

        assertThat(textInteraction.fetchSemanticsNode().children.size).isEqualTo(1)
        textInteraction.onChild().assert(hasContentDescriptionExactly(testContentDescription))
    }

    // regression test for b/376479686
    @Test
    fun linkAndInlineContentSemantics_matchesInMergedSemantics() {
        val testContentDescription = "Red Box"
        val id = "inline"
        val inlineContent =
            InlineTextContent(Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.Center)) {
                Box(
                    modifier = Modifier
                        .semantics {
                            this.contentDescription = testContentDescription
                            this.role = Role.Image
                        }
                        .background(Color.Red)
                        .fillMaxSize()
                )
            }
        val inlineContentMap = mapOf(id to inlineContent)
        val link = "developer.android.com"
        val linkAnnotation = LinkAnnotation.Url(link)

        val tag = "tag"
        rule.setContent {
            val text = buildAnnotatedString {
                append("before content - ")
                appendInlineContent(id)
                append(" - after content, before link -")
                withLink(linkAnnotation) { append("Link") }
                append(" - after link")
            }
            BasicText(text, inlineContent = inlineContentMap, modifier = Modifier.testTag(tag))
        }

        val interaction = rule.onNodeWithTag(
            testTag = tag,
            // This test is specifically for talkback functionality,
            // so using the merged tree that is seen in prod is required.
            // Do not change this to true, even if the test failure recommends it.
            useUnmergedTree = false,
        ).assertExists()

        assertThat(interaction.fetchSemanticsNode().children.size).isEqualTo(2)
        val childrenInteractions = interaction.onChildren()

        // inline content should have the expected content description.
        childrenInteractions[0].assert(hasContentDescriptionExactly(testContentDescription))

        // link should have onClick but also be invisible
        childrenInteractions[1]
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.InvisibleToUser))
    }
}
