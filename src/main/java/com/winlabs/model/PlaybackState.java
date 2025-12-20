package com.winlabs.model;

/**
 * Represents the current state of audio playback.
 */
public enum PlaybackState {
    STOPPED,    // No audio playing, at position 0
    PLAYING,    // Audio is currently playing
    PAUSED,     // Audio is paused, can be resumed
    PRE_WAIT,   // Waiting before starting next cue
    POST_WAIT   // Waiting after cue finishes before auto-following
}
