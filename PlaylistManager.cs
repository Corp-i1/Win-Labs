using System;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Forms;

namespace Win_Labs
{
    public class PlaylistManager
    {
        private string playlistFolderPath;
        private string playlistImportFilePath;
        private Window _startupWindow;

        public PlaylistManager(Window startupWindow)
        {
            _startupWindow = startupWindow;
        }

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

        public void CreateNewPlaylist()
        {
            string selectedPath = OpenFolderDialog("Select a folder to create a playlist in.");
            if (!string.IsNullOrEmpty(selectedPath))
            {
                playlistFolderPath = selectedPath;
                Log.Info($"New playlist folder selected: {playlistFolderPath}");
                OpenMainWindow(playlistFolderPath);
            }
        }

        public void OpenExistingPlaylist()
        {
            var folderDialog = new FolderBrowserDialog
            {
                Description = "Select a playlist folder."
            };

            if (folderDialog.ShowDialog() == DialogResult.OK)
            {
                string folderPath = folderDialog.SelectedPath;
                Log.Info($"Opening existing playlist folder: {folderPath}");
                if (!Directory.EnumerateFiles(folderPath, "cue_*.json").Any())
                {
                    Log.Error("No cue files found in the selected folder: " + folderPath);
                    System.Windows.MessageBox.Show("The selected folder does not contain any cue files.", "Error", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }

                OpenMainWindow(folderPath);
            }
        }

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

                string exportPath = OpenFolderDialog("Select the directory you want to import to.");
                if (!string.IsNullOrEmpty(exportPath))
                {
                    playlistFolderPath = exportPath;
                    Log.Info($"Importing playlist to: {playlistFolderPath}");
                    TryImportPlaylist();
                }
            }
        }

        private void TryImportPlaylist()
        {
            try
            {
                import.openZIP(playlistImportFilePath, playlistFolderPath);
                Log.Info($"Playlist imported successfully from {playlistImportFilePath} to {import.importFolderPath}.");
                OpenMainWindow(import.importFolderPath);
            }
            catch (Exception ex)
            {
                Log.Error($"Failed to import playlist: {ex.Message}");
                System.Windows.MessageBox.Show($"Error importing playlist: {ex.Message}", "Import Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private string OpenFolderDialog(string description)
        {
            var folderDialog = new FolderBrowserDialog
            {
                Description = description
            };

            return folderDialog.ShowDialog() == DialogResult.OK ? folderDialog.SelectedPath : string.Empty;
        }

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
                    export.createZIP(_playlistFolderPath, destinationPath);
                    Log.Info($"Playlist exported successfully to {destinationPath}.");
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
                Log.Warning("!!! No MainWindow instance found. !!! \n \n Please create a bug report and upload you log file to github. \n \n !!! No MainWindow instance found. !!! ");
            }
        }
    }
}
