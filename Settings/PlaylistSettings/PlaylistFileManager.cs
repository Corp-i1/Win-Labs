using System;
using System.Collections.Generic;
using System.IO;
using System.Windows.Forms;
using System.Windows.Media;
using Newtonsoft.Json;
using static Win_Labs.Log;

namespace Win_Labs.Settings.PlaylistSettings
{
    public class PlaylistFileManager
    {
        public string FilePath { get; private set; }
        public PlaylistData Data { get; private set; }
        public PlaylistFileManager(string filePath)
        {
            FilePath = filePath;
            Data = new PlaylistData(); // Initialize with default values
        }
        public void Load()
        {
            try
            {
                if (File.Exists(FilePath))
                {
                    var json = File.ReadAllText(FilePath);
                    Data = JsonConvert.DeserializeObject<PlaylistData>(json) ?? new PlaylistData();
                    Log.Info($"Loaded playlist data from {FilePath}");
                }
                else
                {
                    Log.Warning($"Playlist file {FilePath} does not exist. Creating new data.");
                    Data = new PlaylistData();
                }
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
                Data = new PlaylistData();
            }
        }

        public void Save()
        {
            try
            {
                var json = JsonConvert.SerializeObject(Data, Formatting.Indented);
                File.WriteAllText(FilePath, json);
                Log.Info($"Saved playlist data to {FilePath}");
            }
            catch (Exception ex)
            {
                Log.Error($"Couldnt save PlaylistFile to {FilePath}");
                Log.Exception(ex);
            }
        }
    }

    public class PlaylistData
    {
        private double _masterVolume;
        public double MasterVolume
        {
            get => _masterVolume;
            set
            {
                // Default to 100 if the value is less than 0 or invalid
                if (value < 0)
                {
                    Log.Warning("Invalid or missing Master Volume, defaulting to 100.");
                    _masterVolume = 100;

                    if (MainWindow.EditMode)
                    {
                        System.Windows.MessageBox.Show("Invalid or missing Master Volume, defaulting to 100.", "Master Volume Warning", MessageBoxButton.OK, MessageBoxImage.Warning);
                    }
                    else
                    {
                        Log.Info("Show mode enabled, popup skipped. (Master Volume)");
                    }
                }
                else
                {
                    _masterVolume = value;
                }
            }
        }

        public string ExtraInfo { get; set; } = string.Empty;
        public bool IsSortEnabled { get; set; } = false;
        public string SortBy { get; set; } = DefaultSortBy;
        public bool SortAscending { get; set; } = true;
        public const string DefaultSortBy = "Cue_Number";

        public PlaylistData()
        {
            MasterVolume = 100; // Default value
        }

        public void LoadDefault(string option)
        {
            switch (option)
            {
                case nameof(MasterVolume):
                    MasterVolume = 100; // Default value
                    break;
                case nameof(ExtraInfo):
                    ExtraInfo = string.Empty; // Default value
                    break;
                case nameof(IsSortEnabled):
                    IsSortEnabled = false; // Default value
                    break;
                case nameof(SortBy):
                    SortBy = "Cue_Number"; // Default value
                    break;
                case nameof(SortAscending):
                    SortAscending = true; // Default value
                    break;
                default:
                    Log.Warning($"Unknown option '{option}' provided. No default value set.");
                    break;
            }
        }
    }



}