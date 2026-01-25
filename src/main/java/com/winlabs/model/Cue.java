package com.winlabs.model;

import javafx.beans.property.*;
import java.util.Objects;

/*
     * TODO: Add unit tests covering all Cue factory methods and initialization paths.
 */
/**
 * Represents a single cue in the playlist.
 * Uses JavaFX properties for automatic UI binding.
 */
public class Cue {
    private final IntegerProperty number;
    private final StringProperty name;
    private final DoubleProperty duration; // in seconds
    private final DoubleProperty preWait;  // in seconds
    private final DoubleProperty postWait; // in seconds
    private final BooleanProperty autoFollow;
    private final StringProperty filePath;
    
    /**
     * Creates a new Cue with default values.
     */
    public Cue() {
        this.number = new SimpleIntegerProperty(0);
        this.name = new SimpleStringProperty("");
        this.duration = new SimpleDoubleProperty(0.0);
        this.preWait = new SimpleDoubleProperty(0.0);
        this.postWait = new SimpleDoubleProperty(0.0);
        this.autoFollow = new SimpleBooleanProperty(false);
        this.filePath = new SimpleStringProperty("");
    }
    
    /**
     * Creates a new Cue with specified values.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     */
    public Cue(int number, String name, String filePath) {
        this(number, name, filePath, 0.0, 0.0, 0.0, false);
    }
    
    /**
     * Creates a new Cue with specified duration.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param duration the audio duration in seconds
     * @return a new Cue with the specified duration
     */
    public static Cue withDuration(int number, String name, String filePath, double duration) {
        return new Cue(number, name, filePath, duration, 0.0, 0.0, false);
    }
    
    /**
     * Creates a new Cue with specified timing properties.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param preWait the pre-wait time in seconds
     * @param postWait the post-wait time in seconds
     * @return a new Cue with the specified timing properties
     */
    public static Cue withTiming(int number, String name, String filePath, double preWait, double postWait) {
        return new Cue(number, name, filePath, 0.0, preWait, postWait, false);
    }
    
    /**
     * Creates a new Cue with specified timing and auto-follow properties.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param preWait the pre-wait time in seconds
     * @param postWait the post-wait time in seconds
     * @param autoFollow whether to automatically follow to the next cue
     * @return a new Cue with the specified timing and auto-follow properties
     */
    public static Cue withTimingAndAutoFollow(int number, String name, String filePath, double preWait, double postWait, boolean autoFollow) {
        return new Cue(number, name, filePath, 0.0, preWait, postWait, autoFollow);
    }
    
    /**
     * Creates a new Cue with all properties specified.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param duration the audio duration in seconds
     * @param preWait the pre-wait time in seconds
     * @param postWait the post-wait time in seconds
     * @param autoFollow whether to automatically follow to the next cue
     */
    public Cue(int number, String name, String filePath, double duration, double preWait, double postWait, boolean autoFollow) {
        this();
        setNumber(number);
        setName(name);
        setFilePath(filePath);
        setDuration(duration);
        setPreWait(preWait);
        setPostWait(postWait);
        setAutoFollow(autoFollow);
    }
    
    // Number property
    public int getNumber() {
        return number.get();
    }
    
    public void setNumber(int value) {
        number.set(requireNonNegative(value, "number"));
    }
    
    public IntegerProperty numberProperty() {
        return number;
    }
    
    // Name property
    public String getName() {
        return name.get();
    }
    
    public void setName(String value) {
        name.set(requireNonNull(value, "name"));
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    // Duration property
    public double getDuration() {
        return duration.get();
    }
    
    public void setDuration(double value) {
        duration.set(requireNonNegative(value, "duration"));
    }
    
    public DoubleProperty durationProperty() {
        return duration;
    }
    
    // PreWait property
    public double getPreWait() {
        return preWait.get();
    }
    
    public void setPreWait(double value) {
        preWait.set(requireNonNegative(value, "preWait"));
    }
    
    public DoubleProperty preWaitProperty() {
        return preWait;
    }
    
    // PostWait property
    public double getPostWait() {
        return postWait.get();
    }
    
    public void setPostWait(double value) {
        postWait.set(requireNonNegative(value, "postWait"));
    }
    
    public DoubleProperty postWaitProperty() {
        return postWait;
    }
    
    // AutoFollow property
    public boolean isAutoFollow() {
        return autoFollow.get();
    }
    
    public void setAutoFollow(boolean value) {
        autoFollow.set(value);
    }
    
    public BooleanProperty autoFollowProperty() {
        return autoFollow;
    }
    
    // FilePath property
    public String getFilePath() {
        return filePath.get();
    }
    
    public void setFilePath(String value) {
        filePath.set(requireNonNull(value, "filePath"));
    }
    
    public StringProperty filePathProperty() {
        return filePath;
    }
    
    @Override
    public String toString() {
        return String.format("Cue #%d: %s (%s)", getNumber(), getName(), getFilePath());
    }

    private int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    private double requireNonNegative(double value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    private String requireNonNull(String value, String field) {
        return Objects.requireNonNull(value, field + " cannot be null");
    }
}
