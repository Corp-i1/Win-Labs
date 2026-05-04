package com.winlabs.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+A;B");

        KeyCombination firstKey = sequence.get(0);
        assertNotNull(firstKey);

        assertTrue(KeyBindingService.matchesKeyEvent(
            firstKey,
            createKeyEvent(KeyCode.A, true, false, false, false)));
        assertFalse(KeyBindingService.matchesKeyEvent(
            firstKey,
            createKeyEvent(KeyCode.A, false, false, false, false)));
        assertEquals(2, sequence.size());
    }

    @Test
    void testMultiKeySequenceWithHeldModifiers_SecondKeyLenient() {
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+S;F");
        
        KeyCombination secondKey = sequence.get(1);
        assertNotNull(secondKey);
        assertTrue(KeyBindingService.matchesKeyCodeOnly(
            secondKey,
            createKeyEvent(KeyCode.F, true, false, false, false)));
        assertTrue(KeyBindingService.matchesKeyCodeOnly(
            secondKey,
            createKeyEvent(KeyCode.F, true, true, true, true)));
        assertFalse(KeyBindingService.matchesKeyCodeOnly(
            secondKey,
            createKeyEvent(KeyCode.G, true, false, false, false)));
        assertEquals(2, sequence.size());
    }

    @Test
    void testMultiKeySequenceWithAllLenientKeys() {
        List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence("CTRL+ALT+D;A;B;C");
        
        assertEquals(4, sequence.size());
        assertNotNull(sequence.get(0));
        assertNotNull(sequence.get(1));
        assertNotNull(sequence.get(2));
        assertNotNull(sequence.get(3));

        assertTrue(KeyBindingService.matchesKeyEvent(
            sequence.get(0),
            createKeyEvent(KeyCode.D, true, true, false, false)));
        assertTrue(KeyBindingService.matchesKeyCodeOnly(
            sequence.get(1),
            createKeyEvent(KeyCode.A, false, false, false, false)));
        assertTrue(KeyBindingService.matchesKeyCodeOnly(
            sequence.get(2),
            createKeyEvent(KeyCode.B, true, false, true, false)));
        assertTrue(KeyBindingService.matchesKeyCodeOnly(
            sequence.get(3),
            createKeyEvent(KeyCode.C, false, true, true, true)));
    }

    private KeyEvent createKeyEvent(KeyCode code, boolean ctrl, boolean alt, boolean shift, boolean meta) {
        return new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            code.toString(),
            code,
            shift,
            ctrl,
            alt,
            meta
        );
    }
}
