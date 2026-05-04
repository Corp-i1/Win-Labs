package com.winlabs.service;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.TextInputControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service for managing keyboard accelerators and multi-key sequences.
 * 
 * Handles:
 * - Parsing and registration of single-key and multi-key bindings
 * - Debouncing to prevent repeated action firings during key hold
 * - Multi-key sequence tracking with timeout handling
 * - Integration with JavaFX Scene for global key event filtering
 * 
 * Multi-key Sequences Behavior:
 * - First key MUST match exactly (including modifiers): User presses CTRL+A, system matches CTRL+A
 * - Subsequent keys match by KEY CODE ONLY (modifiers ignored): User presses B (even while holding CTRL),
 *   system matches just 'B' code. This is lenient to handle users who don't perfectly release modifiers
 *   between key presses.
 * - Timeout: 500ms between keys; if exceeded, sequence resets
 * 
 * Example: For binding "CTRL+S;F", user can:
 * 1. Press CTRL+S (exact match required)
 * 2. Keep holding CTRL and press F (lenient: F key code matches, CTRL is ignored)
 * 3. Action triggers after F is pressed
 */
public class KeyboardManager {
    private static final Logger logger = LoggerFactory.getLogger(KeyboardManager.class);

    // Configuration constants
    private static final long SEQUENCE_TIMEOUT_MS = 500; // Timeout between keys in multi-key sequence (milliseconds)
    private static final long ACTION_DEBOUNCE_MS = 50; // Minimum time between same action firings (milliseconds)

    // State tracking
    private final Map<String, List<KeyCombination>> multiKeySequences = new HashMap<>(); // actionId -> sequence
    private final Map<String, Integer> sequenceProgress = new HashMap<>(); // actionId -> how many keys matched
    private final Map<String, Long> sequenceTimestamps = new HashMap<>(); // actionId -> last key press time
    private final Map<String, Long> lastActionExecutionTime = new HashMap<>(); // actionId -> last execution time (for debounce)
    private final Set<KeyCombination> registeredAccelerators = new HashSet<>(); // Track accelerators we've added

    // Event handling
    private EventHandler<KeyEvent> acceleratorEventFilter = null;
    private Scene currentScene = null;

    // Configuration
    private boolean allowKeyRepeat = false;
    private Consumer<String> actionExecutor = null; // Callback to execute actions

    public KeyboardManager() {
        logger.debug("KeyboardManager initialized");
    }

    /**
     * Sets the callback to execute when a keyboard action is completed.
     * This allows MainWindow to handle action execution while KeyboardManager handles key event dispatch.
     *
     * @param executor a Consumer that accepts action IDs and executes the corresponding action
     */
    public void setActionExecutor(Consumer<String> executor) {
        this.actionExecutor = executor;
    }

    /**
     * Sets whether to allow key repeat (continuous firing while key is held).
     *
     * @param allow true to allow repeats, false to debounce
     */
    public void setAllowKeyRepeat(boolean allow) {
        this.allowKeyRepeat = allow;
    }

