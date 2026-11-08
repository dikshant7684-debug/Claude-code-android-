package com.example.ui.system;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * High-performance native Android View rendering an animated AI Intelligence Core.
 * Features rotating orbital rings, energy particles, breathing pulsation, and
 * reactive state transitions optimized for low-end to high-end devices.
 */
public class ThinkingOrbView extends View {

  public enum State {
    IDLE,
    THINKING,
    SEARCHING,
    CODING,
    TOOL_EXECUTION,
    ERROR,
    COMPLETED
  }

  private State currentState = State.IDLE;
  private float rotationAngle = 0f;
  private float pulseProgress = 0f;
  private ValueAnimator animator;

  private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private final List<Particle> particles = new ArrayList<>();
  private final Random random = new Random();

  // Nebula Purple & Charcoal Color Constants
  private static final int COLOR_PRIMARY_PURPLE = Color.parseColor("#8B5CF6");
  private static final int COLOR_ACCENT_VIOLET = Color.parseColor("#A78BFA");
  private static final int COLOR_LAVENDER = Color.parseColor("#C4B5FD");
  private static final int COLOR_CYAN_ACCENT = Color.parseColor("#38BDF8");
  private static final int COLOR_ERROR_RED = Color.parseColor("#F87171");
  private static final int COLOR_SUCCESS_GREEN = Color.parseColor("#4ADE80");

  private static class Particle {
    float angle;
    float distance;
    float speed;
    float size;
    int color;
    float alpha;
  }

  public ThinkingOrbView(Context context) {
    super(context);
    init();
  }

  public ThinkingOrbView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ThinkingOrbView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    ringPaint.setStyle(Paint.Style.STROKE);
    ringPaint.setStrokeWidth(2.5f);

    particlePaint.setStyle(Paint.Style.FILL);

    // Initialize 24 reactive energy particles
    particles.clear();
    for (int i = 0; i < 24; i++) {
      Particle p = new Particle();
      p.angle = random.nextFloat() * 360f;
      p.distance = 0.3f + random.nextFloat() * 0.65f;
      p.speed = 0.8f + random.nextFloat() * 2.2f;
      p.size = 2f + random.nextFloat() * 3.5f;
      p.color = (i % 3 == 0) ? COLOR_ACCENT_VIOLET : (i % 3 == 1) ? COLOR_PRIMARY_PURPLE : COLOR_LAVENDER;
      p.alpha = 0.4f + random.nextFloat() * 0.6f;
      particles.add(p);
    }

