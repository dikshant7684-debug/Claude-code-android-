package com.example.ui.system;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * Unified Motion Manager defining transition curves and durations.
 */
public class MotionManager {

  public static final long DURATION_MICRO = 150L;
  public static final long DURATION_STANDARD = 250L;
  public static final long DURATION_EMPHASIZED = 350L;
  public static final long DURATION_LONG = 500L;

  public static final Interpolator FAST_OUT_SLOW_IN = new AccelerateDecelerateInterpolator();
  public static final Interpolator DECELERATE = new DecelerateInterpolator();
  public static final Interpolator LINEAR = new LinearInterpolator();

  private static boolean animationsEnabled = true;

  public static void setAnimationsEnabled(boolean enabled) {
    animationsEnabled = enabled;
  }

  public static boolean areAnimationsEnabled() {
    return animationsEnabled;
  }

  public static long getAdjustedDuration(long standardDuration) {
    return animationsEnabled ? standardDuration : 0L;
  }
}
