package com.example.ui.system;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Theme Manager providing Dark (Nebula Purple + Charcoal Black),
 * Light, and Custom theme configurations with local persistence.
 */
public class ThemeManager {

  public enum ThemeMode {
    DARK,
    LIGHT,
    CUSTOM
  }

  private static final String PREF_NAME = "neucode_theme_prefs";
  private static final String KEY_THEME_MODE = "theme_mode";
  private static final String KEY_CUSTOM_BG = "custom_bg";
  private static final String KEY_CUSTOM_SURFACE = "custom_surface";
  private static final String KEY_CUSTOM_ACCENT = "custom_accent";
  private static final String KEY_CUSTOM_BORDER = "custom_border";

  // Nebula Dark Theme Constants
  public static final int DARK_BG = Color.parseColor("#09090D");
  public static final int DARK_SURFACE = Color.parseColor("#14131A");
  public static final int DARK_ELEVATED_SURFACE = Color.parseColor("#1D1A26");
  public static final int DARK_PRIMARY_PURPLE = Color.parseColor("#8B5CF6");
  public static final int DARK_ACCENT_VIOLET = Color.parseColor("#A78BFA");
  public static final int DARK_LAVENDER = Color.parseColor("#C4B5FD");
  public static final int DARK_TEXT_PRIMARY = Color.parseColor("#F5F3FA");
  public static final int DARK_TEXT_SECONDARY = Color.parseColor("#A5A0B2");
  public static final int DARK_BORDER = Color.parseColor("#302A3B");

  // Light Theme Constants
  public static final int LIGHT_BG = Color.parseColor("#F7F6FA");
  public static final int LIGHT_SURFACE = Color.parseColor("#FFFFFF");
  public static final int LIGHT_PRIMARY_PURPLE = Color.parseColor("#7041C8");
  public static final int LIGHT_ACCENT = Color.parseColor("#8B5CF6");
  public static final int LIGHT_TEXT_PRIMARY = Color.parseColor("#191522");
  public static final int LIGHT_TEXT_SECONDARY = Color.parseColor("#716B80");
  public static final int LIGHT_BORDER = Color.parseColor("#E7E2EF");

  private static volatile ThemeManager INSTANCE;
  private final SharedPreferences prefs;

  public static ThemeManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (ThemeManager.class) {
        if (INSTANCE == null) {
          INSTANCE = new ThemeManager(context.getApplicationContext());
        }
      }
    }
    return INSTANCE;
  }

  private ThemeManager(Context context) {
    this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
  }

  public ThemeMode getThemeMode() {
    String mode = prefs.getString(KEY_THEME_MODE, "DARK");
    try {
      return ThemeMode.valueOf(mode);
    } catch (Exception e) {
      return ThemeMode.DARK;
    }
  }

  public void setThemeMode(ThemeMode mode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.name()).apply();
  }

  public int getBackgroundColor() {
    ThemeMode mode = getThemeMode();
    if (mode == ThemeMode.LIGHT) return LIGHT_BG;
    if (mode == ThemeMode.CUSTOM) return prefs.getInt(KEY_CUSTOM_BG, DARK_BG);
    return DARK_BG;
  }

  public int getSurfaceColor() {
    ThemeMode mode = getThemeMode();
    if (mode == ThemeMode.LIGHT) return LIGHT_SURFACE;
    if (mode == ThemeMode.CUSTOM) return prefs.getInt(KEY_CUSTOM_SURFACE, DARK_SURFACE);
    return DARK_SURFACE;
  }

  public int getAccentColor() {
    ThemeMode mode = getThemeMode();
    if (mode == ThemeMode.LIGHT) return LIGHT_ACCENT;
    if (mode == ThemeMode.CUSTOM) return prefs.getInt(KEY_CUSTOM_ACCENT, DARK_PRIMARY_PURPLE);
    return DARK_PRIMARY_PURPLE;
  }

  public int getBorderColor() {
    ThemeMode mode = getThemeMode();
    if (mode == ThemeMode.LIGHT) return LIGHT_BORDER;
    if (mode == ThemeMode.CUSTOM) return prefs.getInt(KEY_CUSTOM_BORDER, DARK_BORDER);
    return DARK_BORDER;
  }

  public void saveCustomTheme(int bg, int surface, int accent, int border) {
    prefs.edit()
        .putString(KEY_THEME_MODE, ThemeMode.CUSTOM.name())
        .putInt(KEY_CUSTOM_BG, bg)
        .putInt(KEY_CUSTOM_SURFACE, surface)
        .putInt(KEY_CUSTOM_ACCENT, accent)
        .putInt(KEY_CUSTOM_BORDER, border)
        .apply();
  }

  public void restoreDefaults() {
    prefs.edit().clear().apply();
  }
}
