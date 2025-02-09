using System.IO;
using System.IO.Compression;
using System.Windows;

namespace Win_Labs
{
    internal class export
    {
        public static void createZIP(string playlistFolderPath, string playlistExportFolderPath)
        {
            string playlistName = Path.GetFileName(playlistFolderPath) + ".zip";
            Log.Info("File name set to: " + playlistName);
            var zipFile = Path.Combine(playlistExportFolderPath, playlistName);
            Log.Info($"Export File Path: {zipFile}");

            if (string.IsNullOrEmpty(playlistFolderPath))
            {
                Log.Warning("Playlist folder path is not set.");
                return;
            }

            if (!Directory.Exists(playlistExportFolderPath))
            {
                Directory.CreateDirectory(playlistExportFolderPath);
                Log.Info("Created export folder as it did not exist.");
            }

            try
            {
                if (File.Exists(zipFile))
                {
                    var result = System.Windows.MessageBox.Show(
                        "File with the same name detected. Overwrite?",
                        "Overwrite File?",
                        MessageBoxButton.OKCancel,
                        MessageBoxImage.Warning);

                    if (result == MessageBoxResult.Cancel)
                    {
                        Log.Info("User canceled the overwrite.");
                        return;
                    }

                    File.Delete(zipFile);
                    Log.Info("Existing file deleted.");
                }

                System.IO.Compression.ZipFile.CreateFromDirectory(playlistFolderPath, zipFile, CompressionLevel.Fastest, false);
                Log.Info($"File created: {zipFile}");
                System.Windows.MessageBox.Show("Playlist exported successfully.", "Export Complete", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                Log.Error($"Error creating ZIP file: {ex.Message}");
                System.Windows.MessageBox.Show($"Could not create file {zipFile}. Please check the location or file permissions.",
                    "File Creation Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }
    }
}
