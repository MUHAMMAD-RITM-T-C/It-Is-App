package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HTML Code Studio", appName)
  }

  @Test
  fun `ai api key manager saves and retrieves custom key`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.ai.AiApiKeyManager(context)
    manager.saveCustomApiKey("test_key_12345")
    assertEquals("test_key_12345", manager.getApiKey())
  }

  @Test
  fun `syntax highlighter handles code without crashing`() {
    val htmlCode = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Hello</h1></body></html>"
    val transformed = com.example.ui.editor.syntax.CodeSyntaxVisualTransformation.highlightCode(
        htmlCode,
        com.example.ui.editor.syntax.CodeLanguage.HTML,
        com.example.ui.editor.theme.EditorThemeType.VS_CODE_DARK
    )
    assertEquals(htmlCode, transformed.text)
  }
}
