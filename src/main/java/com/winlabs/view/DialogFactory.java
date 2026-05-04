package com.winlabs.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.util.Optional;

/**
 * Factory for creating and configuring standard dialogs used throughout the application.
 * 
 * Centralizes dialog creation patterns to ensure consistency and reduce code duplication.
 * All dialogs are pre-configured with appropriate titles and behaviors.
 */
public final class DialogFactory {

    private DialogFactory() {
        // Utility class - no instantiation
    }

    /**
     * Creates a file chooser configured for selecting audio files.
     * 
     * @return a configured FileChooser ready for showOpenDialog()
     */
    public static FileChooser createAudioFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Audio File");
        
        // Add audio file extensions filter
        FileChooser.ExtensionFilter audioFilter = new FileChooser.ExtensionFilter(
            "Audio Files", "*.mp3", "*.wav", "*.aiff", "*.aac", "*.ogg", "*.flac", "*.m4a", "*.wma"
        );
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(
            "All Files", "*.*"
        );
        chooser.getExtensionFilters().addAll(audioFilter, allFilter);
        
        return chooser;
    }

    /**
     * Creates an error alert dialog.
     * 
     * @param title the title of the error dialog
     * @param message the error message to display
     * @return a configured error Alert
     */
    public static Alert createErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }

    /**
     * Creates a confirmation alert dialog.
     * 
     * @param title the title of the confirmation dialog
     * @param message the confirmation message to display
     * @return a configured confirmation Alert
     */
    public static Alert createConfirmationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }

    /**
     * Creates an information alert dialog.
     * 
     * @param title the title of the information dialog
     * @param message the information message to display
     * @return a configured information Alert
     */
    public static Alert createInformationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }

    /**
     * Creates a warning alert dialog.
     * 
     * @param title the title of the warning dialog
     * @param message the warning message to display
     * @return a configured warning Alert
     */
    public static Alert createWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }

    /**
     * Shows an error dialog and waits for user acknowledgment.
     * 
     * @param parentWindow the parent window to anchor the dialog to
     * @param title the title of the error dialog
     * @param message the error message to display
     */
    public static void showError(Window parentWindow, String title, String message) {
        Alert alert = createErrorAlert(title, message);
        alert.initOwner(parentWindow);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog and returns the user's response.
     * 
     * @param parentWindow the parent window to anchor the dialog to
     * @param title the title of the confirmation dialog
     * @param message the confirmation message to display
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirmation(Window parentWindow, String title, String message) {
        Alert alert = createConfirmationAlert(title, message);
        alert.initOwner(parentWindow);
        Optional<ButtonType> response = alert.showAndWait();
        return response.isPresent() && response.get() == ButtonType.OK;
    }

    /**
     * Shows an information dialog and waits for user acknowledgment.
     * 
     * @param parentWindow the parent window to anchor the dialog to
     * @param title the title of the information dialog
     * @param message the information message to display
     */
    public static void showInformation(Window parentWindow, String title, String message) {
        Alert alert = createInformationAlert(title, message);
        alert.initOwner(parentWindow);
        alert.showAndWait();
    }

    /**
     * Shows a warning dialog and waits for user acknowledgment.
     * 
     * @param parentWindow the parent window to anchor the dialog to
     * @param title the title of the warning dialog
     * @param message the warning message to display
     */
    public static void showWarning(Window parentWindow, String title, String message) {
        Alert alert = createWarningAlert(title, message);
        alert.initOwner(parentWindow);
        alert.showAndWait();
    }
}
