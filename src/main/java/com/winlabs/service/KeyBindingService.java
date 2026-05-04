package com.winlabs.service;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Service for parsing and matching keyboard shortcuts.
 * Handles conversion of shortcut strings (e.g., "CTRL+SHIFT+Space") to KeyCombination objects
 * and matching KeyEvent objects against registered shortcuts.
 */
public final class KeyBindingService {
    private static final Logger logger = LoggerFactory.getLogger(KeyBindingService.class);

    private KeyBindingService() {
        // Utility class
    }

    /**
     * Parses a multi-key sequence string into a list of KeyCombination objects.
     * Format Details:
     * - First key: may have modifiers [CTRL+][ALT+][SHIFT+][META+]
     * - Subsequent keys: bare key names ONLY (no modifiers in the format string)
     * 
     * Examples: "SPACE", "CTRL+S", "SHIFT+Alt+D", "CTRL+A;B", "CTRL+A;B;C"
     * 
     * Lenient Matching: Non-first keys match by key code only, allowing users to hold modifiers
     * from the previous key press (e.g., "CTRL+A;B" works even if user holds Ctrl while pressing B).
     *
     * @param sequenceStr the sequence string to parse (may contain multiple key combinations separated by semicolon)
     * @return a list of KeyCombination objects (guaranteed non-empty)
     * @throws IllegalArgumentException if the sequence string is null, empty, or invalid
     */
    public static List<KeyCombination> parseMultiKeySequence(String sequenceStr) {
        if (sequenceStr == null || sequenceStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Sequence string cannot be null or empty");
        }

        List<KeyCombination> result = new ArrayList<>();
        String[] keyCombinations = sequenceStr.trim().split(";");

        if (keyCombinations.length == 0) {
            throw new IllegalArgumentException("Invalid sequence format: " + sequenceStr);
        }

        for (String keyCombStr : keyCombinations) {
            try {
                KeyCombination kc = parseSingleKeyBindingInternal(keyCombStr.trim());
                result.add(kc);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to parse key combination in sequence '" + sequenceStr + "': " + e.getMessage(), e);
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("No valid key combinations found in sequence: " + sequenceStr);
        }

        return result;
    }

    /**
     * Parses a binding string into a KeyCombination.
     * Format: [CTRL+][ALT+][SHIFT+][META+]KeyName
     * Examples: "SPACE", "CTRL+S", "SHIFT+Alt+D"
     *
     * @param bindingStr the binding string to parse
     * @return a KeyCombination object
     * @throws IllegalArgumentException if the binding string is null, empty, or invalid
     */
    public static KeyCombination parseBinding(String bindingStr) {
        if (bindingStr == null || bindingStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Binding string cannot be null or empty");
        }

        return parseSingleKeyBindingInternal(bindingStr);
    }

    /**
     * Internal method to parse a single key binding.
     */
    private static KeyCombination parseSingleKeyBindingInternal(String bindingStr) {
        String trimmed = bindingStr.trim().toUpperCase();
        String[] parts = trimmed.split("\\+");

        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid binding format: " + bindingStr);
        }

        // Extract modifiers and key
        boolean hasCtrl = false;
        boolean hasAlt = false;
        boolean hasShift = false;
        boolean hasMeta = false;
        String mainKey = null;

        for (String part : parts) {
            part = part.trim();
            switch (part) {
                case "CTRL":
                case "CONTROL":
                    hasCtrl = true;
                    break;
                case "ALT":
                    hasAlt = true;
                    break;
                case "SHIFT":
                    hasShift = true;
                    break;
                case "META":
                case "WIN":
                case "CMD":
                    hasMeta = true;
                    break;
                default:
                    if (mainKey == null) {
                        mainKey = part;
                    } else {
                        // Multiple non-modifier keys found
                        throw new IllegalArgumentException("Invalid binding format: multiple main keys in " + bindingStr);
                    }
            }
        }

        if (mainKey == null || mainKey.isEmpty()) {
            throw new IllegalArgumentException("No main key found in binding: " + bindingStr);
        }

        // Validate that the key name is valid
        KeyCode keyCode = parseKeyCode(mainKey);
        if (keyCode == null) {
            throw new IllegalArgumentException("Unknown key: " + mainKey);
        }

        // Build KeyCombination string for JavaFX
        StringBuilder keyCombStr = new StringBuilder();
        if (hasCtrl) keyCombStr.append("Ctrl+");
        if (hasAlt) keyCombStr.append("Alt+");
        if (hasShift) keyCombStr.append("Shift+");
        if (hasMeta) keyCombStr.append("Meta+");
        keyCombStr.append(mainKey);

        try {
            return KeyCombination.valueOf(keyCombStr.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to create KeyCombination from: " + keyCombStr.toString(), e);
        }
    }

    /**
     * Matches a KeyEvent against a KeyCombination.
     *
     * @param binding the KeyCombination to match against
     * @param event   the KeyEvent to test
     * @return true if the event matches the binding, false otherwise
     */
    public static boolean matchesKeyEvent(KeyCombination binding, KeyEvent event) {
        if (binding == null || event == null) {
            return false;
        }
        return binding.match(event);
    }

    /**
     * Matches a KeyEvent against a KeyCombination by key code only.
     * This lenient match is used for later keys in a multi-key sequence, where the
     * user may still be holding modifiers from the previous key press.
     *
     * @param binding the KeyCombination to match against
     * @param event   the KeyEvent to test
     * @return true if the event's key code matches the binding's key code, false otherwise
     */
    public static boolean matchesKeyCodeOnly(KeyCombination binding, KeyEvent event) {
        if (binding == null || event == null) {
            return false;
        }
        try {
            // Extract the key code from the binding string and compare with event
            // We need to get the main key from the binding without modifiers
            KeyCode expectedCode = extractMainKeyCode(binding);
            KeyCode eventCode = event.getCode();
            
            return expectedCode != null && expectedCode == eventCode;
        } catch (Exception e) {
            // Fallback to exact match
            return binding.match(event);
        }
    }

    /**
     * Extracts the main key code from a KeyCombination by stripping any modifiers first.
     *
     * @param binding the KeyCombination to extract from
     * @return the KeyCode of the main key, or null if unable to extract
     */
    private static KeyCode extractMainKeyCode(KeyCombination binding) {
        if (binding == null) {
            return null;
        }
        return parseKeyCode(extractMainKeyName(binding.getName()));
    }

    /**
     * Extracts the final key token from a binding name after removing any modifiers.
     *
     * @param bindingName the KeyCombination name, such as CTRL+SHIFT+S
     * @return the main key token, or null if the input is null or empty
     */
    private static String extractMainKeyName(String bindingName) {
        if (bindingName == null || bindingName.isEmpty()) {
            return null;
        }

        String normalizedBinding = bindingName.toUpperCase();
        int lastPlusIndex = normalizedBinding.lastIndexOf('+');
        return lastPlusIndex >= 0 ? normalizedBinding.substring(lastPlusIndex + 1) : normalizedBinding;
    }

    /**
     * Registry of key name aliases mapped to their KeyCode values.
     * Centralizes all special key name handling in one place for maintainability.
     */
    private static class KeyCodeRegistry {
        private static final Map<String, KeyCode> REGISTRY = buildRegistry();

        private static Map<String, KeyCode> buildRegistry() {
            Map<String, KeyCode> map = new HashMap<>();
            // Space and spacebar
            map.put("SPACE", KeyCode.SPACE);
            map.put("SPACEBAR", KeyCode.SPACE);
            // Enter/return
            map.put("ENTER", KeyCode.ENTER);
            map.put("RETURN", KeyCode.ENTER);
            // Escape
            map.put("ESC", KeyCode.ESCAPE);
            map.put("ESCAPE", KeyCode.ESCAPE);
            // Tab
            map.put("TAB", KeyCode.TAB);
            // Backspace
            map.put("BACKSPACE", KeyCode.BACK_SPACE);
            map.put("BACK_SPACE", KeyCode.BACK_SPACE);
            // Delete
            map.put("DELETE", KeyCode.DELETE);
            map.put("DEL", KeyCode.DELETE);
            // Insert
            map.put("INSERT", KeyCode.INSERT);
            map.put("INS", KeyCode.INSERT);
            // Navigation
            map.put("HOME", KeyCode.HOME);
            map.put("END", KeyCode.END);
            map.put("PAGE_UP", KeyCode.PAGE_UP);
            map.put("PAGEUP", KeyCode.PAGE_UP);
            map.put("PRIOR", KeyCode.PAGE_UP);
            map.put("PAGE_DOWN", KeyCode.PAGE_DOWN);
            map.put("PAGEDOWN", KeyCode.PAGE_DOWN);
            map.put("NEXT", KeyCode.PAGE_DOWN);
            // Arrow keys
            map.put("UP", KeyCode.UP);
            map.put("ARROW_UP", KeyCode.UP);
            map.put("DOWN", KeyCode.DOWN);
            map.put("ARROW_DOWN", KeyCode.DOWN);
            map.put("LEFT", KeyCode.LEFT);
            map.put("ARROW_LEFT", KeyCode.LEFT);
            map.put("RIGHT", KeyCode.RIGHT);
            map.put("ARROW_RIGHT", KeyCode.RIGHT);
            // Punctuation
            map.put("COMMA", KeyCode.COMMA);
            map.put(",", KeyCode.COMMA);
            map.put("PERIOD", KeyCode.PERIOD);
            map.put("DOT", KeyCode.PERIOD);
            map.put(".", KeyCode.PERIOD);
            map.put("SEMICOLON", KeyCode.SEMICOLON);
            map.put(";", KeyCode.SEMICOLON);
            map.put("SLASH", KeyCode.SLASH);
            map.put("/", KeyCode.SLASH);
            map.put("BACKSLASH", KeyCode.BACK_SLASH);
            map.put("\\", KeyCode.BACK_SLASH);
            map.put("QUOTE", KeyCode.QUOTE);
            map.put("'", KeyCode.QUOTE);
            map.put("DOUBLE_QUOTE", KeyCode.QUOTE);
            map.put("\"", KeyCode.QUOTE);
            map.put("BACKTICK", KeyCode.BACK_QUOTE);
            map.put("`", KeyCode.BACK_QUOTE);
            map.put("EQUALS", KeyCode.EQUALS);
            map.put("EQUAL", KeyCode.EQUALS);
            map.put("=", KeyCode.EQUALS);
            map.put("MINUS", KeyCode.MINUS);
            map.put("-", KeyCode.MINUS);
            map.put("OPEN_BRACKET", KeyCode.OPEN_BRACKET);
            map.put("[", KeyCode.OPEN_BRACKET);
            map.put("CLOSE_BRACKET", KeyCode.CLOSE_BRACKET);
            map.put("]", KeyCode.CLOSE_BRACKET);
            return map;
        }

        /**
         * Look up a key name alias in the registry.
         * Falls back to KeyCode.getKeyCode() if not found in registry.
         *
         * @param normalizedKeyName the normalized (uppercase) key name
         * @return the KeyCode, or null if not found
         */
        static KeyCode lookup(String normalizedKeyName) {
            if (normalizedKeyName == null || normalizedKeyName.isEmpty()) {
                return null;
            }
            // Try registry first for special aliases
            KeyCode registered = REGISTRY.get(normalizedKeyName);
            if (registered != null) {
                return registered;
            }
            // Fall back to JavaFX KeyCode enum for standard names (A-Z, F1-F12, etc.)
            try {
                return KeyCode.getKeyCode(normalizedKeyName);
            } catch (IllegalArgumentException e) {
                logger.debug("Unknown key name: {}", normalizedKeyName);
                return null;
            }
        }
    }

    /**
     * Parses a key name string to KeyCode.
     * Handles various naming conventions (e.g., "Space", "SPACE", "A", "ENTER", "Enter").
     * Uses KeyCodeRegistry for special aliases, falls back to JavaFX KeyCode enum for standard names.
     *
     * @param keyName the key name to parse
     * @return the KeyCode, or null if not found
     */
    private static KeyCode parseKeyCode(String keyName) {
        if (keyName == null || keyName.isEmpty()) {
            return null;
        }
        String normalized = keyName.toUpperCase().replace(" ", "_");
        return KeyCodeRegistry.lookup(normalized);
    }
}
