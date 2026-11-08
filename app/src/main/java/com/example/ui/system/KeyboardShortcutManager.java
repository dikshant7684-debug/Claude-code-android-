package com.example.ui.system;

import android.view.KeyEvent;

/**
 * Physical Keyboard Shortcut Manager.
 * Detects standard shortcuts when a hardware keyboard is connected.
 */
public class KeyboardShortcutManager {

  public enum ShortcutAction {
    NONE,
    OPEN_COMMAND_PALETTE,  // Ctrl + K
    NEW_CONVERSATION,      // Ctrl + N
    OPEN_PROJECT,          // Ctrl + O
    SAVE_FILE,             // Ctrl + S
    SUBMIT_PROMPT,         // Ctrl + Enter
    SEARCH_COMMANDS,       // Ctrl + Shift + P
    CLOSE_OVERLAY,         // Escape
    TOGGLE_DRAWER,         // Ctrl + B
    COPY_CODE              // Ctrl + Shift + C
  }

  public static ShortcutAction processKeyEvent(KeyEvent event) {
    if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
      return ShortcutAction.NONE;
    }

    boolean isCtrl = event.isCtrlPressed();
    boolean isShift = event.isShiftPressed();
    int keyCode = event.getKeyCode();

    if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
      return ShortcutAction.CLOSE_OVERLAY;
    }

    if (isCtrl) {
      if (keyCode == KeyEvent.KEYCODE_K) {
        return ShortcutAction.OPEN_COMMAND_PALETTE;
      }
      if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
        return ShortcutAction.SUBMIT_PROMPT;
      }
      if (keyCode == KeyEvent.KEYCODE_N) {
        return ShortcutAction.NEW_CONVERSATION;
      }
      if (keyCode == KeyEvent.KEYCODE_O) {
        return ShortcutAction.OPEN_PROJECT;
      }
      if (keyCode == KeyEvent.KEYCODE_S) {
        return ShortcutAction.SAVE_FILE;
      }
      if (keyCode == KeyEvent.KEYCODE_B) {
        return ShortcutAction.TOGGLE_DRAWER;
      }
      if (isShift && keyCode == KeyEvent.KEYCODE_P) {
        return ShortcutAction.SEARCH_COMMANDS;
      }
      if (isShift && keyCode == KeyEvent.KEYCODE_C) {
        return ShortcutAction.COPY_CODE;
      }
    }

    return ShortcutAction.NONE;
  }
}
