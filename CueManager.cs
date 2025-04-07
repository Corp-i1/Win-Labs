using NAudio.Wave;
using Newtonsoft.Json;
using System.Collections.ObjectModel;
using System.IO;
using System.Windows;

namespace Win_Labs
{
        public class CueManager
    {
         internal static bool StartupFinished { get; private set; }

        public static void MarkStartupAsFinished()
        {
            StartupFinished = true;
        }
        public static bool ValidJsonFileInPlaylist { get; private set; }
        internal int numberOfCues;
        internal int currentCueCount;
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

        private static string GetCueFilePath(Cue cue, string folderPath) =>
            Path.Combine(folderPath, $"cue_{cue.CueNumber}.json");

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
                Log.Info("Sucessfully DeserialisedCue");
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


        private static string MaskFilePath(string filePath)
        {
            const int maxLength = 30;
            return filePath.Length > maxLength
                ? $"...{filePath[^maxLength..]}"
                : filePath;
        }
    }
}
