package com.winlabs;

import com.winlabs.view.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for Win-Labs application.
 * A cross-platform cue list manager for sound technicians.
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Create and show the main window
        MainWindow mainWindow = new MainWindow();
        mainWindow.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