    /**
     * Registers keyboard accelerators from application settings.
     * Parses stored key bindings and registers them on the scene for global key dispatch.
     *
     * @param scene the JavaFX scene to register accelerators on
     * @param keyBindings a map of action IDs to binding strings (e.g., "CTRL+S" or "CTRL+A;B")
     */
    public void registerKeyboardAccelerators(Scene scene, Map<String, String> keyBindings) {
        if (scene == null) {
            logger.warn("Cannot register keyboard accelerators: scene is null");
            return;
        }

        if (keyBindings == null || keyBindings.isEmpty()) {
            logger.debug("No keyboard bindings configured");
            return;
        }

        this.currentScene = scene;

        // Clear previous state
        clearMultiKeySequenceState();
        final Map<KeyCombination, String> actionByKeyCombination = new HashMap<>();

        // Parse all bindings
        for (Map.Entry<String, String> configuredBinding : keyBindings.entrySet()) {
            String actionId = configuredBinding.getKey();
            String bindingStr = configuredBinding.getValue();

            if (bindingStr == null || bindingStr.isEmpty()) {
                continue;
            }

            try {
                // Check if this is a multi-key sequence (contains semicolon)
                if (bindingStr.contains(";")) {
                    // Parse as multi-key sequence
                    List<KeyCombination> sequence = KeyBindingService.parseMultiKeySequence(bindingStr);
                    multiKeySequences.put(actionId, sequence);
                    sequenceProgress.put(actionId, 0); // Start at 0 keys matched
                    logger.debug(
                        "Registered multi-key sequence for '{}': {} (sequence: {} keys: {})",
                        actionId, bindingStr, sequence.size(),
                        sequence.stream().map(KeyCombination::getName).toList()
                    );
                } else {
                    // Parse as single key binding
                    KeyCombination binding = KeyBindingService.parseBinding(bindingStr);
                    String existingActionId = actionByKeyCombination.get(binding);
                    if (existingActionId != null) {
                        logger.warn(
                            "Duplicate keyboard shortcut {} for action {} conflicts with existing action {}; ignoring duplicate binding",
                            bindingStr, actionId, existingActionId
                        );
                        continue;
                    }
                    actionByKeyCombination.put(binding, actionId);
                    logger.debug("Registered single keyboard shortcut for '{}': {}", actionId, bindingStr);
                }
            } catch (IllegalArgumentException e) {
                logger.error("Failed to parse keyboard binding for '{}': {} ({})", actionId, bindingStr, e.getMessage());
            }
        }

        if (actionByKeyCombination.isEmpty() && multiKeySequences.isEmpty()) {
            logger.warn("No valid keyboard bindings were registered");
            return;
        }

        // Remove any previously registered accelerators to avoid duplicates
        clearPreviousAccelerators(scene);

        // Register single-key accelerators on the scene
        for (Map.Entry<KeyCombination, String> actionEntry : actionByKeyCombination.entrySet()) {
            KeyCombination keyCombination = actionEntry.getKey();
            String actionId = actionEntry.getValue();
            scene.getAccelerators().put(keyCombination, () -> handleAcceleratorAction(actionId));
            registeredAccelerators.add(keyCombination);
            logger.debug("Registered accelerator for action '{}' -> {}", actionId, keyCombination.getName());
        }

        // Create a copy for the event filter lambda
        final Map<KeyCombination, String> finalActionByKeyCombination = actionByKeyCombination;

        // Add a capturing-level event filter to handle both single and multi-key sequences
        attachEventFilter(scene, finalActionByKeyCombination);
    }

    /**
     * Clears all registered keyboard accelerators and internal state.
     * Call this when unloading or switching scenes.
     */
    public void clearAccelerators() {
        if (currentScene != null) {
            clearPreviousAccelerators(currentScene);
        }
        clearMultiKeySequenceState();
        lastActionExecutionTime.clear();
        if (currentScene != null && acceleratorEventFilter != null) {
            try {
                currentScene.removeEventFilter(KeyEvent.KEY_PRESSED, acceleratorEventFilter);
                acceleratorEventFilter = null;
            } catch (Exception ex) {
                logger.debug("Error removing event filter: {}", ex.getMessage());
            }
        }
        currentScene = null;
        logger.debug("Keyboard accelerators cleared");
    }

    /**
     * Handles an accelerator-invoked action. Respects the allowKeyRepeat setting and applies
     * per-action debouncing when repeats are disabled.
     *
     * @param actionId the ID of the action to execute
     */
    private void handleAcceleratorAction(String actionId) {
        if (actionId == null || actionId.isEmpty()) {
            return;
        }

        try {
            logger.debug("Accelerator invoked: {}", actionId);

            if (!allowKeyRepeat) {
                // Apply debouncing: check if enough time has passed since last execution
                Long lastExecution = lastActionExecutionTime.get(actionId);
                long now = System.currentTimeMillis();

                if (lastExecution != null && (now - lastExecution) < ACTION_DEBOUNCE_MS) {
                    logger.trace(
                        "Skipping action '{}' due to debounce ({} ms since last)",
                        actionId, (now - lastExecution)
                    );
                    // Skip repeated firing while key is held
                    return;
                }
                lastActionExecutionTime.put(actionId, now);
            }

            executeAction(actionId);
        } catch (Exception e) {
            logger.error("Error handling accelerator action {}: {}", actionId, e.getMessage(), e);
        }
    }

    /**
     * Executes an action by delegating to the registered action executor.
     *
     * @param actionId the ID of the action to execute
     */
    private void executeAction(String actionId) {
        if (actionExecutor == null) {
            logger.warn("No action executor registered; cannot execute action: {}", actionId);
            return;
        }
        actionExecutor.accept(actionId);
    }

    /**
     * Clears the previous accelerators from the scene.
     */
    private void clearPreviousAccelerators(Scene scene) {
        try {
            for (KeyCombination keyCombination : registeredAccelerators) {
                scene.getAccelerators().remove(keyCombination);
            }
            logger.debug("Cleared {} previously registered keyboard accelerators", registeredAccelerators.size());
        } catch (Exception ex) {
            logger.warn("Error clearing previous accelerators: {}", ex.getMessage());
        }
        registeredAccelerators.clear();
    }

    /**
     * Clears all multi-key sequence tracking state.
     */
    private void clearMultiKeySequenceState() {
        multiKeySequences.clear();
        sequenceProgress.clear();
        sequenceTimestamps.clear();
    }

