using Microsoft.VisualBasic.ApplicationServices;
using Microsoft.VisualBasic.Logging;
using System;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Documents;
using System.Windows.Forms;
using Win_Labs.ZIPManagement;

namespace Win_Labs
{
    public class PlaylistManager
    {
        internal static string playlistFolderPath;
        private string playlistImportFilePath;
        private string playlistFolderPathTemp;
        private Window _startupWindow;

        /* Constructor: PlaylistManager
         * Description: Initializes a new instance of the PlaylistManager class and sets the startup window.
         * Parameters:
         *   - startupWindow: The startup window to associate with the PlaylistManager.
         */
        public PlaylistManager(Window startupWindow)
        {
            _startupWindow = startupWindow;
            Log.Info($"PlaylistManager initialized with window: {_startupWindow.GetType().Name}");
        }

        /* Function: OpenMainWindow
         * Description: Opens the MainWindow with the specified playlist folder path and closes the startup window.
         * Parameters:
         *   - playlistFolderPath: The path to the playlist folder to open.
         */
        private void OpenMainWindow(string playlistFolderPath)
        {
            var mainWindow = new MainWindow(playlistFolderPath);
            Log.Info("MainWindow created and initialized.");
            CueManager.MarkStartupAsFinished();
            Log.Info("Startup process completed.");
            mainWindow.Show();
            if (_startupWindow is StartupWindow startupWindow)
            {
                startupWindow.StartupWindowClosing = true;
            }
            _startupWindow.Close();
        }

