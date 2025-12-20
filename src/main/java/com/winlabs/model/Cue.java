package com.winlabs.model;

import javafx.beans.property.*;

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
     */
    public Cue(int number, String name, String filePath) {
        this();
        setNumber(number);
        setName(name);
        setFilePath(filePath);
    }
    
    // Number property
    public int getNumber() {
        return number.get();
    }
    
    public void setNumber(int value) {
        number.set(value);
    }
    
    public IntegerProperty numberProperty() {
        return number;
    }
    
    // Name property
    public String getName() {
        return name.get();
    }
    
    public void setName(String value) {
        name.set(value);
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    // Duration property
    public double getDuration() {
        return duration.get();
    }
    
    public void setDuration(double value) {
        duration.set(value);
    }
    
    public DoubleProperty durationProperty() {
        return duration;
    }
    
    // PreWait property
    public double getPreWait() {
        return preWait.get();
    }
    
    public void setPreWait(double value) {
        preWait.set(value);
    }
    
    public DoubleProperty preWaitProperty() {
        return preWait;
    }
    
    // PostWait property
    public double getPostWait() {
        return postWait.get();
    }
    
    public void setPostWait(double value) {
        postWait.set(value);
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
        filePath.set(value);
    }
    
    public StringProperty filePathProperty() {
        return filePath;
    }
    
    @Override
    public String toString() {
        return String.format("Cue #%d: %s (%s)", getNumber(), getName(), getFilePath());
    }
}
