package com.winlabs.service;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

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
                KeyCombination kc = parseSingleKeyBinding(keyCombStr.trim());
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
     * Parses a single key binding string into a KeyCombination.
     * Format: [CTRL+][ALT+][SHIFT+][META+]KeyName
     * Examples: "SPACE", "CTRL+S", "SHIFT+Alt+D"
     *
     * @param bindingStr the binding string to parse
     * @return a KeyCombination object
     * @throws IllegalArgumentException if the binding string is null, empty, or invalid
     */
    private static KeyCombination parseSingleKeyBinding(String bindingStr) {
        if (bindingStr == null || bindingStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Binding string cannot be null or empty");
        }

        return parseSingleKeyBindingInternal(bindingStr);
    }

    /**
     * Parses a binding string into a KeyCombination
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
     * Matches a KeyEvent against a KeyCombination by key code only, ignoring modifiers.
     * This is used for non-first keys in multi-key sequences, where users may still have
     * modifiers held from the previous key press.
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
     * Extracts the main key code from a KeyCombination name, stripping modifiers.
     * Avoids regex for performance on the hot path.
     *
     * @param binding the KeyCombination to extract from
     * @return the KeyCode of the main key, or null if unable to extract
     */
    private static KeyCode extractMainKeyCode(KeyCombination binding) {
        if (binding == null) {
            return null;
        }
        try {
            String bindingName = binding.getName().toUpperCase();
            // Find the last '+' and take everything after it as the main key
            // This is more efficient than regex
            int lastPlusIndex = bindingName.lastIndexOf('+');
            String mainKey = (lastPlusIndex >= 0) ? bindingName.substring(lastPlusIndex + 1) : bindingName;
            return parseKeyCode(mainKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses a key name string to KeyCode.
     * Handles various naming conventions (e.g., "Space", "SPACE", "A", "ENTER", "Enter")
     *
     * @param keyName the key name to parse
     * @return the KeyCode, or null if not found
     */
    private static KeyCode parseKeyCode(String keyName) {
        if (keyName == null || keyName.isEmpty()) {
            return null;
        }

        String normalized = keyName.toUpperCase().replace(" ", "_");

        // Handle special cases
        switch (normalized) {
            case "SPACE":
            case "SPACEBAR":
                return KeyCode.SPACE;
            case "ENTER":
            case "RETURN":
                return KeyCode.ENTER;
            case "ESC":
            case "ESCAPE":
                return KeyCode.ESCAPE;
            case "TAB":
                return KeyCode.TAB;
            case "BACKSPACE":
            case "BACK_SPACE":
                return KeyCode.BACK_SPACE;
            case "DELETE":
            case "DEL":
                return KeyCode.DELETE;
            case "INSERT":
            case "INS":
                return KeyCode.INSERT;
            case "HOME":
                return KeyCode.HOME;
            case "END":
                return KeyCode.END;
            case "PAGE_UP":
            case "PAGEUP":
            case "PRIOR":
                return KeyCode.PAGE_UP;
            case "PAGE_DOWN":
            case "PAGEDOWN":
            case "NEXT":
                return KeyCode.PAGE_DOWN;
            case "UP":
            case "ARROW_UP":
                return KeyCode.UP;
            case "DOWN":
            case "ARROW_DOWN":
                return KeyCode.DOWN;
            case "LEFT":
            case "ARROW_LEFT":
                return KeyCode.LEFT;
            case "RIGHT":
            case "ARROW_RIGHT":
                return KeyCode.RIGHT;
            case "COMMA":
            case ",":
                return KeyCode.COMMA;
            case "PERIOD":
            case "DOT":
            case ".":
                return KeyCode.PERIOD;
            case "SEMICOLON":
            case ";":
                return KeyCode.SEMICOLON;
            case "SLASH":
            case "/":
                return KeyCode.SLASH;
            case "BACKSLASH":
            case "\\":
                return KeyCode.BACK_SLASH;
            case "QUOTE":
            case "'":
                return KeyCode.QUOTE;
            case "DOUBLE_QUOTE":
            case "\"":
                return KeyCode.QUOTE;
            case "BACKTICK":
            case "`":
                return KeyCode.BACK_QUOTE;
            case "EQUALS":
            case "EQUAL":
            case "=":
                return KeyCode.EQUALS;
            case "MINUS":
            case "-":
                return KeyCode.MINUS;
            case "OPEN_BRACKET":
            case "[":
                return KeyCode.OPEN_BRACKET;
            case "CLOSE_BRACKET":
            case "]":
                return KeyCode.CLOSE_BRACKET;
            default:
                // Try to find by KeyCode enum value
                try {
                    return KeyCode.getKeyCode(normalized);
                } catch (IllegalArgumentException e) {
                    logger.debug("Unknown key name: {}", keyName);
                    return null;
                }
        }
    }
}
