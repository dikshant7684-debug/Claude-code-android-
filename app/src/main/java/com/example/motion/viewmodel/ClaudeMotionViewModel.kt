package com.example.motion.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.files.ProjectFileManager
import com.example.data.model.ProviderConfig
import com.example.motion.data.MotionProjectRepository
import com.example.motion.engine.MotionAiGenerator
import com.example.motion.engine.MotionExporter
import com.example.motion.engine.MotionHtmlRenderer
import com.example.motion.model.ExportConfig
import com.example.motion.model.MotionAspectRatio
import com.example.motion.model.MotionGenerationStage
import com.example.motion.model.MotionOperationResult
import com.example.motion.model.MotionProject
import com.example.motion.model.MotionStyle
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing state, playback, AI generation, project lifecycle,
 * and exports for Claude Motion.
 */
class ClaudeMotionViewModel(
  application: Application,
  private val mainViewModel: MainViewModel? = null
) : AndroidViewModel(application) {

  private val fileManager = ProjectFileManager(application)
  val repository = MotionProjectRepository(application, fileManager)
  private val aiGenerator = MotionAiGenerator(mainViewModel?.aiClient ?: com.example.data.ai.AiProviderClient())
  private val exporter = MotionExporter(application, fileManager)

  val projects: StateFlow<List<MotionProject>> = repository.projects

  private val _currentProject = MutableStateFlow<MotionProject?>(null)
  val currentProject: StateFlow<MotionProject?> = _currentProject.asStateFlow()

  private val _generationStage = MutableStateFlow(MotionGenerationStage.IDLE)
  val generationStage: StateFlow<MotionGenerationStage> = _generationStage.asStateFlow()

  private val _stageMessage = MutableStateFlow("Ready")
  val stageMessage: StateFlow<String> = _stageMessage.asStateFlow()

  private val _isPlaying = MutableStateFlow(true)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _currentTimeSeconds = MutableStateFlow(0.0)
  val currentTimeSeconds: StateFlow<Double> = _currentTimeSeconds.asStateFlow()

  private val _durationSeconds = MutableStateFlow(8.0)
  val durationSeconds: StateFlow<Double> = _durationSeconds.asStateFlow()

  private val _selectedAspectRatio = MutableStateFlow(MotionAspectRatio.RATIO_16_9)
  val selectedAspectRatio: StateFlow<MotionAspectRatio> = _selectedAspectRatio.asStateFlow()

  private val _selectedStyle = MutableStateFlow(MotionStyle.CINEMATIC)
  val selectedStyle: StateFlow<MotionStyle> = _selectedStyle.asStateFlow()

  private val _promptInput = MutableStateFlow("")
  val promptInput: StateFlow<String> = _promptInput.asStateFlow()

  private val _isExporting = MutableStateFlow(false)
  val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

  private val _exportProgress = MutableStateFlow(0f)
  val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

  private val _lastExportResult = MutableStateFlow<MotionOperationResult?>(null)
  val lastExportResult: StateFlow<MotionOperationResult?> = _lastExportResult.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  init {
    viewModelScope.launch {
      repository.initialize()
      if (repository.projects.value.isNotEmpty()) {
        val first = repository.projects.value.first()
        selectProject(first)
      } else {
        createNewProject()
      }
    }
  }

  fun setPromptInput(text: String) {
    _promptInput.value = text
  }

  fun setAspectRatio(ratio: MotionAspectRatio) {
    _selectedAspectRatio.value = ratio
    _currentProject.value?.let { proj ->
      val updated = proj.copy(
        aspectRatio = ratio,
        executableHtml = MotionHtmlRenderer.generateExecutableHtml(proj.copy(aspectRatio = ratio))
      )
      _currentProject.value = updated
      viewModelScope.launch { repository.saveProject(updated) }
    }
  }

  fun setStyle(style: MotionStyle) {
    _selectedStyle.value = style
  }

  fun createNewProject() {
    val newId = UUID.randomUUID().toString()
    val newProj = aiGenerator.buildProceduralProject(
      prompt = "New Motion Project",
      style = _selectedStyle.value,
      aspectRatio = _selectedAspectRatio.value,
      duration = 8.0
    ).copy(id = newId, name = "Untitled Animation")

    val withHtml = newProj.copy(executableHtml = MotionHtmlRenderer.generateExecutableHtml(newProj))
    _currentProject.value = withHtml
    _currentTimeSeconds.value = 0.0
    _durationSeconds.value = withHtml.durationSeconds
    _selectedAspectRatio.value = withHtml.aspectRatio
    _selectedStyle.value = withHtml.style
    _isPlaying.value = true

    viewModelScope.launch {
      repository.saveProject(withHtml)
    }
  }

  fun selectProject(project: MotionProject) {
    val html = if (project.executableHtml.isNotBlank()) project.executableHtml else MotionHtmlRenderer.generateExecutableHtml(project)
    val ready = project.copy(executableHtml = html)
    _currentProject.value = ready
    _durationSeconds.value = ready.durationSeconds
    _currentTimeSeconds.value = 0.0
    _selectedAspectRatio.value = ready.aspectRatio
    _selectedStyle.value = ready.style
    _isPlaying.value = true
  }

  fun renameProject(newName: String) {
    val proj = _currentProject.value ?: return
    val clean = newName.trim()
    if (clean.isBlank()) return
    val updated = proj.copy(
      name = clean,
      executableHtml = MotionHtmlRenderer.generateExecutableHtml(proj.copy(name = clean))
    )
    _currentProject.value = updated
    viewModelScope.launch {
      repository.saveProject(updated)
    }
  }

  fun deleteCurrentProject() {
    val proj = _currentProject.value ?: return
    deleteProject(proj.id)
  }

  fun deleteProject(id: String) {
    viewModelScope.launch {
      repository.deleteProject(id)
      if (_currentProject.value?.id == id) {
        val remaining = repository.projects.value.filter { it.id != id }
        if (remaining.isNotEmpty()) {
          selectProject(remaining.first())
        } else {
          createNewProject()
        }
      }
    }
  }

  fun duplicateCurrentProject() {
    val proj = _currentProject.value ?: return
    viewModelScope.launch {
      val cloned = repository.duplicateProject(proj.id)
      if (cloned != null) {
        selectProject(cloned)
      }
    }
  }

  fun generateAnimation() {
    val prompt = _promptInput.value.trim()
    if (prompt.isBlank()) return

    val config = mainViewModel?.providerConfig?.value ?: ProviderConfig()

    viewModelScope.launch {
      _errorMessage.value = null
      val result = aiGenerator.generateAnimation(
        prompt = prompt,
        style = _selectedStyle.value,
        aspectRatio = _selectedAspectRatio.value,
        targetDurationSeconds = _durationSeconds.value,
        config = config,
        onStageChanged = { stage, msg ->
          _generationStage.value = stage
          _stageMessage.value = msg
        }
      )

      if (result.isSuccess) {
        val proj = result.getOrNull()!!
        _currentProject.value = proj
        _durationSeconds.value = proj.durationSeconds
        _currentTimeSeconds.value = 0.0
        _isPlaying.value = true
        _promptInput.value = ""
        repository.saveProject(proj)
      } else {
        _errorMessage.value = result.exceptionOrNull()?.message ?: "Animation generation failed."
        _generationStage.value = MotionGenerationStage.IDLE
      }
    }
  }

  fun applyTargetedEdit(instruction: String) {
    val proj = _currentProject.value ?: return
    val clean = instruction.trim()
    if (clean.isBlank()) return

    val config = mainViewModel?.providerConfig?.value ?: ProviderConfig()

    viewModelScope.launch {
      _errorMessage.value = null
      val result = aiGenerator.applyTargetedEdit(
        currentProject = proj,
        instruction = clean,
        config = config,
        onStageChanged = { stage, msg ->
          _generationStage.value = stage
          _stageMessage.value = msg
        }
      )

      if (result.isSuccess) {
        val updated = result.getOrNull()!!
        _currentProject.value = updated
        _durationSeconds.value = updated.durationSeconds
        _isPlaying.value = true
        repository.saveProject(updated)
      } else {
        _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to apply edit."
        _generationStage.value = MotionGenerationStage.IDLE
      }
    }
  }

  fun togglePlay() {
    _isPlaying.value = !_isPlaying.value
  }

  fun play() {
    _isPlaying.value = true
  }

  fun pause() {
    _isPlaying.value = false
  }

  fun replay() {
    _currentTimeSeconds.value = 0.0
    _isPlaying.value = true
  }

  fun seekTo(seconds: Double) {
    val clamped = seconds.coerceIn(0.0, _durationSeconds.value)
    _currentTimeSeconds.value = clamped
  }

  fun onTimeUpdateFromBridge(time: Double, duration: Double) {
    _currentTimeSeconds.value = time
    if (duration > 0 && duration != _durationSeconds.value) {
      _durationSeconds.value = duration
    }
  }

  fun exportCurrentProject(config: ExportConfig) {
    val proj = _currentProject.value ?: return
    viewModelScope.launch {
      _isExporting.value = true
      _exportProgress.value = 0.0f
      _lastExportResult.value = null

      val result = exporter.exportProject(
        project = proj,
        config = config,
        onProgress = { p -> _exportProgress.value = p }
      )

      _lastExportResult.value = result
      _isExporting.value = false
    }
  }

  fun dismissExportResult() {
    _lastExportResult.value = null
  }

  fun dismissError() {
    _errorMessage.value = null
  }
}
