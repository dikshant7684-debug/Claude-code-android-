package com.example.ui.system;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Haptic Feedback Manager delivering subtle, accessible vibration cues
 * while strictly respecting Android system and user preference toggles.
 */
public class HapticManager {

  private static volatile HapticManager INSTANCE;
  private final Context appContext;
  private final Vibrator vibrator;
  private boolean hapticsEnabled = true;

  public static HapticManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (HapticManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new HapticManager(context.getApplicationContext());
        }
      }
    }
    return INSTANCE;
  }

  private HapticManager(Context context) {
    this.appContext = context;
    this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
  }

  public void setHapticsEnabled(boolean enabled) {
    this.hapticsEnabled = enabled;
  }

  public boolean isHapticsEnabled() {
    return hapticsEnabled;
  }

  private boolean canVibrate() {
    if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) {
      return false;
    }
    try {
      int hapticSetting = Settings.System.getInt(
          appContext.getContentResolver(),
          Settings.System.HAPTIC_FEEDBACK_ENABLED,
          1
      );
      return hapticSetting != 0;
    } catch (Exception e) {
      return true;
    }
  }

  public void triggerButtonClick(View view) {
    if (view != null && canVibrate()) {
      view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
      return;
    }
    triggerTick();
  }

  public void triggerTick() {
    if (!canVibrate()) return;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
      } else {
        vibrator.vibrate(12);
      }
    } catch (Exception ignored) {}
  }

  public void triggerSuccess() {
    if (!canVibrate()) return;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
      } else {
        vibrator.vibrate(25);
      }
    } catch (Exception ignored) {}
  }

  public void triggerCopy() {
    triggerSuccess();
  }

  public void triggerError() {
    if (!canVibrate()) return;
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        long[] timings = new long[]{0, 30, 60, 30};
        int[] amplitudes = new int[]{0, 140, 0, 180};
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
      } else {
        vibrator.vibrate(new long[]{0, 30, 60, 30}, -1);
      }
    } catch (Exception ignored) {}
  }
}
