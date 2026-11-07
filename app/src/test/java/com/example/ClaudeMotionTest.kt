package com.example

import com.example.data.model.MessageRole
import com.example.data.model.UiChatMessage
import com.example.data.model.ProviderConfig
import com.example.motion.engine.MotionAiGenerator
import com.example.motion.engine.MotionHtmlRenderer
import com.example.motion.model.MotionAspectRatio
import com.example.motion.model.MotionElement
import com.example.motion.model.MotionElementType
import com.example.motion.model.MotionProject
import com.example.motion.model.MotionScene
import com.example.motion.model.MotionStyle
import com.example.motion.model.SceneTransition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudeMotionTest {

  @Test
  fun testMotionHtmlRenderer_generatesValidHtmlWithControls() {
    val project = MotionProject(
      id = "test-proj-1",
      name = "Test Animation",
      aspectRatio = MotionAspectRatio.RATIO_16_9,
      durationSeconds = 6.0,
      style = MotionStyle.CINEMATIC,
      scenes = listOf(
        MotionScene(
          id = "sc-1",
          title = "Intro Scene",
          durationSeconds = 6.0,
          background = "#14151C",
          transition = SceneTransition.FADE,
          elements = listOf(
            MotionElement(
              id = "el-1",
              type = MotionElementType.TEXT,
              name = "Title",
              startTimeSeconds = 0.0,
              durationSeconds = 6.0,
              properties = mapOf("content" to "Hello World")
            )
          )
        )
      )
    )

    val html = MotionHtmlRenderer.generateExecutableHtml(project)
    assertNotNull(html)
    assertTrue("HTML must contain canvas element", html.contains("<canvas id=\"motionCanvas\""))
    assertTrue("HTML must contain window.ClaudeMotion API", html.contains("window.ClaudeMotion = {"))
    assertTrue("HTML must contain play method", html.contains("play: function()"))
    assertTrue("HTML must contain pause method", html.contains("pause: function()"))
    assertTrue("HTML must contain seek method", html.contains("seek: function(timeSec)"))
    assertTrue("HTML must reference project duration", html.contains("const TOTAL_DURATION = 6"))
  }

  @Test
  fun testMotionAiGenerator_proceduralFallbackCreatesValidProject() {
    val generator = MotionAiGenerator()
    val project = generator.buildProceduralProject(
      prompt = "Animated bar chart displaying quarterly revenue",
      style = MotionStyle.INFOGRAPHIC,
      aspectRatio = MotionAspectRatio.RATIO_16_9,
      duration = 8.0
    )

    assertNotNull(project)
    assertTrue(project.scenes.isNotEmpty())
    assertEquals(8.0, project.durationSeconds, 0.01)
    assertEquals(MotionStyle.INFOGRAPHIC, project.style)
    // Should have recognized chart prompt and created chart element
    val allElements = project.scenes.flatMap { it.elements }
    assertTrue("Should contain chart element", allElements.any { it.type == MotionElementType.CHART })
  }

  @Test
  fun testMotionAiGenerator_targetedEditsPreservesUnaffectedScenes() = runBlocking {
    val generator = MotionAiGenerator()
    val base = generator.buildProceduralProject(
      prompt = "Tech Showcase",
      style = MotionStyle.MODERN_MINIMAL,
      duration = 10.0
    )

    // 1. Test slowing down animation
    val slowerResult = generator.applyTargetedEdit(base, "Make the animation slower", ProviderConfig())
    assertTrue(slowerResult.isSuccess)
    val slower = slowerResult.getOrNull()!!
    assertTrue("Duration should increase", slower.durationSeconds > base.durationSeconds)

    // 2. Test changing background color
    val bgResult = generator.applyTargetedEdit(base, "Change background color to deep blue", ProviderConfig())
    assertTrue(bgResult.isSuccess)
    val blueBgProj = bgResult.getOrNull()!!
    assertTrue("Background should be dark blue", blueBgProj.scenes.first().background == "#0D1B2A")

    // 3. Test renaming title
    val renameResult = generator.applyTargetedEdit(base, "Change title to \"Future of AI\"", ProviderConfig())
    assertTrue(renameResult.isSuccess)
    val renamedProj = renameResult.getOrNull()!!
    assertEquals("Future of AI", renamedProj.name)
  }

  @Test
  fun testClaudeMotion_integratedInChatModel() {
    val project = MotionProject(
      id = "chat-motion-proj",
      name = "Square 1:1 Chat Preview",
      aspectRatio = MotionAspectRatio.RATIO_1_1,
      durationSeconds = 6.0,
      style = MotionStyle.CINEMATIC
    )

    val message = UiChatMessage(
      id = "msg-123",
      role = MessageRole.ASSISTANT,
      content = "Here is your generated animation.",
      motionProject = project,
      motionProjectId = project.id,
      isMotionGenerating = false
    )

    assertEquals(project.id, message.motionProjectId)
    assertEquals(MotionAspectRatio.RATIO_1_1, message.motionProject?.aspectRatio)
    assertFalse(message.isMotionGenerating)
    assertEquals("Square 1:1 Chat Preview", message.motionProject?.name)
  }

  @Test
  fun testMotionAspectRatios_calculations() {
    assertEquals(1f, MotionAspectRatio.RATIO_1_1.ratio, 0.001f)
    assertEquals(16f / 9f, MotionAspectRatio.RATIO_16_9.ratio, 0.001f)
    assertEquals(9f / 16f, MotionAspectRatio.RATIO_9_16.ratio, 0.001f)
    assertEquals(4f / 5f, MotionAspectRatio.RATIO_4_5.ratio, 0.001f)
  }
}
