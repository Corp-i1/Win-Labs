package com.winlabs.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import javafx.scene.input.KeyCombination;
import java.util.List;

/**
 * Tests for multi-key sequence support in KeyBindingService.
 */
class KeyBindingServiceMultiKeyTest {

    @Test
    void testParseMultiKeySequence_SingleKey() {
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("SPACE");
        assertEquals(1, sequence.size());
        assertNotNull(sequence.get(0));
    }

    @Test
    void testParseMultiKeySequence_TwoKeys() {
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+A;B");
        assertEquals(2, sequence.size());
        assertNotNull(sequence.get(0));
        assertNotNull(sequence.get(1));
    }

    @Test
    void testParseMultiKeySequence_ThreeKeys() {
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+SHIFT+D;C;SPACE");
        assertEquals(3, sequence.size());
        for (KeyCombination kc : sequence) {
            assertNotNull(kc);
        }
    }

    @Test
    void testParseMultiKeySequence_ComplexSequence() {
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+ALT+DELETE;A;SHIFT+B;C");
        assertEquals(4, sequence.size());
        for (KeyCombination kc : sequence) {
            assertNotNull(kc);
        }
    }

    @Test
    void testParseMultiKeySequence_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyBindingService.parseMultiKeySequence(null);
        });
    }

    @Test
    void testParseMultiKeySequence_Empty() {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyBindingService.parseMultiKeySequence("");
        });
    }

    @Test
    void testParseMultiKeySequence_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            KeyBindingService.parseMultiKeySequence("INVALID_KEY_NAME");
        });
    }

    @Test
    void testParseMultiKeySequence_Whitespace() {
        // Should handle whitespace gracefully
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("  CTRL+A ; B ; C  ");
        assertEquals(3, sequence.size());
        for (KeyCombination kc : sequence) {
            assertNotNull(kc);
        }
    }

    @Test
    void testParseBinding_StillWorks() {
        // Ensure backward compatibility - single keys should still work
        KeyCombination kc = KeyBindingService.parseBinding("CTRL+S");
        assertNotNull(kc);
    }

    @Test
    void testParseBinding_WithModifiers() {
        KeyCombination kc = KeyBindingService.parseBinding("CTRL+ALT+SHIFT+DELETE");
        assertNotNull(kc);
    }

    @Test
    void testParseBinding_JustKey() {
        KeyCombination kc = KeyBindingService.parseBinding("SPACE");
        assertNotNull(kc);
    }
}
