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

    @Test
    void testMultiKeySequenceWithHeldModifiers_FirstKeyExact() {
        // Test that first key in sequence must match exactly (with modifiers)
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+A;B");
        
        // First key MUST match CTRL+A exactly
        KeyCombination firstKey = sequence.get(0);
        assertNotNull(firstKey);
        // Verify it requires Ctrl modifier (would need JavaFX engine to fully test)
        assertEquals(2, sequence.size(), "Sequence should have 2 keys");
    }

    @Test
    void testMultiKeySequenceWithHeldModifiers_SecondKeyLenient() {
        // Test that second key in sequence uses lenient matching
        // This verifies the structure allows lenient matching for non-first keys
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+S;F");
        
        KeyCombination secondKey = sequence.get(1);
        assertNotNull(secondKey, "Second key should be parsed");
        // Second key 'F' should match with key code only (lenient)
        // This would be verified at runtime with KeyBindingService.matchesKeyCodeOnly()
        assertEquals(2, sequence.size());
    }

    @Test
    void testMultiKeySequenceWithAllLenientKeys() {
        // Test sequence where all keys after first use lenient matching
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+ALT+D;A;B;C");
        
        assertEquals(4, sequence.size());
        // First key: CTRL+ALT+D (exact match required)
        // Keys 2-4: A, B, C (lenient matching - key code only)
        assertNotNull(sequence.get(0));
        assertNotNull(sequence.get(1));
        assertNotNull(sequence.get(2));
        assertNotNull(sequence.get(3));
    }
}