    startAnimation();
  }

  public void setState(State state) {
    if (this.currentState != state) {
      this.currentState = state;
      invalidate();
    }
  }

  public State getState() {
    return currentState;
  }

  private void startAnimation() {
    if (animator != null && animator.isRunning()) {
      animator.cancel();
    }
    animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(4000);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setInterpolator(new LinearInterpolator());
    animator.addUpdateListener(animation -> {
      float val = animation.getAnimatedFraction();
      float speedFactor = getSpeedFactor();
      rotationAngle = (rotationAngle + (1.2f * speedFactor)) % 360f;
      pulseProgress = (float) Math.sin(val * Math.PI * 2);
      invalidate();
    });
    animator.start();
  }

  private float getSpeedFactor() {
    switch (currentState) {
      case SEARCHING:
        return 3.0f;
      case CODING:
        return 2.5f;
      case TOOL_EXECUTION:
        return 3.5f;
      case THINKING:
        return 1.8f;
      case ERROR:
        return 0.8f;
      case COMPLETED:
        return 0.5f;
      case IDLE:
      default:
        return 1.0f;
    }
  }

  private int getMainOrbColor() {
    switch (currentState) {
      case ERROR:
        return COLOR_ERROR_RED;
      case COMPLETED:
        return COLOR_SUCCESS_GREEN;
      case SEARCHING:
        return COLOR_CYAN_ACCENT;
      case CODING:
        return COLOR_ACCENT_VIOLET;
      case TOOL_EXECUTION:
      case THINKING:
      case IDLE:
      default:
        return COLOR_PRIMARY_PURPLE;
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    float cx = w / 2f;
    float cy = h / 2f;
    float maxRadius = Math.min(cx, cy) * 0.85f;

    int orbColor = getMainOrbColor();

    // 1. Ambient Glow Background
    float glowRadius = maxRadius * (0.85f + 0.15f * pulseProgress);
    RadialGradient glowGradient = new RadialGradient(
        cx, cy, Math.max(1f, glowRadius),
        new int[]{Color.argb(80, Color.red(orbColor), Color.green(orbColor), Color.blue(orbColor)), Color.TRANSPARENT},
        new float[]{0f, 1f},
        Shader.TileMode.CLAMP
    );
    glowPaint.setShader(glowGradient);
    canvas.drawCircle(cx, cy, glowRadius, glowPaint);

    // 2. Rotating Orbital Rings
    drawOrbitalRings(canvas, cx, cy, maxRadius, orbColor);

    // 3. Energy Particles
    drawParticles(canvas, cx, cy, maxRadius);

    // 4. Central Nebula Core Orb
    float coreRadius = maxRadius * 0.35f * (1.0f + 0.08f * pulseProgress);
    RadialGradient coreGradient = new RadialGradient(
        cx - coreRadius * 0.25f, cy - coreRadius * 0.25f, Math.max(1f, coreRadius * 1.2f),
        new int[]{COLOR_LAVENDER, orbColor, Color.argb(220, 20, 19, 26)},
        new float[]{0f, 0.6f, 1f},
        Shader.TileMode.CLAMP
    );
    corePaint.setShader(coreGradient);
    canvas.drawCircle(cx, cy, coreRadius, corePaint);
  }

  private void drawOrbitalRings(Canvas canvas, float cx, float cy, float maxRadius, int orbColor) {
    // Ring 1 (Inner, Clockwise)
    canvas.save();
    canvas.rotate(rotationAngle, cx, cy);
    ringPaint.setColor(Color.argb(140, Color.red(orbColor), Color.green(orbColor), Color.blue(orbColor)));
    float ring1Radius = maxRadius * 0.58f;
    canvas.drawCircle(cx, cy, ring1Radius, ringPaint);
    // Node indicator on ring 1
    canvas.drawCircle(cx + ring1Radius, cy, 3.5f, particlePaint);
    canvas.restore();

    // Ring 2 (Outer, Counter-Clockwise tilted)
    canvas.save();
    canvas.rotate(-rotationAngle * 0.75f, cx, cy);
    canvas.scale(1.0f, 0.65f, cx, cy);
    ringPaint.setColor(Color.argb(90, 167, 139, 250));
    float ring2Radius = maxRadius * 0.78f;
    canvas.drawCircle(cx, cy, ring2Radius, ringPaint);
    // Node indicator on ring 2
    canvas.drawCircle(cx, cy + ring2Radius, 4.0f, particlePaint);
    canvas.restore();

    // Ring 3 (Equatorial, Active state only)
    if (currentState == State.CODING || currentState == State.TOOL_EXECUTION || currentState == State.SEARCHING) {
      canvas.save();
      canvas.rotate(rotationAngle * 1.5f, cx, cy);
      canvas.scale(0.55f, 1.0f, cx, cy);
      ringPaint.setColor(Color.argb(160, 196, 181, 253));
      float ring3Radius = maxRadius * 0.88f;
      canvas.drawCircle(cx, cy, ring3Radius, ringPaint);
      canvas.restore();
    }
  }

  private void drawParticles(Canvas canvas, float cx, float cy, float maxRadius) {
    float speedFactor = getSpeedFactor();
    for (Particle p : particles) {
      p.angle = (p.angle + p.speed * speedFactor * 0.4f) % 360f;
      double rad = Math.toRadians(p.angle);
      float r = p.distance * maxRadius * (1.0f + 0.05f * pulseProgress);
      float px = cx + (float) (Math.cos(rad) * r);
      float py = cy + (float) (Math.sin(rad) * r);

      particlePaint.setColor(p.color);
      particlePaint.setAlpha((int) (p.alpha * 255));
      canvas.drawCircle(px, py, p.size, particlePaint);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (animator != null) {
      animator.cancel();
    }
  }
}
