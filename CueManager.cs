using NAudio.Wave;
using Newtonsoft.Json;
using System.Collections.ObjectModel;
using System.IO;
using System.Windows;

namespace Win_Labs
{
    public class CueManager
    {
        /* Property: StartupFinished
         * Description: Indicates whether the startup process has finished.
         */
        internal static bool StartupFinished { get; private set; }

        /* Function: MarkStartupAsFinished
         * Description: Marks the startup process as finished by setting the StartupFinished property to true.
         */
        public static void MarkStartupAsFinished()
        {
            StartupFinished = true;
        }

        /* Property: ValidJsonFileInPlaylist
         * Description: Indicates whether a valid JSON file exists in the playlist folder.
         */
        public static bool ValidJsonFileInPlaylist { get; private set; }
        internal int numberOfCues;
        internal int currentCueCount;

        /* Function: LoadCues
         * Description: Loads all cues from the specified playlist folder path and returns them as an ObservableCollection.
         * Parameters:
         *   - playlistFolderPath: The path to the playlist folder containing cue files.
         * Returns: An ObservableCollection of Cue objects.
         */
        public ObservableCollection<Cue> LoadCues(string playlistFolderPath)
        {
            var cues = new ObservableCollection<Cue>();
            if (string.IsNullOrEmpty(playlistFolderPath))
            {
                Log.Warning("Playlist folder path is not set.");
                return cues;
            }
            var cueFiles = Directory.GetFiles(playlistFolderPath, "*.json");
            Log.Info($"Found {cueFiles.Length} cue files in {playlistFolderPath}.");
            try
            {
                numberOfCues = Directory.GetFiles(playlistFolderPath, "cue_*.json").Length;
                LoadingWindow loadingWindow = new LoadingWindow();
                loadingWindow.SetProgressBarMaximum(numberOfCues);

                foreach (var file in Directory.EnumerateFiles(playlistFolderPath, "cue_*.json"))
                {
                    currentCueCount++;
                    loadingWindow.SetProgressBarValue(currentCueCount);
                    var cue = JsonConvert.DeserializeObject<Cue>(File.ReadAllText(file));
                    cue.PlaylistFolderPath = playlistFolderPath;
                    cue.TargetFile = cue.GetAbsolutePath(cue.TargetFile); // Resolve to absolute path
                    cues.Add(cue);
                }
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
            }
            MarkStartupAsFinished();
            return cues;
        }

        /* Function: CreateNewCue
         * Description: Creates a new Cue object with default values and the specified cue number and playlist folder path.
         * Parameters:
         *   - cueNumber: The number of the new cue.
         *   - playlistFolderPath: The path to the playlist folder.
         * Returns: A new Cue object.
         */
        public static Cue CreateNewCue(int cueNumber, string playlistFolderPath)
        {
            return new Cue(playlistFolderPath)
            {
                CueNumber = cueNumber,
                PlaylistFolderPath = playlistFolderPath,
                CueName = $"Cue {cueNumber}",
                Duration = "00:00.00",
                PreWait = "00:00.00",
                AutoFollow = false,
                FileName = "",
                TargetFile = "",
                Notes = ""
            };
        }

        /* Function: SaveCueToFile
         * Description: Saves a Cue object to a file in the specified folder path.
         * Parameters:
         *   - cue: The Cue object to save.
         *   - folderPath: The folder path where the cue file will be saved.
         */
        public static void SaveCueToFile(Cue cue, string folderPath)
        {
            try
            {
                string filePath = Path.Combine(folderPath, $"cue_{cue.CueNumber}.json");
                File.WriteAllText(filePath, JsonConvert.SerializeObject(cue));
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
            }
        }

        /* Function: SaveAllCues
         * Description: Saves all Cue objects in the specified ObservableCollection to the specified playlist folder path.
         * Parameters:
         *   - cues: The collection of Cue objects to save.
         *   - playlistFolderPath: The folder path where the cue files will be saved.
         */
        public static void SaveAllCues(ObservableCollection<Cue> cues, string playlistFolderPath)
        {
            if (cues == null || string.IsNullOrEmpty(playlistFolderPath))
            {
                Log.Warning("Cues collection or playlist folder path is not set.");
                return;
            }

            foreach (var cue in cues)
            {
                SaveCueToFile(cue, playlistFolderPath);
            }
            Log.Info("All cues saved successfully.");
        }

