package com.winlabs.view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for keyboard action debouncing logic.
 * 
 * Tests the time-based debouncing behavior that prevents the same action
 * from firing multiple times during a single key hold, while allowing
 * different actions to fire immediately and the same action to fire again
 * after the debounce window expires.
 */
class KeyboardDebounceTest {

    /**
     * Simulates the debouncing logic from MainWindow.dispatchKeyboardAction()
     * Returns true if the action should fire, false if it should be debounced.
     */
    private boolean shouldFireAction(String actionId, java.util.Map<String, Long> lastActionExecutionTime, long debounceMs) {
        long now = System.currentTimeMillis();
        Long lastExecution = lastActionExecutionTime.get(actionId);
        
        if (lastExecution != null && (now - lastExecution) < debounceMs) {
            // Action fired too recently, skip this keystroke (key repeat)
            return false;
        }
        
        // Update execution time for this action
        lastActionExecutionTime.put(actionId, now);
        return true;
    }

    @Test
    void singleActionDoesNotFireTwiceWithinDebounceWindow() {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 50;
        
        // First fire should succeed
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Immediate second fire should be debounced
        assertFalse(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        assertFalse(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
    }

    @Test
    void differentActionsCanFireImmediately() {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 50;
        
        // Fire first action
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Different action should fire immediately (not debounced)
        assertTrue(shouldFireAction("stop", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Another different action should also fire immediately
        assertTrue(shouldFireAction("pause", lastActionExecutionTime, DEBOUNCE_MS));
    }

    @Test
    void sameActionCanFireAgainAfterDebounceWindowExpires() throws InterruptedException {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 50;
        
        // First fire
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Immediate second fire is debounced
        assertFalse(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Wait for debounce window to expire
        Thread.sleep(DEBOUNCE_MS + 10);
        
        // Now it should fire again
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
    }

    @Test
    void multipleRapidPressesDebounceCorrectly() {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 50;
        
        AtomicInteger fireCount = new AtomicInteger(0);
        
        // Simulate 10 rapid key presses of the same action
        for (int i = 0; i < 10; i++) {
            if (shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS)) {
                fireCount.incrementAndGet();
            }
        }
        
        // Only the first press should fire, all others should be debounced
        assertEquals(1, fireCount.get());
    }

    @Test
    void perActionDebounceDoesNotAffectOtherActions() throws InterruptedException {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 50;
        
        // Fire go action multiple times
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        assertFalse(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Stop action should fire immediately even though go is debounced
        assertTrue(shouldFireAction("stop", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Wait for stop debounce window to expire
        Thread.sleep(DEBOUNCE_MS + 10);
        
        // Stop can fire again
        assertTrue(shouldFireAction("stop", lastActionExecutionTime, DEBOUNCE_MS));
        
        // Go is still debounced (or may have expired by now)
        // Let's check after additional wait
        Thread.sleep(10);
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
    }

    @Test
    void debounceMapTracksMultipleActionsIndependently() {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 50;
        
        // Verify map starts empty
        assertTrue(lastActionExecutionTime.isEmpty());
        
        // Fire multiple actions
        shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS);
        shouldFireAction("stop", lastActionExecutionTime, DEBOUNCE_MS);
        shouldFireAction("pause", lastActionExecutionTime, DEBOUNCE_MS);
        
        // Map should contain all three actions
        assertEquals(3, lastActionExecutionTime.size());
        assertTrue(lastActionExecutionTime.containsKey("go"));
        assertTrue(lastActionExecutionTime.containsKey("stop"));
        assertTrue(lastActionExecutionTime.containsKey("pause"));
    }

    @Test
    void debounceWithZeroTimeAllowsImmediateRefire() {
        java.util.Map<String, Long> lastActionExecutionTime = new java.util.HashMap<>();
        final long DEBOUNCE_MS = 0;
        
        // With 0ms debounce window, the condition (now - lastExecution) < 0 is never true,
        // so actions can fire on every call. This is an edge case for testing purposes.
        // Normal debounce is set to ACTION_DEBOUNCE_MS = 50 in production.
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
        
        // With 0ms window, second call fires immediately (condition is never satisfied)
        assertTrue(shouldFireAction("go", lastActionExecutionTime, DEBOUNCE_MS));
    }
}
