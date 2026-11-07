package com.example.motion.data

import android.content.Context
import com.example.data.files.ProjectFileManager
import com.example.motion.engine.MotionHtmlRenderer
import com.example.motion.model.MotionAspectRatio
import com.example.motion.model.MotionElement
import com.example.motion.model.MotionElementType
import com.example.motion.model.MotionProject
import com.example.motion.model.MotionScene
import com.example.motion.model.MotionStyle
import com.example.motion.model.SceneTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Manages persistent storage and retrieval of Claude Motion projects on device.
 */
class MotionProjectRepository(
  private val context: Context,
  private val fileManager: ProjectFileManager
) {

  private val motionProjectsDir: File by lazy {
    File(fileManager.internalClaudeDir, "projects/motion").apply { mkdirs() }
  }

  private val _projects = MutableStateFlow<List<MotionProject>>(emptyList())
  val projects: StateFlow<List<MotionProject>> = _projects.asStateFlow()

  suspend fun initialize() = withContext(Dispatchers.IO) {
    loadProjectsFromDisk()
    if (_projects.value.isEmpty()) {
      seedStarterProjects()
    }
  }

  suspend fun loadProjectsFromDisk() = withContext(Dispatchers.IO) {
    try {
      val files = motionProjectsDir.listFiles { file -> file.isFile && file.extension == "json" } ?: emptyArray()
      val loaded = mutableListOf<MotionProject>()

      files.forEach { file ->
        try {
          val jsonStr = file.readText()
          val proj = deserializeProject(jsonStr)
          if (proj != null) {
            loaded.add(proj)
          }
        } catch (_: Exception) {}
      }

      val sorted = loaded.sortedByDescending { it.lastModified }
      _projects.value = sorted
    } catch (_: Exception) {}
  }

  suspend fun saveProject(project: MotionProject): Boolean = withContext(Dispatchers.IO) {
    try {
      val html = if (project.executableHtml.isNotBlank()) project.executableHtml else MotionHtmlRenderer.generateExecutableHtml(project)
      val updated = project.copy(
        executableHtml = html,
        lastModified = System.currentTimeMillis()
      )

      val file = File(motionProjectsDir, "${updated.id}.json")
      val json = serializeProject(updated)
      file.writeText(json)

      _projects.value = (listOf(updated) + _projects.value.filter { it.id != updated.id })
        .sortedByDescending { it.lastModified }
      true
    } catch (e: Exception) {
      false
    }
  }

  suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
    try {
      val file = File(motionProjectsDir, "$id.json")
      if (file.exists()) {
        file.delete()
      }
      _projects.value = _projects.value.filter { it.id != id }
      true
    } catch (_: Exception) {
      false
    }
  }

  suspend fun duplicateProject(id: String): MotionProject? = withContext(Dispatchers.IO) {
    val existing = _projects.value.firstOrNull { it.id == id } ?: return@withContext null
    val newId = UUID.randomUUID().toString()
    val cloned = existing.copy(
      id = newId,
      name = "${existing.name} (Copy)",
      lastModified = System.currentTimeMillis(),
      version = 1
    )
    saveProject(cloned)
    cloned
  }

  private suspend fun seedStarterProjects() {
    val starters = listOf(
      createStarterKineticReveal(),
      createStarterChartExplainer(),
      createStarterCyberNeon()
    )
    starters.forEach { saveProject(it) }
  }

  private fun createStarterKineticReveal(): MotionProject {
    val scene1 = MotionScene(
      id = "scene-intro-1",
      title = "Logo Reveal",
      durationSeconds = 4.0,
      background = "#13141C",
      transition = SceneTransition.FADE,
      elements = listOf(
        MotionElement(
          id = "el-1",
          type = MotionElementType.SHAPE,
          name = "Kinetic Ring",
          startTimeSeconds = 0.0,
          durationSeconds = 4.0,
          properties = mapOf("radius" to "90")
        ),
        MotionElement(
          id = "el-2",
          type = MotionElementType.TEXT,
          name = "Brand Title",
          startTimeSeconds = 0.4,
          durationSeconds = 3.6,
          properties = mapOf("content" to "Claude Motion", "fontSize" to "64", "y" to "460")
        )
      )
    )

    val scene2 = MotionScene(
      id = "scene-intro-2",
      title = "Feature Overview",
      durationSeconds = 4.0,
      background = "#1A1722",
      transition = SceneTransition.ZOOM_IN,
      elements = listOf(
        MotionElement(
          id = "el-3",
          type = MotionElementType.TEXT,
          name = "Subtitle",
          startTimeSeconds = 0.2,
          durationSeconds = 3.8,
          properties = mapOf("content" to "Autonomous AI Motion Graphics", "fontSize" to "40", "y" to "520", "color" to "#D95B26")
        )
      )
    )

    return MotionProject(
      id = "starter-kinetic-reveal",
      name = "Claude Motion Kinetic Reveal",
      description = "Smooth geometric easing with warm terracotta typography.",
      aspectRatio = MotionAspectRatio.RATIO_16_9,
      durationSeconds = 8.0,
      style = MotionStyle.CINEMATIC,
      scenes = listOf(scene1, scene2)
    )
  }

  private fun createStarterChartExplainer(): MotionProject {
    val scene1 = MotionScene(
      id = "scene-chart-1",
      title = "Metric Explainer",
      durationSeconds = 7.0,
      background = "#111420",
      transition = SceneTransition.SLIDE_UP,
      elements = listOf(
        MotionElement(
          id = "el-c1",
          type = MotionElementType.TEXT,
          name = "Chart Header",
          startTimeSeconds = 0.0,
          durationSeconds = 7.0,
          properties = mapOf("content" to "AI Model Throughput & Latency", "fontSize" to "50", "y" to "160")
        ),
        MotionElement(
          id = "el-c2",
          type = MotionElementType.CHART,
          name = "Latency Comparison",
          startTimeSeconds = 0.3,
          durationSeconds = 6.7,
          properties = mapOf("bars" to "6")
        )
      )
    )

    return MotionProject(
      id = "starter-chart-explainer",
      name = "AI Performance Metrics Chart",
      description = "Dynamic data visualization with animated rising bar series.",
      aspectRatio = MotionAspectRatio.RATIO_16_9,
      durationSeconds = 7.0,
      style = MotionStyle.INFOGRAPHIC,
      scenes = listOf(scene1)
    )
  }

  private fun createStarterCyberNeon(): MotionProject {
    val scene1 = MotionScene(
      id = "scene-neon-1",
      title = "Cyberpunk Pulse",
      durationSeconds = 6.0,
      background = "#07080D",
      transition = SceneTransition.DISSOLVE,
      elements = listOf(
        MotionElement(
          id = "el-n1",
          type = MotionElementType.TEXT,
          name = "Neon Headline",
          startTimeSeconds = 0.0,
          durationSeconds = 6.0,
          properties = mapOf("content" to "CYBERPUNK 2088", "fontSize" to "68", "color" to "#00E5FF", "y" to "480")
        )
      )
    )

    return MotionProject(
      id = "starter-cyber-neon",
      name = "Cyberpunk Neon Promo",
      description = "High-contrast glowing typography with ambient particles.",
      aspectRatio = MotionAspectRatio.RATIO_9_16,
      durationSeconds = 6.0,
      style = MotionStyle.NEON_CYBER,
      scenes = listOf(scene1)
    )
  }

  private fun serializeProject(project: MotionProject): String {
    return JSONObject().apply {
      put("id", project.id)
      put("name", project.name)
      put("description", project.description)
      put("aspectRatio", project.aspectRatio.name)
      put("durationSeconds", project.durationSeconds)
      put("fps", project.fps)
      put("style", project.style.name)
      put("version", project.version)
      put("lastModified", project.lastModified)
      put("executableHtml", project.executableHtml)

      val scenesArray = JSONArray()
      project.scenes.forEach { sc ->
        scenesArray.put(JSONObject().apply {
          put("id", sc.id)
          put("title", sc.title)
          put("durationSeconds", sc.durationSeconds)
          put("background", sc.background)
          put("transition", sc.transition.name)

          val elArray = JSONArray()
          sc.elements.forEach { el ->
            elArray.put(JSONObject().apply {
              put("id", el.id)
              put("type", el.type.name)
              put("name", el.name)
              put("startTimeSeconds", el.startTimeSeconds)
              put("durationSeconds", el.durationSeconds)
              val pObj = JSONObject()
              el.properties.forEach { (k, v) -> pObj.put(k, v) }
              put("properties", pObj)
            })
          }
          put("elements", elArray)
        })
      }
      put("scenes", scenesArray)
    }.toString(2)
  }

  private fun deserializeProject(jsonStr: String): MotionProject? {
    try {
      val obj = JSONObject(jsonStr)
      val id = obj.getString("id")
      val name = obj.getString("name")
      val desc = obj.optString("description", "")
      val ar = try { MotionAspectRatio.valueOf(obj.optString("aspectRatio", "RATIO_16_9")) } catch (_: Exception) { MotionAspectRatio.RATIO_16_9 }
      val dur = obj.optDouble("durationSeconds", 8.0)
      val fps = obj.optInt("fps", 60)
      val style = try { MotionStyle.valueOf(obj.optString("style", "MODERN_MINIMAL")) } catch (_: Exception) { MotionStyle.MODERN_MINIMAL }
      val ver = obj.optInt("version", 1)
      val modified = obj.optLong("lastModified", System.currentTimeMillis())
      val html = obj.optString("executableHtml", "")

      val scenesList = mutableListOf<MotionScene>()
      val scenesArray = obj.optJSONArray("scenes")
      if (scenesArray != null) {
        for (i in 0 until scenesArray.length()) {
          val sObj = scenesArray.getJSONObject(i)
          val sId = sObj.optString("id", UUID.randomUUID().toString())
          val sTitle = sObj.optString("title", "Scene")
          val sDur = sObj.optDouble("durationSeconds", dur)
          val sBg = sObj.optString("background", "#141620")
          val sTrans = try { SceneTransition.valueOf(sObj.optString("transition", "FADE")) } catch (_: Exception) { SceneTransition.FADE }

          val elList = mutableListOf<MotionElement>()
          val elArray = sObj.optJSONArray("elements")
          if (elArray != null) {
            for (j in 0 until elArray.length()) {
              val elObj = elArray.getJSONObject(j)
              val elId = elObj.optString("id", UUID.randomUUID().toString())
              val elType = try { MotionElementType.valueOf(elObj.optString("type", "TEXT")) } catch (_: Exception) { MotionElementType.TEXT }
              val elName = elObj.optString("name", "Element")
              val elStart = elObj.optDouble("startTimeSeconds", 0.0)
              val elDur = elObj.optDouble("durationSeconds", sDur)
              val pMap = mutableMapOf<String, String>()
              val pObj = elObj.optJSONObject("properties")
              pObj?.keys()?.forEach { k -> pMap[k] = pObj.optString(k) }

              elList.add(MotionElement(elId, elType, elName, elStart, elDur, pMap))
            }
          }
          scenesList.add(MotionScene(sId, sTitle, sDur, sBg, sTrans, elList))
        }
      }

      return MotionProject(
        id = id,
        name = name,
        description = desc,
        aspectRatio = ar,
        durationSeconds = dur,
        fps = fps,
        style = style,
        scenes = scenesList,
        executableHtml = html,
        lastModified = modified,
        version = ver
      )
    } catch (_: Exception) {
      return null
    }
  }
}
