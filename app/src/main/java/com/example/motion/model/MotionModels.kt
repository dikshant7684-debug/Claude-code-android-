package com.example.motion.model

/**
 * Aspect ratios supported by Claude Motion.
 */
enum class MotionAspectRatio(val label: String, val ratio: Float, val widthPx: Int, val heightPx: Int) {
  RATIO_16_9("16:9 (Landscape)", 16f / 9f, 1920, 1080),
  RATIO_9_16("9:16 (Story / Reel)", 9f / 16f, 1080, 1920),
  RATIO_1_1("1:1 (Square)", 1f, 1080, 1080),
  RATIO_4_5("4:5 (Portrait)", 4f / 5f, 1080, 1350);

  companion object {
    fun fromLabel(label: String): MotionAspectRatio {
      return entries.firstOrNull { it.label.startsWith(label) || it.name == label } ?: RATIO_16_9
    }
  }
}

/**
 * Creative visual animation style presets.
 */
enum class MotionStyle(val displayName: String, val description: String) {
  CINEMATIC("Cinematic", "Dramatic lighting, soft glows, dynamic camera easing and depth"),
  MODERN_MINIMAL("Minimalist Modern", "Clean typography, geometric forms, smooth spring physics"),
  INFOGRAPHIC("Infographic / Chart", "Data visualization, rising metric bars, animated counters"),
  NEON_CYBER("Neon / Cyber", "High contrast dark canvas, luminous strokes, holographic effects"),
  PLAYFUL("Playful / Bounce", "Bouncy bezier curves, vibrant pastel palettes, joyful motion"),
  KINETIC_TYPO("Kinetic Typography", "Staggered bold type reveals, sliding masks, expressive layout");

  companion object {
    fun fromName(name: String): MotionStyle {
      return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: MODERN_MINIMAL
    }
  }
}

/**
 * Element types that can appear inside a motion scene.
 */
enum class MotionElementType {
  TEXT,
  SHAPE,
  SVG,
  CHART,
  DIAGRAM,
  BACKGROUND,
  EFFECT
}

/**
 * Transition effect between scenes.
 */
enum class SceneTransition(val displayName: String) {
  FADE("Cross Fade"),
  SLIDE_LEFT("Slide Left"),
  SLIDE_RIGHT("Slide Right"),
  SLIDE_UP("Slide Up"),
  ZOOM_IN("Zoom In"),
  DISSOLVE("Dissolve")
}

/**
 * Individual animated visual element in a scene.
 */
data class MotionElement(
  val id: String,
  val type: MotionElementType,
  val name: String,
  val startTimeSeconds: Double,
  val durationSeconds: Double,
  val properties: Map<String, String> = emptyMap()
)

/**
 * Discrete scene inside an animation project.
 */
data class MotionScene(
  val id: String,
  val title: String,
  val durationSeconds: Double,
  val background: String = "#1A1A1A",
  val transition: SceneTransition = SceneTransition.FADE,
  val elements: List<MotionElement> = emptyList()
)

/**
 * Complete, structured motion animation project.
 */
data class MotionProject(
  val id: String,
  val name: String,
  val description: String = "",
  val aspectRatio: MotionAspectRatio = MotionAspectRatio.RATIO_16_9,
  val durationSeconds: Double = 8.0,
  val fps: Int = 60,
  val style: MotionStyle = MotionStyle.MODERN_MINIMAL,
  val scenes: List<MotionScene> = emptyList(),
  val executableHtml: String = "",
  val lastModified: Long = System.currentTimeMillis(),
  val version: Int = 1
)

/**
 * Generation lifecycle states for AI motion creation.
 */
enum class MotionGenerationStage(val label: String) {
  IDLE("Ready"),
  PLANNING("Planning motion scenes & storyboards..."),
  GENERATING_SCENES("Structuring visual elements & timing..."),
  WRITING_CODE("Writing executable Canvas / SVG animation code..."),
  VALIDATING("Validating syntax and keyframe constraints..."),
  RENDERING_PREVIEW("Rendering high-fps preview..."),
  READY("Animation ready."),
  ERROR("Generation failed.")
}

/**
 * Supported video and package export formats.
 */
enum class ExportFormat(val extension: String, val displayName: String, val mimeType: String) {
  MP4("mp4", "MP4 Video (.mp4)", "video/mp4"),
  WEBM("webm", "WebM Video (.webm)", "video/webm"),
  HTML_BUNDLE("html", "Standalone Interactive Package (.html)", "text/html")
}

/**
 * Configuration parameters for project export.
 */
data class ExportConfig(
  val format: ExportFormat = ExportFormat.MP4,
  val resolution: String = "1080p (Full HD)",
  val fps: Int = 60,
  val quality: String = "High"
)

/**
 * Diagnostic status for project operations.
 */
data class MotionOperationResult(
  val isSuccess: Boolean,
  val message: String,
  val filePath: String? = null,
  val fileSizeBytes: Long = 0
)
