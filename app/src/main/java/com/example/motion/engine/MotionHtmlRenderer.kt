package com.example.motion.engine

import com.example.motion.model.MotionAspectRatio
import com.example.motion.model.MotionProject
import com.example.motion.model.MotionStyle

/**
 * MotionHtmlRenderer generates self-contained, isolated, high-performance
 * HTML5 Canvas & CSS animation packages executable in Android WebView.
 */
object MotionHtmlRenderer {

  /**
   * Generates a complete, executable HTML5 animation bundle from a structured MotionProject.
   */
  fun generateExecutableHtml(project: MotionProject): String {
    val durationSec = project.durationSeconds.coerceAtLeast(1.0)
    val width = project.aspectRatio.widthPx
    val height = project.aspectRatio.heightPx
    val title = project.name.replace("\"", "\\\"")
    val style = project.style

    // Serialize scenes for javascript player using pure Kotlin
    val scenesJson = project.scenes.joinToString(prefix = "[", postfix = "]") { scene ->
      val elementsJson = scene.elements.joinToString(prefix = "[", postfix = "]") { el ->
        val propsJson = el.properties.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
          "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        """{"id":"${escapeJson(el.id)}","type":"${el.type.name}","name":"${escapeJson(el.name)}","startTime":${el.startTimeSeconds},"duration":${el.durationSeconds},"properties":$propsJson}"""
      }
      """{"id":"${escapeJson(scene.id)}","title":"${escapeJson(scene.title)}","duration":${scene.durationSeconds},"background":"${escapeJson(scene.background)}","transition":"${scene.transition.name}","elements":$elementsJson}"""
    }

    return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>$title</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; -webkit-user-select: none; }
  body, html {
    width: 100%;
    height: 100%;
    overflow: hidden;
    background-color: #0d0e12;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  }
  #motion-container {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  canvas {
    display: block;
    max-width: 100%;
    max-height: 100%;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
    background-color: #121318;
  }
</style>
</head>
<body>
<div id="motion-container">
  <canvas id="motionCanvas" width="$width" height="$height"></canvas>
</div>

<script>
(function() {
  const canvas = document.getElementById('motionCanvas');
  const ctx = canvas.getContext('2d');
  const W = $width;
  const H = $height;
  const TOTAL_DURATION = $durationSec;
  const STYLE = '${style.name}';
  const SCENES = $scenesJson;

  let currentTime = 0;
  let isPlaying = true;
  let isLooping = true;
  let playbackRate = 1.0;
  let lastTimestamp = performance.now();

  // Particle systems & background motion states
  const particles = [];
  for (let i = 0; i < 40; i++) {
    particles.push({
      x: Math.random() * W,
      y: Math.random() * H,
      radius: 1.5 + Math.random() * 3,
      vx: (Math.random() - 0.5) * 40,
      vy: (Math.random() - 0.5) * 40,
      alpha: 0.2 + Math.random() * 0.5,
      hue: Math.random() * 60 + 10 // Terracotta to amber
    });
  }

  function getActiveScene(t) {
    if (!SCENES || SCENES.length === 0) return null;
    let accumulated = 0;
    for (let i = 0; i < SCENES.length; i++) {
      const scene = SCENES[i];
      if (t >= accumulated && t <= accumulated + scene.duration) {
        return { scene, sceneIndex: i, localTime: t - accumulated };
      }
      accumulated += scene.duration;
    }
    return { scene: SCENES[SCENES.length - 1], sceneIndex: SCENES.length - 1, localTime: SCENES[SCENES.length - 1].duration };
  }

  function easeOutCubic(x) {
    return 1 - Math.pow(1 - x, 3);
  }

  function easeInOutQuad(x) {
    return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
  }

  function renderFrame(t) {
    ctx.clearRect(0, 0, W, H);

    const active = getActiveScene(t);
    const scene = active ? active.scene : null;
    const localT = active ? active.localTime : t;
    const progress = Math.min(1.0, Math.max(0.0, t / TOTAL_DURATION));

    // 1. Draw Scene Background with subtle dynamic gradient
    let bg = (scene && scene.background) ? scene.background : '#12131A';
    const grad = ctx.createRadialGradient(W * 0.5, H * 0.4, W * 0.1, W * 0.5, H * 0.5, W * 0.8);
    grad.addColorStop(0, '#1E202B');
    grad.addColorStop(1, bg);
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, W, H);

    // 2. Ambient Floating Particles
    ctx.save();
    for (let p of particles) {
      const px = (p.x + p.vx * t + W) % W;
      const py = (p.y + p.vy * t + H) % H;
      ctx.fillStyle = 'hsla(' + p.hue + ', 80%, 65%, ' + (p.alpha * 0.4) + ')';
      ctx.beginPath();
      ctx.arc(px, py, p.radius, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();

    // 3. Render Custom Elements from Scene
    if (scene && scene.elements && scene.elements.length > 0) {
      for (let el of scene.elements) {
        const elStart = el.startTime || 0;
        const elDuration = el.duration || scene.duration;
        if (localT >= elStart && localT <= elStart + elDuration + 0.1) {
          const elProgress = Math.min(1.0, Math.max(0.0, (localT - elStart) / elDuration));
          renderElement(el, elProgress, localT);
        }
      }
    } else {
      // Procedural fallback animation when elements are empty
      renderDefaultProcedural(t, progress);
    }

    // 4. Subtle Vignette
    const vig = ctx.createRadialGradient(W * 0.5, H * 0.5, W * 0.4, W * 0.5, H * 0.5, W * 0.75);
    vig.addColorStop(0, 'rgba(0,0,0,0)');
    vig.addColorStop(1, 'rgba(0,0,0,0.55)');
    ctx.fillStyle = vig;
    ctx.fillRect(0, 0, W, H);

    // 5. Minimal sleek corner watermark
    ctx.fillStyle = 'rgba(255, 255, 255, 0.25)';
    ctx.font = '600 ' + Math.round(W * 0.016) + 'px sans-serif';
    ctx.fillText('CLAUDE MOTION', W * 0.04, H * 0.06);
  }

  function renderElement(el, progress, localT) {
    const props = el.properties || {};
    const eased = easeOutCubic(progress);

    ctx.save();
    if (el.type === 'TEXT') {
      const text = props.content || el.name || 'Motion Title';
      const size = parseInt(props.fontSize || Math.round(W * 0.045));
      const color = props.color || '#FFFFFF';
      const yOffset = (1 - eased) * 50;

      ctx.globalAlpha = Math.min(1.0, eased * 1.5);
      ctx.fillStyle = color;
      ctx.font = '700 ' + size + 'px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';

      const x = props.x ? parseInt(props.x) : W * 0.5;
      const y = props.y ? parseInt(props.y) : H * 0.5;

      // Glow effect for neon / cinematic
      if (STYLE === 'NEON_CYBER' || STYLE === 'CINEMATIC') {
        ctx.shadowColor = '#D95B26';
        ctx.shadowBlur = 24 * eased;
      }
      ctx.fillText(text, x, y + yOffset);
    } else if (el.type === 'CHART') {
      // Dynamic rising animated data chart bars
      const barCount = 6;
      const chartW = W * 0.6;
      const chartH = H * 0.35;
      const startX = (W - chartW) / 2;
      const bottomY = H * 0.68;
      const barW = chartW / (barCount * 1.6);
      const values = [0.45, 0.72, 0.55, 0.88, 0.64, 0.95];

      ctx.strokeStyle = 'rgba(255,255,255,0.15)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(startX - 20, bottomY);
      ctx.lineTo(startX + chartW + 20, bottomY);
      ctx.stroke();

      for (let i = 0; i < barCount; i++) {
        const barX = startX + i * (barW * 1.6) + barW * 0.3;
        const targetH = chartH * values[i];
        const currentH = targetH * Math.min(1.0, eased * (1.0 + i * 0.15));

        const barGrad = ctx.createLinearGradient(barX, bottomY, barX, bottomY - currentH);
        barGrad.addColorStop(0, '#D95B26');
        barGrad.addColorStop(1, '#FFAB91');
        ctx.fillStyle = barGrad;

        ctx.fillRect(barX, bottomY - currentH, barW, currentH);

        // Value text above bar
        if (progress > 0.4) {
          ctx.fillStyle = '#FFFFFF';
          ctx.font = '600 ' + Math.round(W * 0.018) + 'px sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText(Math.round(values[i] * 100 * eased) + '%', barX + barW / 2, bottomY - currentH - 12);
        }
      }
    } else if (el.type === 'SHAPE' || el.type === 'SVG') {
      // Animated rotating geometric motif / badge
      const cx = props.x ? parseInt(props.x) : W * 0.5;
      const cy = props.y ? parseInt(props.y) : H * 0.35;
      const radius = (props.radius ? parseInt(props.radius) : W * 0.08) * eased;

      ctx.translate(cx, cy);
      ctx.rotate(localT * 0.8);
      ctx.strokeStyle = '#D95B26';
      ctx.lineWidth = 6;
      ctx.beginPath();
      for (let i = 0; i < 6; i++) {
        const angle = (i * Math.PI) / 3;
        const x = radius * Math.cos(angle);
        const y = radius * Math.sin(angle);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.stroke();

      // Inner glowing core
      ctx.fillStyle = 'rgba(217, 91, 38, 0.25)';
      ctx.fill();
    }
    ctx.restore();
  }

  function renderDefaultProcedural(t, progress) {
    const eased = easeOutCubic(progress);

    // Glowing geometric central ring
    ctx.save();
    ctx.translate(W * 0.5, H * 0.42);
    ctx.rotate(t * 0.5);

    ctx.strokeStyle = '#D95B26';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.arc(0, 0, Math.min(W, H) * 0.16 * eased, 0, Math.PI * 2);
    ctx.stroke();

    ctx.strokeStyle = 'rgba(255, 171, 145, 0.4)';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(0, 0, Math.min(W, H) * 0.22 * eased, 0, Math.PI * 2);
    ctx.stroke();
    ctx.restore();

    // Bold title text
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '800 ' + Math.round(W * 0.055) + 'px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('$title', W * 0.5, H * 0.72);

    // Subtitle
    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.font = '400 ' + Math.round(W * 0.025) + 'px sans-serif';
    ctx.fillText('${style.displayName} • Generated with Claude Code', W * 0.5, H * 0.80);
  }

  function loop(timestamp) {
    const delta = (timestamp - lastTimestamp) / 1000;
    lastTimestamp = timestamp;

    if (isPlaying) {
      currentTime += delta * playbackRate;
      if (currentTime >= TOTAL_DURATION) {
        if (isLooping) {
          currentTime = 0;
        } else {
          currentTime = TOTAL_DURATION;
          isPlaying = false;
          if (window.AndroidMotionBridge && window.AndroidMotionBridge.onPlaybackEnded) {
            window.AndroidMotionBridge.onPlaybackEnded();
          }
        }
      }
      notifyBridge();
    }

    renderFrame(currentTime);
    requestAnimationFrame(loop);
  }

  function notifyBridge() {
    if (window.AndroidMotionBridge && window.AndroidMotionBridge.onTimeUpdate) {
      window.AndroidMotionBridge.onTimeUpdate(currentTime, TOTAL_DURATION);
    }
  }

  // Exposed Public Bridge API for Android
  window.ClaudeMotion = {
    play: function() {
      isPlaying = true;
      lastTimestamp = performance.now();
    },
    pause: function() {
      isPlaying = false;
    },
    togglePlay: function() {
      isPlaying = !isPlaying;
      if (isPlaying) lastTimestamp = performance.now();
      return isPlaying;
    },
    replay: function() {
      currentTime = 0;
      isPlaying = true;
      lastTimestamp = performance.now();
    },
    seek: function(timeSec) {
      currentTime = Math.max(0, Math.min(TOTAL_DURATION, timeSec));
      renderFrame(currentTime);
      notifyBridge();
    },
    setPlaybackRate: function(rate) {
      playbackRate = Math.max(0.25, Math.min(4.0, rate));
    },
    setLoop: function(loop) {
      isLooping = Boolean(loop);
    },
    getCurrentTime: function() {
      return currentTime;
    },
    getDuration: function() {
      return TOTAL_DURATION;
    },
    isPlaying: function() {
      return isPlaying;
    }
  };

  requestAnimationFrame(loop);
})();
</script>
</body>
</html>
    """.trimIndent()
  }

  private fun escapeJson(value: String): String {
    return value.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")
  }
}
