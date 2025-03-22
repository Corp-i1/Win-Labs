using NAudio.Dsp;
using NAudio.Wave;
using Newtonsoft.Json;
using System;
using System.ComponentModel;
using System.IO;
using System.Windows;

using System.Windows.Forms;

namespace Win_Labs
{
    public class Cue : INotifyPropertyChanged
    {
        private int _cueNumber;
        private string _cueFilePath;
        private string _duration;
        private string _preWait;
        private string _postWait;
        private bool _autoFollow;
        private string _fileName;
        private string _targetFile;
        private string _notes;
        private string _cueName;
        private bool _renaming;

        internal static bool IsInitializing { get; set; }
        internal string PlaylistFolderPath = PlaylistManager.playlistFolderPath;
        private TimeSpan _totalDuration;
        public event PropertyChangedEventHandler PropertyChanged;
        private static readonly object _lock = new object();

        public Cue(string playlistFolderPath)
        {
            PlaylistFolderPath = PlaylistManager.playlistFolderPath;
        }

        public Cue() : this(string.Empty) { }

        public int CueNumber
        {
            get => _cueNumber;
            set
            {
                int oldCueNumber = value;
                lock (_lock)
                {
                    if (_cueNumber != value)
                    {
                        Log.Info($"PropertyChange.CueNumber - Attempting to set CueNumber to {value}");
                        bool proceed = !IsInitializing && !_renaming && CheckForDuplicateCueFile(value);

                        if (proceed == true)
                        {
                            RenameCueFile(oldCueNumber, value);
                            _cueNumber = value;
                            OnPropertyChanged(nameof(CueNumber));
                            Log.Info($"CueNumber set to {value}");

                        }
                        else
                        {
                            _cueNumber = oldCueNumber;
                            Log.Info("User chose not to replace the existing cue file.");
                        }
                    }
                    else
                    {
                        OnPropertyChanged(nameof(CueNumber));
                        Log.Info($"CueNumber set to {value}");
                    }
                }
            }
        }
        public string CueName
        {
            get => _cueName;
            set
            {
                if (_cueName != value)
                {
                    Log.Info("PropertyChange.CueName");
                    _cueName = value;
                    OnPropertyChanged(nameof(CueName));
                }
            }
        }
        private string _displayedDuration;
        private bool _isEditingDuration = false;
        private string _lastValidDuration;
        public string Duration
        {
            get => _displayedDuration ?? $"{_totalDuration:mm\\:ss\\.ff}";
            set
            {
                if (_isEditingDuration)
                {
                    _displayedDuration = value; // Temporarily store user input
                }
                else
                {
                    if (TryParseDuration(value, out TimeSpan parsedDuration))
                    {
                        _totalDuration = parsedDuration;
                        _displayedDuration = value;
                        _lastValidDuration = value; // Store the last valid value
                        Log.Info($"Duration successfully updated to: {value}");
                    }
                    else
                    {
                        Log.Warning($"Invalid duration format entered: {value}. Reverting to file duration: {_displayedDuration}");
                        UpdateDuration(); // Set to the duration of the file
                    }
                }
            }
        }

        private bool TryParseDuration(string value, out TimeSpan parsedDuration)
        {
            return TimeSpan.TryParseExact(value, @"mm\:ss\.ff", null, out parsedDuration);
        }

        public void Duration_GotFocus()
        {
            _isEditingDuration = true; // Mark as editing
            Log.Info("User started editing the Duration field.");
            OnPropertyChanged(nameof(Duration)); // Update the binding
        }

        public void Duration_LostFocus()
        {
            _isEditingDuration = false; // Mark as no longer editing
            Log.Info("User finished editing the Duration field.");
            // Trigger validation by setting Duration to itself
            string tempDuration = _displayedDuration;
            _displayedDuration = null; // Force property change
            Duration = tempDuration;
            OnPropertyChanged(nameof(Duration)); // Update the binding
        }
        public string PreWait
        {
            get => _preWait;
            set
            {
                if (_preWait != value)
                {
                    Log.Info("PropertyChange.PreWait");
                    _preWait = value;
                    OnPropertyChanged(nameof(PreWait));


                }
            }
        }