        /* Function: DeleteCueFile
         * Description: Deletes the file associated with the specified Cue object from the specified playlist folder path.
         * Parameters:
         *   - cue: The Cue object whose file will be deleted.
         *   - playlistFolderPath: The folder path where the cue file is located.
         */
        public static void DeleteCueFile(Cue cue, string playlistFolderPath)
        {
            var filePath = GetCueFilePath(cue, playlistFolderPath);

            if (File.Exists(filePath))
            {
                try
                {
                    File.Delete(filePath);
                    Log.Info($"Deleted cue file: {MaskFilePath(filePath)}");
                }
                catch (Exception ex)
                {
                    Log.Error($"Failed to delete cue file {MaskFilePath(filePath)}: {ex.Message}");
                }
            }
            else
            {
                Log.Warning("Cue file not found for deletion.");
            }
        }

        /* Function: IsValidJsonFileInPlaylist
         * Description: Checks if there is a valid JSON file in the specified playlist folder path.
         * Parameters:
         *   - playlistFolderPath: The path to the playlist folder.
         * Returns: True if a valid JSON file exists; otherwise, false.
         */
        public static bool IsValidJsonFileInPlaylist(string playlistFolderPath)
        {
            if (string.IsNullOrEmpty(playlistFolderPath) || !Directory.Exists(playlistFolderPath))
            {
                Log.Warning("Invalid playlist folder path.");
                return false;
            }

            var jsonFiles = Directory.GetFiles(playlistFolderPath, "*.json");
            foreach (var file in jsonFiles)
            {
                if (DeserializeCue(file) != null)
                {
                    ValidJsonFileInPlaylist = true;
                    return true;
                }
            }

            ValidJsonFileInPlaylist = false;
            return false;
        }

        /* Function: GetCueFilePath
         * Description: Constructs the file path for a Cue object based on its cue number and the specified folder path.
         * Parameters:
         *   - cue: The Cue object.
         *   - folderPath: The folder path where the cue file is located.
         * Returns: The file path for the Cue object.
         */
        private static string GetCueFilePath(Cue cue, string folderPath) =>
            Path.Combine(folderPath, $"cue_{cue.CueNumber}.json");

        /* Function: DeserializeCue
         * Description: Deserializes a Cue object from a JSON file. If the file is missing or invalid, a default Cue object is returned.
         * Parameters:
         *   - filePath: The path to the JSON file.
         * Returns: A Cue object or a default Cue if deserialization fails.
         */
        private static Cue DeserializeCue(string filePath)
        {
            try
            {
                if (!File.Exists(filePath))
                {
                    Log.Warning($"Cue file not found: {MaskFilePath(filePath)}");

                    // Ask the user whether they want to abort or create a default cue
                    var result = System.Windows.MessageBox.Show("Cue file not found. Do you want to create a default cue?", "Cue File Missing", MessageBoxButton.YesNo, MessageBoxImage.Question);

                    if (result == MessageBoxResult.No)
                    {
                        return null; // Abort and return null if the user chooses not to create a default cue
                    }
                    else
                    {
                        return new Cue(); // Return a default Cue if the file is missing and the user chooses to create one
                    }
                }

                var json = File.ReadAllText(filePath);
                var cue = JsonConvert.DeserializeObject<Cue>(json);

                if (cue == null)
                {
                    Log.Warning($"Failed to deserialize cue: JSON resulted in a null object from {MaskFilePath(filePath)}");
                    return new Cue(); // Return a default Cue if deserialization failed
                }
                Log.Info("Successfully Deserialized Cue");
                return cue;
            }
            catch (JsonException jsonEx)
            {
                Log.Error($"Invalid JSON format in file {MaskFilePath(filePath)}: {jsonEx.Message}");
                return new Cue(); // Return a default Cue on JSON error
            }
            catch (IOException ioEx)
            {
                Log.Error($"File access error for {MaskFilePath(filePath)}: {ioEx.Message}");
                return new Cue(); // Return a default Cue on file access error
            }
            catch (Exception ex)
            {
                Log.Error($"Unexpected error deserializing cue from {MaskFilePath(filePath)}: {ex.Message}");
                return new Cue(); // Return a default Cue for any other errors
            }
        }

        /* Function: MaskFilePath
         * Description: Masks a file path to limit its length for logging purposes.
         * Parameters:
         *   - filePath: The file path to mask.
         * Returns: A masked version of the file path.
         */
        private static string MaskFilePath(string filePath)
        {
            const int maxLength = 30;
            return filePath.Length > maxLength
                ? $"...{filePath[^maxLength..]}"
                : filePath;
        }
    }
}
