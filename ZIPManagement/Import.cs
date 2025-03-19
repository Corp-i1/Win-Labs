using System.IO.Compression;
using System.IO;
using System.Windows;
using Newtonsoft.Json;

namespace Win_Labs.ZIPManagement
{
    internal class import
    {
        internal static string destinationPath { get; private set; }
        internal static string importFolderPath { get; private set; }
        public static string openZIP(string playlistImportFilePath, string playlistFolderPath)
        {
            var folderName = Path.GetFileNameWithoutExtension(playlistImportFilePath);
            destinationPath = Path.Combine(playlistFolderPath, folderName);
            try
            {
                Log.Info($"Creating playlist at {playlistFolderPath}");

                // Check if the export directory exists
                if (!Directory.Exists(playlistFolderPath))
                {
                    Directory.CreateDirectory(playlistFolderPath);
                    Log.Info("Export path created: " + playlistFolderPath);
                }

                // Flags for overwrite/skip all options
                bool overwriteAll = false;
                bool skipAll = false;
                var CuePath = "";
                var directoryPath = Path.GetDirectoryName(CuePath);
                if (directoryPath != null)
                {
                    Directory.CreateDirectory(directoryPath);
                }

                // Extract files with overwrite handling
                Log.Info("Opening file: " + playlistImportFilePath);
                using (var archive = ZipFile.OpenRead(playlistImportFilePath))
                {
                    foreach (var entry in archive.Entries)
                    {
                        CuePath = Path.GetFullPath(Path.Combine(destinationPath, entry.FullName));
                        string fullDestDirPath = Path.GetFullPath(destinationPath + Path.DirectorySeparatorChar);
                        if (!CuePath.StartsWith(fullDestDirPath))
                        {
                            throw new InvalidOperationException("Entry is outside the target dir: " + CuePath);
                        }

                        // Check if the file already exists
                        if (File.Exists(CuePath))
                        {
                            if (skipAll)
                            {
                                Log.Info($"Skipped file (skip all): {entry.FullName}");
                                continue; // Skip all remaining duplicates
                            }

                            if (!overwriteAll)
                            {
                                var result = MessageBox.Show(
                                    $"The file '{entry.FullName}' already exists. Overwrite?",
                                    "File Exists",
                                    MessageBoxButton.YesNoCancel,
                                    MessageBoxImage.Warning);

                                if (result == MessageBoxResult.No)
                                {
                                    Log.Info($"Skipped file: {entry.FullName}");
                                    // Ask if this action should apply to all remaining duplicates
                                    var applyToAll = MessageBox.Show(
                                        "Apply this action to all remaining files?",
                                        "Apply to All",
                                        MessageBoxButton.YesNo,
                                        MessageBoxImage.Question);

                                    if (applyToAll == MessageBoxResult.Yes)
                                    {
                                        skipAll = true; // Set skip all flag
                                    }
                                    continue; // Skip this file
                                }
                                else if (result == MessageBoxResult.Cancel)
                                {
                                    Log.Info("User canceled extraction.");
                                    return null; // Stop extraction
                                }
                                else if (result == MessageBoxResult.Yes)
                                {
                                    // Ask if this action should apply to all remaining duplicates
                                    var applyToAll = MessageBox.Show(
                                        "Apply this action to all remaining files?",
                                        "Apply to All",
                                        MessageBoxButton.YesNo,
                                        MessageBoxImage.Question);

                                    if (applyToAll == MessageBoxResult.Yes)
                                    {
                                        overwriteAll = true; // Set overwrite all flag
                                    }
                                }
                            }
                        }
                        // Extract the file
                        entry.ExtractToFile(CuePath, overwriteAll); // Respect user's choice
                        Log.Info($"Extracted file: {entry.FullName}");
                    }
                }

                // Ensure paths in cue files are absolute
                MakePathsAbsolute(destinationPath);

                Log.Info($"{destinationPath} successfully populated with files from the ZIP archive.");
                return destinationPath;
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
                Log.Error($"Error extracting ZIP file: {ex.Message}");
                MessageBox.Show($"Could not open file {playlistImportFilePath}. Please check the location or permissions.",
                    "File Opening Error", MessageBoxButton.OK, MessageBoxImage.Warning);
                return null;
            }
        }

        private static void MakePathsAbsolute(string playlistFolderPath)
        {
            var cueFiles = Directory.GetFiles(playlistFolderPath, "cue_*.json");
            foreach (var file in cueFiles)
            {
                var cue = JsonConvert.DeserializeObject<Cue>(File.ReadAllText(file));
                if (cue != null)
                {
                    cue.TargetFile = cue.GetAbsolutePath(cue.TargetFile);
                    File.WriteAllText(file, JsonConvert.SerializeObject(cue));
                }
            }
        }
    }
}