        public string PostWait
        {
            get => _postWait;
            set
            {
                if (_postWait != value)
                {
                    Log.Info("PropertyChange.PostWait");
                    _postWait = value;
                    OnPropertyChanged(nameof(PostWait));
                }
            }
        }

        public bool AutoFollow
        {
            get => _autoFollow;
            set
            {
                if (_autoFollow != value)
                {
                    Log.Info("PropertyChange.AutoFollow");
                    _autoFollow = value;
                    OnPropertyChanged(nameof(AutoFollow));
                }
            }
        }

        public string FileName
        {
            get => _fileName;
            set
            {
                if (_fileName != value)
                {
                    Log.Info("PropertyChange.FileName");
                    _fileName = value;
                    OnPropertyChanged(nameof(FileName));
                }
            }
        }
        public string TargetFile
        {
            get => _targetFile;
            set
            {
                if (_targetFile != value)
                {
                    Log.Info("PropertyChange.TargetFile");
                    _targetFile = GetRelativePath(value);
                    OnPropertyChanged(nameof(TargetFile));
                    UpdateDuration();
                }
            }
        }


        internal string GetRelativePath(string filePath)
        {
            try {
                if (string.IsNullOrEmpty(filePath) || string.IsNullOrEmpty(PlaylistFolderPath))
                {
                    return filePath;
                }

                // Ensure filePath is an absolute path
                if (!Path.IsPathRooted(filePath))
                {
                    filePath = Path.GetFullPath(Path.Combine(PlaylistFolderPath, filePath));
                }

                var playlistFolderUri = new Uri(PlaylistFolderPath);
                var fileUri = new Uri(filePath);

                if (playlistFolderUri.IsBaseOf(fileUri))
                {
                    return Uri.UnescapeDataString(playlistFolderUri.MakeRelativeUri(fileUri).ToString().Replace('/', Path.DirectorySeparatorChar));
                }

                return filePath;
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
                return filePath;
            }
        }

        bool EditMode = MainWindow.EditMode;
        internal string GetAbsolutePath(string relativePath)
        {
            try
            {
                if (string.IsNullOrEmpty(relativePath) || string.IsNullOrEmpty(PlaylistFolderPath))
                {
                    return relativePath;
                }

                // Ensure relativePath is correctly combined with PlaylistFolderPath
                var playlistFolderUri = new Uri(PlaylistFolderPath);
                var fileUri = new Uri(playlistFolderUri, relativePath);

                return fileUri.LocalPath;
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
                if(EditMode == true)
                {
                    System.Windows.MessageBox.Show("Error: " + ex.Message);
                }
                return relativePath;
            }
        }



        public string Notes
        {
            get => _notes;
            set
            {
                if (_notes != value)
                {
                    Log.Info("PropertyChange.Notes");
                    _notes = value;
                    OnPropertyChanged(nameof(Notes));
                }
            }
        }

        public static string GetCurrentPlaylistPath()
        {
            try
            {
                string normalizedPath = Path.GetFullPath(PlaylistManager.playlistFolderPath);
                if (!normalizedPath.StartsWith(Path.GetFullPath(AppDomain.CurrentDomain.BaseDirectory)))
                {
                    Log.Warning("Attempted path traversal detected.");
                    MainWindow.CloseMainWindow();
                    return string.Empty;
                }
                var json = File.ReadAllText(normalizedPath);
                var cue = JsonConvert.DeserializeObject<Cue>(json);
                if (cue != null && !string.IsNullOrEmpty(cue.PlaylistFolderPath))
                {
                    return cue.PlaylistFolderPath;
                }
                else
                {
                    Log.Warning("PlaylistFolderPath not found in cue file.");
                    return string.Empty;
                }
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
                return string.Empty;
            }
        }