    /**
     * Attaches the event filter to the scene to handle key events.
     */
    private void attachEventFilter(Scene scene, Map<KeyCombination, String> actionByKeyCombination) {
        // Remove any previously attached event filter
        try {
            if (acceleratorEventFilter != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, acceleratorEventFilter);
            }
        } catch (Exception ex) {
            logger.debug("Could not remove previous event filter: {}", ex.getMessage());
        }

        // Create and attach the new event filter
        acceleratorEventFilter = keyEvent -> handleKeyEvent(keyEvent, actionByKeyCombination);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, acceleratorEventFilter);
    }

    /**
     * Handles a KEY_PRESSED event, checking both single-key bindings and multi-key sequences.
     */
    private void handleKeyEvent(KeyEvent keyEvent, Map<KeyCombination, String> actionByKeyCombination) {
        // Ignore if target is a text input control to not interfere with typing
        if (keyEvent.getTarget() instanceof TextInputControl) {
            return;
        }

        // First, check single-key bindings (capture phase)
        for (Map.Entry<KeyCombination, String> actionEntry : actionByKeyCombination.entrySet()) {
            try {
                if (KeyBindingService.matchesKeyEvent(actionEntry.getKey(), keyEvent)) {
                    handleAcceleratorAction(actionEntry.getValue());
                    keyEvent.consume();
                    return;
                }
            } catch (Exception ex) {
                logger.debug("Error checking single-key binding: {}", ex.getMessage());
            }
        }

        // Then check multi-key sequences
        handleMultiKeySequence(keyEvent);
    }

    /**
     * Handles multi-key sequence detection and progression.
     */
    private void handleMultiKeySequence(KeyEvent keyEvent) {
        for (Map.Entry<String, List<KeyCombination>> seqEntry : multiKeySequences.entrySet()) {
            String actionId = seqEntry.getKey();
            List<KeyCombination> sequence = seqEntry.getValue();

            long now = System.currentTimeMillis();
            Integer progress = sequenceProgress.getOrDefault(actionId, 0);
            Long lastTimestamp = sequenceTimestamps.get(actionId);

            // Check if sequence has timed out
            if (lastTimestamp != null && (now - lastTimestamp) > SEQUENCE_TIMEOUT_MS) {
                logger.debug(
                    "Multi-key sequence timeout for action '{}' (timed out after {} ms)",
                    actionId, now - lastTimestamp
                );
                progress = 0; // Reset progress
            }

            // Check if current key matches the next key in the sequence
            if (progress < sequence.size()) {
                try {
                    KeyCombination expectedKey = sequence.get(progress);
                    // For first key (progress=0), match with modifiers. For subsequent keys, match key code only
                    // because users may still have modifiers held from previous key press
                    boolean matches = (progress == 0)
                        ? KeyBindingService.matchesKeyEvent(expectedKey, keyEvent)
                        : KeyBindingService.matchesKeyCodeOnly(expectedKey, keyEvent);

                    logger.debug(
                        "Multi-key sequence '{}': checking key {} of {}, expected: {}, matches: {}",
                        actionId, progress, sequence.size(), expectedKey.getName(), matches
                    );

                    if (matches) {
                        progress++;
                        sequenceTimestamps.put(actionId, now);
                        sequenceProgress.put(actionId, progress);

                        // Check if sequence is complete
                        if (progress == sequence.size()) {
                            logger.debug("Multi-key sequence COMPLETED for action: {}", actionId);
                            handleAcceleratorAction(actionId);
                            // Reset for next sequence
                            sequenceProgress.put(actionId, 0);
                            sequenceTimestamps.remove(actionId);
                        } else {
                            logger.debug(
                                "Multi-key sequence PROGRESS for {}: {} / {} keys matched",
                                actionId, progress, sequence.size()
                            );
                        }

                        keyEvent.consume();
                        return;
                    }
                } catch (Exception ex) {
                    logger.debug("Error checking multi-key sequence: {}", ex.getMessage(), ex);
                }
            }

            // If a key doesn't match the expected next key, check for restart
            if (progress > 0) {
                // But first check if this key could be the start of a new sequence
                try {
                    KeyCombination firstKey = sequence.get(0);
                    boolean isRestart = KeyBindingService.matchesKeyEvent(firstKey, keyEvent);
                    if (!isRestart) {
                        logger.debug("Multi-key sequence '{}': key didn't match, resetting progress", actionId);
                        sequenceProgress.put(actionId, 0);
                        sequenceTimestamps.remove(actionId);
                    }
                } catch (Exception ex) {
                    logger.debug("Error resetting sequence: {}", ex.getMessage());
                }
            }
        }
    }
}