        /* Function: CreateNewPlaylist
         * Description: Creates a new playlist by selecting a folder, closing the current MainWindow, and opening a new MainWindow.
         */
        public void CreateNewPlaylist()
        {
            string playlistFolderPath = OpenFolderDialog("Select a folder to create a playlist in.");
            if (!string.IsNullOrEmpty(playlistFolderPath))
            {
                try
                {
                    Log.Info("Show loading window");
                    var loadingWindow = new LoadingWindow();
                    loadingWindow.Show();
                    Log.Info("Closing Old Main Window");
                    var mainWindow = System.Windows.Application.Current.Windows.OfType<MainWindow>().FirstOrDefault();
                    if (mainWindow != null)
                    {
                        MainWindow.CloseMainWindow();
                    }
                    else
                    {
                        Log.Info("No MainWindow instance found to close.");
                    }
                    Log.Info($"New playlist folder selected: {playlistFolderPath}");
                    OpenMainWindow(playlistFolderPath);

                    Log.Info("Close loading window");
                    loadingWindow.Close();
                }
                catch (Exception ex)
                {
                    Log.Error($"Failed to create new playlist: {ex.Message}");
                    System.Windows.MessageBox.Show($"Error creating new playlist: {ex.Message}", "Create Error", MessageBoxButton.OK, MessageBoxImage.Error);
                }
            }
        }
         /* Function: OpenExistingPlaylist
         * Description: Opens an existing playlist by selecting a folder and validating the presence of cue files.
         */
        public void OpenExistingPlaylist()
        {
            string playlistFolderPath = OpenFolderDialog("Select a playlist folder.");
            Log.Info($"Opening existing playlist folder: {playlistFolderPath}");

            try
            {
                if (!Directory.EnumerateFiles(playlistFolderPath, "cue_*.json").Any())
                {
                    Log.Error("No cue files found in the selected folder: " + playlistFolderPath);
                    System.Windows.MessageBox.Show("The selected folder does not contain any cue files.", "Error", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }
            }
            catch
            {
                Log.Error("No folder selected.");
                return;
            }

            try
            {
                Log.Info("Show loading window");
                var loadingWindow = new LoadingWindow();
                loadingWindow.Show();
                Log.Info("Closing Old Main Window");
                var mainWindow = System.Windows.Application.Current.Windows.OfType<MainWindow>().FirstOrDefault();
                if (mainWindow != null)
                {
                    MainWindow.CloseMainWindow();
                }
                else
                {
                    Log.Info("No MainWindow instance found to close.");
                }
                Log.Info("Open new Main Window.");
                OpenMainWindow(playlistFolderPath);

                Log.Info("Close loading window");
                loadingWindow.Close();
            }
            catch (Exception ex)
            {
                Log.Error($"Failed to open existing playlist: {ex.Message}");
                System.Windows.MessageBox.Show($"Error opening existing playlist: {ex.Message}", "Open Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        /* Function: ImportPlaylist
         * Description: Imports a playlist from a ZIP file to a selected directory, purging existing cue files if necessary.
         */
        public void ImportPlaylist()
        {
            var openFileDialog = new System.Windows.Forms.OpenFileDialog
            {
                Filter = "Zip files (*.zip)|*.zip",
                Title = "Select the playlist you want to import."
            };

            if (openFileDialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                playlistImportFilePath = openFileDialog.FileName;
                Log.Info($"Selected import file: {playlistImportFilePath}");

                string playlistFolderPath = OpenFolderDialog("Select the directory you want to import to.");
                if (!string.IsNullOrEmpty(playlistFolderPath))
                {
                    playlistFolderPathTemp = playlistFolderPath;
                    // Ensure the directory exists
                    PurgeExistingCueFiles();
                    Log.Info($"Importing playlist to: {playlistFolderPath}");

                    TryImportPlaylist();
                }
            }
        }

        /* Function: TryImportPlaylist
         * Description: Attempts to import a playlist from a ZIP file to the specified directory and opens the MainWindow.
         */
        private void TryImportPlaylist()
        {
            try
            {
                playlistFolderPath = playlistFolderPathTemp;
                Log.Info("Show loading window");
                var loadingWindow = new LoadingWindow();
                loadingWindow.Show();

                Log.Info("Import the playlist");
                import.openZIP(playlistImportFilePath, playlistFolderPath);
                Log.Info($"Playlist imported successfully from {playlistImportFilePath} to {playlistFolderPath}.");

                Log.Info("Close Startup window");
                if (_startupWindow is StartupWindow startupWindow)
                {
                    startupWindow.StartupWindowClosing = true;
                    _startupWindow.Close();
                }
                Log.Info("Closing Old Main Window");
                var mainWindow = System.Windows.Application.Current.Windows.OfType<MainWindow>().FirstOrDefault();
                if (mainWindow != null)
                {
                    MainWindow.CloseMainWindow();
                }
                else
                {
                    Log.Info("No MainWindow instance found to close.");
                }
                Log.Info("Open new main window with the new playlist");
                var newMainWindow = new MainWindow(playlistFolderPath);
                newMainWindow.Show();

                Log.Info("Close loading window");
                loadingWindow.Close();
            }
            catch (Exception ex)
            {
                Log.Error($"Failed to import playlist: {ex.Message}");
                Log.Exception(ex);
                System.Windows.MessageBox.Show($"Error importing playlist: {ex.Message}", "Import Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        /* Function: PurgeExistingCueFiles
         * Description: Deletes existing cue files in the destination folder if the user confirms the action.
         */
        private void PurgeExistingCueFiles()
        {
            try
            {
                var folderName = Path.GetFileNameWithoutExtension(playlistImportFilePath);
                var destinationPath = Path.Combine(playlistFolderPathTemp, folderName);
                if (destinationPath != null)
                {
                    return;
                }
                if (Directory.EnumerateFiles(destinationPath, "cue_*.json").Any())
                {
                    var cueFiles = Directory.EnumerateFiles(destinationPath, "cue_*.json").ToList();
                    var MssgBox = System.Windows.MessageBox.Show($"Found {cueFiles.Count} cue files in {destinationPath}. Do you want to delete them?", "Purge Cue Files?", MessageBoxButton.YesNo, MessageBoxImage.Warning);
                    if (MssgBox == MessageBoxResult.No)
                    {
                        Log.Info("No files deleted");
                        return;
                    }

                    foreach (var file in cueFiles)
                    {
                        File.Delete(file);
                        Log.Info($"Deleted cue file: {file}");
                    }
                    Log.Info("All existing cue files have been purged.");
                }
                else
                {
                    Log.Info("No cue files found to purge.");
                }
            }
            catch (Exception ex)
            {
                Log.Error($"Failed to purge existing cue files: {ex.Message}");
                System.Windows.MessageBox.Show($"Error purging cue files: {ex.Message}", "Purge Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        /* Function: OpenFolderDialog
         * Description: Opens a folder browser dialog for the user to select a directory.
         * Parameters:
         *   - description: The description to display in the folder browser dialog.
         * Returns: The selected folder path, or an empty string if no folder is selected.
         */
        private string OpenFolderDialog(string description)
        {
            var folderDialog = new FolderBrowserDialog
            {
                Description = description
            };

            return folderDialog.ShowDialog() == DialogResult.OK ? folderDialog.SelectedPath : string.Empty;
        }

        /* Function: ExportPlaylist
         * Description: Exports the current playlist to a ZIP file in a user-selected destination folder.
         * Parameters:
         *   - _playlistFolderPath: The path to the playlist folder to export.
         */
        public void ExportPlaylist(string _playlistFolderPath)
        {
            Log.Info("Export menu item clicked.");
            var folderDialog = new System.Windows.Forms.FolderBrowserDialog
            {
                Description = "Select a destination folder for the exported playlist."
            };

            if (folderDialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                string destinationPath = folderDialog.SelectedPath;
                try
                {
                    Log.Info("Show loading window");
                    var loadingWindow = new LoadingWindow();
                    loadingWindow.Show();

                    Log.Info("Exporting playlist");
                    export.createZIP(_playlistFolderPath, destinationPath);
                    Log.Info($"Playlist exported successfully to {destinationPath}.");

                    Log.Info("Close loading window");
                    loadingWindow.Close();
                }
                catch (Exception ex)
                {
                    Log.Error($"Error exporting playlist: {ex.Message}");
                    System.Windows.MessageBox.Show($"Failed to export playlist: {ex.Message}", "Export Error", MessageBoxButton.OK, MessageBoxImage.Error);
                }
            }
            var mainWindow = System.Windows.Application.Current.Windows.OfType<MainWindow>().FirstOrDefault();
            if (mainWindow != null)
            {
                mainWindow.RefreshCueList();
            }
            else
            {
                Log.Warning("!!! No MainWindow instance found. !!! \n \n Please create a bug report and upload your log file to GitHub. \n \n !!! No MainWindow instance found. !!! ");
            }
        }
    }
}