        private static bool isResolvingPath = false;
        protected void OnPropertyChanged(string propertyName)
        {
            if (!isResolvingPath && string.IsNullOrEmpty(PlaylistFolderPath))
            {
                isResolvingPath = true;
                PlaylistFolderPath = GetCurrentPlaylistPath();
                isResolvingPath = false;
            }

            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
            Log.Info($"Save.Called - Property changed: {propertyName}");
            if (!IsInitializing)
            {
                Save();
            }
        }
        private void UpdateDuration()
        {
            if (_isEditingDuration)
            {
                Log.Info("Skipping duration update because the user is editing the Duration field.");
                return;
            }

            var absoluteTargetFile = GetAbsolutePath(_targetFile);

            if (string.IsNullOrEmpty(absoluteTargetFile) || !File.Exists(absoluteTargetFile))
            {
                SetDuration(TimeSpan.Zero);
                Log.Warning("TargetFile is invalid or does not exist. Duration set to 00:00.00");
                return;
            }

            try
            {
                using (var audioFileReader = new AudioFileReader(absoluteTargetFile))
                {
                    var totalDuration = audioFileReader.TotalTime;
                    Log.Info($"Duration updated for TargetFile {absoluteTargetFile}: {totalDuration}");
                    SetDuration(totalDuration);
                }
            }
            catch (Exception ex)
            {
                SetDuration(TimeSpan.Zero);
                Log.Error($"Error calculating duration for file '{absoluteTargetFile}': {ex.Message}");
            }
        }

        private void SetDuration(TimeSpan duration)
        {
            _totalDuration = duration;
            _displayedDuration = $"{duration:mm\\:ss\\.ff}";
            OnPropertyChanged(nameof(Duration));
        }


        public void Save()
        {
            if (_renaming || string.IsNullOrEmpty(PlaylistFolderPath))
            {
                Log.Warning("Not Saved - PlaylistFolderPath is empty or null.");
                return;
            }
            CueManager.SaveCueToFile(this, PlaylistFolderPath);
            Log.Info("Saved successfully.");
        }
        bool PlaylistFolderPathNull = false;
        private bool CheckForDuplicateCueFile(float newCueNumber)
        {
            if (Cue.IsInitializing == true) { return false; }

            if (PlaylistFolderPath == null)
            {
                PlaylistFolderPathNull = true;
            }
            if (!Directory.Exists(PlaylistFolderPath) || PlaylistFolderPathNull)
            {
                Log.Warning($"Playlist folder does not exist: {PlaylistFolderPath}");
                return false;
            }
            string normalizedPath = Path.GetFullPath(PlaylistFolderPath);
            if (!normalizedPath.StartsWith(Path.GetFullPath(AppDomain.CurrentDomain.BaseDirectory)))
            {
                Log.Warning("Attempted path traversal detected in playlist folder path.");
                return false;
            }
            string newFilePath = Path.Combine(normalizedPath, $"cue_{newCueNumber}.json");

            if (File.Exists(newFilePath))
            {
                var result = System.Windows.MessageBox.Show(
                    $"Cue {newCueNumber} already exists. Replace it?",
                    "Duplicate File",
                    MessageBoxButton.YesNo,
                    MessageBoxImage.Warning
                );

                if (result == MessageBoxResult.No)
                {
                    newFilePath = null;
                    Log.Info("User chose not to replace the file.");
                    return false;
                }
                else
                {
                    try
                    {
                        File.Delete(newFilePath);
                        Log.Info($"Existing file deleted: {newFilePath}");
                        newFilePath = null;
                    }
                    catch (Exception ex)
                    {
                        Log.Error($"Error deleting file: {ex.Message}");
                        return false;
                    }
                }
            }

            return true;
        }

        private void RenameCueFile(float oldCueNumber, float newCueNumber)
        {
            string oldFilePath = Path.Combine(PlaylistFolderPath, $"cue_{oldCueNumber}.json");
            string newFilePath = Path.Combine(PlaylistFolderPath, $"cue_{newCueNumber}.json");

            try
            {

                if (File.Exists(oldFilePath))
                {
                    File.Move(oldFilePath, newFilePath);
                    _cueFilePath = newFilePath;
                    Log.Info($"File renamed: {oldFilePath} -> {newFilePath}");
                }
                else
                {
                    Log.Warning($"Old cue file not found: {oldFilePath}");
                }
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
            }

        }


    }
}
