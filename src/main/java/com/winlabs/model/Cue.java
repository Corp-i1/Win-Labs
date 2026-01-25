package com.winlabs.model;

import javafx.beans.property.*;
import java.util.Objects;

/*
     * TODO: Consider using a Builder pattern for more complex construction.
     * TODO: Add Javadoc comments for parameters.
     * TODO: Add unit tests for this constructor.
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
        this();
        setNumber(number);
        setName(name);
        setFilePath(filePath);
    }
    
    /**
     * Creates a new Cue with basic audio properties.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param duration the audio duration in seconds
     */
    public Cue(int number, String name, String filePath, double duration) {
        this(number, name, filePath);
        setDuration(duration);
    }
    
    /**
     * Creates a new Cue with timing properties.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param preWait the pre-wait time in seconds
     * @param postWait the post-wait time in seconds
     */
    public Cue(int number, String name, String filePath, double preWait, double postWait) {
        this(number, name, filePath);
        setPreWait(preWait);
        setPostWait(postWait);
    }
    
    /**
     * Creates a new Cue with timing and auto-follow properties.
     * 
     * @param number the cue number
     * @param name the cue name
     * @param filePath the path to the audio file
     * @param preWait the pre-wait time in seconds
     * @param postWait the post-wait time in seconds
     * @param autoFollow whether to automatically follow to the next cue
     */
    public Cue(int number, String name, String filePath, double preWait, double postWait, boolean autoFollow) {
        this(number, name, filePath, preWait, postWait);
        setAutoFollow(autoFollow);
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
        this(number, name, filePath, preWait, postWait, autoFollow);
        setDuration(duration);
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
