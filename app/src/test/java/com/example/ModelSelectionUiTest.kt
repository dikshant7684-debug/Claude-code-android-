package com.example

import com.example.ui.components.getModelCardInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelSelectionUiTest {

  @Test
  fun testModelCardInfo_metadataIsComplete() {
    val claude = getModelCardInfo("Claude 3.7 Sonnet")
    assertEquals("Claude 3.7 Sonnet", claude.displayName)
    assertEquals("Anthropic", claude.provider)
    assertTrue(claude.description.isNotBlank())

    val nemotron = getModelCardInfo("Nemotron")
    assertEquals("Nemotron 30B (Low Latency)", nemotron.displayName)
    assertEquals("NVIDIA NIM", nemotron.provider)

    val custom = getModelCardInfo("custom-endpoint/my-custom-model")
    assertEquals("custom-endpoint/my-custom-model", custom.displayName)
    assertEquals("Custom Endpoint", custom.provider)
    assertTrue(custom.description.isNotBlank())
  }
}
