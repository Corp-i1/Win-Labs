using Newtonsoft.Json;
using System.IO;
using System;

namespace Win_Labs.Settings.AppSettings
{
    public static class AppSettingsManager
    {
        private static AppSettings _settings;
        private static readonly string SettingsFilePath = "settings.json";

        public static AppSettings Settings
        {
            get
            {
                if (_settings == null)
                {
                    LoadSettings();
                }
                return _settings;
            }
        }

        private static void LoadSettings()
        {
            if (!File.Exists(SettingsFilePath))
            {
                // Create default settings if the file doesn't exist
                _settings = new AppSettings
                {
                    Theme = "Dark",
                    Language = "en-UK",
                    MaxLogFiles = 10
                };
                SaveSettings();
                Log.Info("Default settings created and saved.");
            }
            else
            {
                try
                {
                    var json = File.ReadAllText(SettingsFilePath);
                    _settings = JsonConvert.DeserializeObject<AppSettings>(json);
                    if (_settings == null)
                    {
                        throw new JsonSerializationException("Settings could not be deserialised");
                    }
                    Log.Info("Settings loaded from file: " + SettingsFilePath);
                }
                catch (Exception ex)
                {
                    Log.Error($"Failed to load settings: {ex.Message}");
                    Log.Error($"Loading Defaults");
                    _settings = new AppSettings
                    {
                        Theme = "Dark",
                        Language = "en-UK",
                        MaxLogFiles = 10
                    };
                    SaveSettings();
                }
            }
        }

        public static void SaveSettings()
        {
            var json = JsonConvert.SerializeObject(_settings, Formatting.Indented);
            File.WriteAllText(SettingsFilePath, json);
            Log.Info("Settings saved to file.");
        }
    }

    public class AppSettings
    {
        public string Theme { get; set; } = "Dark";
        public string Language { get; set; } = "en-UK";
        public int MaxLogFiles { get; set; } = 10;
    }


}

