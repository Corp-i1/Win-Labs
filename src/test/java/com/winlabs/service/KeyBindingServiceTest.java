package com.winlabs.service;

import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

class KeyBindingServiceTest {

    @Test
    void parseBindingParsesSingleKeyCorrectly() {
        KeyCombination binding = KeyBindingService.parseBinding("SPACE");
        assertNotNull(binding);
        // Verify it matches a SPACE keypress
        KeyEvent spaceEvent = createKeyEvent(KeyCode.SPACE, false, false, false, false);
        assertTrue(KeyBindingService.matchesKeyEvent(binding, spaceEvent));
    }

    @Test
    void parseBindingParsesCtrlModifierCorrectly() {
        KeyCombination binding = KeyBindingService.parseBinding("CTRL+N");
        assertNotNull(binding);
        
        // Should match Ctrl+N
        KeyEvent ctrlNEvent = createKeyEvent(KeyCode.N, true, false, false, false);
        assertTrue(KeyBindingService.matchesKeyEvent(binding, ctrlNEvent));
        
        // Should not match just N
        KeyEvent nEvent = createKeyEvent(KeyCode.N, false, false, false, false);
        assertFalse(KeyBindingService.matchesKeyEvent(binding, nEvent));
    }

    @Test
    void parseBindingParsesMultipleModifiersCorrectly() {
        KeyCombination binding = KeyBindingService.parseBinding("CTRL+SHIFT+S");
        assertNotNull(binding);
        
        KeyEvent matchingEvent = createKeyEvent(KeyCode.S, true, false, true, false);
        assertTrue(KeyBindingService.matchesKeyEvent(binding, matchingEvent));
        
        // Wrong modifier combination should not match
        KeyEvent wrongEvent = createKeyEvent(KeyCode.S, true, false, false, false);
        assertFalse(KeyBindingService.matchesKeyEvent(binding, wrongEvent));
    }

    @Test
    void parseBindingHandlesSpecialKeyNames() {
        KeyCombination enter = KeyBindingService.parseBinding("ENTER");
        KeyCombination escape = KeyBindingService.parseBinding("ESC");
        KeyCombination comma = KeyBindingService.parseBinding("COMMA");
        KeyCombination period = KeyBindingService.parseBinding(",");
        
        assertNotNull(enter);
        assertNotNull(escape);
        assertNotNull(comma);
        assertNotNull(period);
    }

    @Test
    void parseBindingHandlesCaseInsensitivity() {
        KeyCombination binding1 = KeyBindingService.parseBinding("ctrl+n");
        KeyCombination binding2 = KeyBindingService.parseBinding("CTRL+N");
        KeyCombination binding3 = KeyBindingService.parseBinding("Ctrl+N");
        
        assertNotNull(binding1);
        assertNotNull(binding2);
        assertNotNull(binding3);
        
        KeyEvent event = createKeyEvent(KeyCode.N, true, false, false, false);
        assertTrue(KeyBindingService.matchesKeyEvent(binding1, event));
        assertTrue(KeyBindingService.matchesKeyEvent(binding2, event));
        assertTrue(KeyBindingService.matchesKeyEvent(binding3, event));
    }

    @Test
    void parseBindingThrowsExceptionForNullBinding() {
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding(null));
    }

    @Test
    void parseBindingThrowsExceptionForEmptyBinding() {
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding(""));
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("   "));
    }

    @Test
    void parseBindingThrowsExceptionForInvalidKeyName() {
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("NOTAKEY"));
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("CTRL+NOTAKEY"));
    }

    @Test
    void parseBindingThrowsExceptionForOnlyModifiers() {
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("CTRL+SHIFT"));
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("CTRL"));
    }

    @Test
    void parseBindingThrowsExceptionForMultipleMainKeys() {
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("A+B+C"));
        assertThrows(IllegalArgumentException.class, () -> KeyBindingService.parseBinding("CTRL+A+B"));
    }

    @Test
    void matchesKeyEventReturnsTrueForMatchingBinding() {
        KeyCombination binding = KeyBindingService.parseBinding("A");
        KeyEvent event = createKeyEvent(KeyCode.A, false, false, false, false);
        
        assertTrue(KeyBindingService.matchesKeyEvent(binding, event));
    }

    @Test
    void matchesKeyEventReturnsFalseForNonMatchingBinding() {
        KeyCombination binding = KeyBindingService.parseBinding("A");
        KeyEvent event = createKeyEvent(KeyCode.B, false, false, false, false);
        
        assertFalse(KeyBindingService.matchesKeyEvent(binding, event));
    }

    @Test
    void matchesKeyEventHandlesNullBinding() {
        KeyEvent event = createKeyEvent(KeyCode.A, false, false, false, false);
        assertFalse(KeyBindingService.matchesKeyEvent(null, event));
    }

    @Test
    void matchesKeyEventHandlesNullEvent() {
        KeyCombination binding = KeyBindingService.parseBinding("A");
        assertFalse(KeyBindingService.matchesKeyEvent(binding, null));
    }

    @Test
    void parseBindingHandlesAltModifier() {
        KeyCombination binding = KeyBindingService.parseBinding("ALT+D");
        KeyEvent event = createKeyEvent(KeyCode.D, false, true, false, false);
        assertTrue(KeyBindingService.matchesKeyEvent(binding, event));
    }

    @Test
    void parseBindingHandlesMetaModifier() {
        KeyCombination binding = KeyBindingService.parseBinding("META+S");
        KeyEvent event = createKeyEvent(KeyCode.S, false, false, false, true);
        assertTrue(KeyBindingService.matchesKeyEvent(binding, event));
    }

    @Test
    void parseBindingHandlesArrowKeys() {
        KeyCombination up = KeyBindingService.parseBinding("UP");
        KeyCombination down = KeyBindingService.parseBinding("DOWN");
        KeyCombination left = KeyBindingService.parseBinding("LEFT");
        KeyCombination right = KeyBindingService.parseBinding("RIGHT");
        
        assertNotNull(up);
        assertNotNull(down);
        assertNotNull(left);
        assertNotNull(right);
        
        assertTrue(KeyBindingService.matchesKeyEvent(up, createKeyEvent(KeyCode.UP, false, false, false, false)));
        assertTrue(KeyBindingService.matchesKeyEvent(down, createKeyEvent(KeyCode.DOWN, false, false, false, false)));
        assertTrue(KeyBindingService.matchesKeyEvent(left, createKeyEvent(KeyCode.LEFT, false, false, false, false)));
        assertTrue(KeyBindingService.matchesKeyEvent(right, createKeyEvent(KeyCode.RIGHT, false, false, false, false)));
    }

    @Test
    void parseBindingHandlesFunctionKeys() {
        KeyCombination f1 = KeyBindingService.parseBinding("F1");
        KeyCombination f12 = KeyBindingService.parseBinding("F12");
        
        assertNotNull(f1);
        assertNotNull(f12);
    }

    /**
     * Helper to create a mock KeyEvent for testing.
     * Note: In a real test environment, you might use a testing framework that
     * can properly mock KeyEvent. This creates a minimal stub for testing matching logic.
     */
    private KeyEvent createKeyEvent(KeyCode code, boolean ctrl, boolean alt, boolean shift, boolean meta) {
        // In a real test, you'd use a proper mock or test framework.
        // For now, we'll rely on the fact that KeyCombination matching
        // works correctly when tested against real JavaFX key events.
        // This test is more of an integration test.
        return new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",                 // character
            code.toString(),    // text
            code,               // code
            shift,              // shiftDown
            ctrl,               // ctrlDown
            alt,                // altDown
            meta                // metaDown
        );
    }
}
