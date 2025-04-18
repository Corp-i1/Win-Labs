using System;
using System.Collections.Generic;
using System.IO;
using System.Windows.Forms;
using System.Windows.Media;
using Newtonsoft.Json;

namespace Win_Labs.Settings.PlaylistSettings
{
    internal class PlaylistFileManager
    {
        internal string FilePath { get; private set; }
        internal PlaylistData Data { get; private set; }
        internal PlaylistFileManager(string filePath)
        {
            FilePath = filePath;
            Data = new PlaylistData(); // Initialize with default values
        }
        internal void Load()
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

        internal void Save()
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

    internal class PlaylistData
    {
        private double _masterVolume;
        internal double MasterVolume
        {
            get => _masterVolume;
            set
            {
                if (_masterVolume == null)
                {
                    Log.Warning("No Master Volume found, set to 100.");
                    _masterVolume = 100;
                    if (MainWindow.EditMode == true)
                    {
                        System.Windows.MessageBox.Show("No Master Volume found, set to 100.", "No Master Volume", MessageBoxButton.OK, MessageBoxImage.Warning);
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

        internal string ExtraInfo { get; set; }
        internal bool IsSortEnabled { get; set; }
        internal string SortBy { get; set; }
        internal bool SortAssending { get; set; }

        internal PlaylistData()
        {
            _masterVolume = 100; // Default value
            ExtraInfo = string.Empty; // Default value
            IsSortEnabled = false; // Default value
            SortBy = "Cue_Number"; // Default value 
            SortAssending = true; // Default value
        }
    }


}