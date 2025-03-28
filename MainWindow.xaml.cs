using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Win32;
using Newtonsoft.Json;
using System.Collections.ObjectModel;
using NAudio.Wave;
using System.Text;
using System.Windows.Data;
using System.Xml.Serialization;
using Microsoft.VisualBasic.Logging;
using System.Reflection.Metadata;
using System.DirectoryServices;
using System.Windows.Documents;
using System.Drawing.Interop;
using Win_Labs.Properties;
using System.Reflection.Metadata.Ecma335;
using System.Diagnostics;
using System.Diagnostics.Eventing.Reader;
using System.Runtime.InteropServices;
using System.Printing;
using System.Windows.Input;
using System.Text.RegularExpressions;
using Win_Labs.ZIPManagement;


namespace Win_Labs
{
    public partial class MainWindow : BaseWindow, INotifyPropertyChanged
    {
        private PlaylistFileManager _playlistFileManager;
        private PlaylistManager playlistManager;
        private string _playlistFolderPath;
        private readonly ObservableCollection<Cue> _cues = new ObservableCollection<Cue>();
        private Cue _currentCue = new();
        public string _currentCueFilePath;
        public event RoutedEventHandler GotFocus;
        private InspectorWindow _inspectorWindow;

        public MainWindow(string playlistFolderPath)
        {
            PlaylistManager.playlistFolderPath = playlistFolderPath;
            InitializeComponent();
            DataContext = this; // Ensure DataContext is set to the MainWindow instance
            playlistManager = new PlaylistManager(this);
            _playlistFolderPath = PlaylistManager.playlistFolderPath;
            CueListView.ItemsSource = _cues;
            Initialize();

            // Initialize PlaylistFileManager
            string playlistFilePath = Path.Combine(_playlistFolderPath, "playlist.wlp");
            _playlistFileManager = new PlaylistFileManager(playlistFilePath);
            _playlistFileManager.Load();

            // Set the master volume slider value
            MasterVolumeSliderValue = _playlistFileManager.Data.MasterVolume;

            Log.Info("Application started.");
        }


        private void Initialize()
        {

            Log.Info("Initializing application...");
            BindCue(_currentCue);
            SetupCueChangeHandler();
            InitializeCueData();
            _activeWaveOuts = new List<WaveOutEvent>();
            RefreshCueList();
        }
        private void InitializeCueData()
        {
            try
            {
                _playlistFolderPath = import.destinationPath;
                if(_playlistFolderPath == null)
                {
                    _playlistFolderPath = PlaylistManager.playlistFolderPath;
                    if (!Directory.EnumerateFiles(_playlistFolderPath, "cue_*.json").Any())
                    {
                        Log.Info("No cues found. Creating a default cue.");
                        var defaultCue = CueManager.CreateNewCue(0, _playlistFolderPath);
                        CueManager.SaveCueToFile(defaultCue, _playlistFolderPath);
                        Log.Info("Default cue created successfully.");
                    }
                }
                else 
                {
                    if (!Directory.EnumerateFiles(_playlistFolderPath, "cue_*.json").Any())
                    {
                        Log.Info("No cues found. Creating a default cue.");
                        var defaultCue = CueManager.CreateNewCue(0, _playlistFolderPath);
                        CueManager.SaveCueToFile(defaultCue, _playlistFolderPath);
                        Log.Info("Default cue created successfully.");
                    }
                }
                LoadCues();
            }
            catch (Exception ex)
            {
                Log.Exception(ex);
            }
            finally
            {
                RefreshCueList();
            }
        }

        private void LoadCues()
        {
            Cue.IsInitializing = true;
            var loadedCues = CueManager.LoadCues(_playlistFolderPath);
            _cues.Clear();
            foreach (var cue in loadedCues)
            {
                Log.Info($"Loaded cue: {cue.CueNumber}");
                _cues.Add(cue);
            }
            RefreshCueList();
            Cue.IsInitializing = false;
        }

        private void SetupCueChangeHandler()
        {
            _currentCue.PropertyChanged += OnCurrentCuePropertyChanged;
        }

        internal void Duration_GotFocus(object sender, RoutedEventArgs e)
        {
            if (sender is TextBox textBox && textBox.DataContext is Cue cue)
            {
                cue.Duration_GotFocus();
            }
        }
        internal void Duration_LostFocus(object sender, RoutedEventArgs e)
        {
            if (sender is TextBox textBox && textBox.DataContext is Cue cue)
            {
                cue.Duration_LostFocus();
            }
        }
        private void OnCurrentCuePropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName == nameof(Cue.CueNumber))
            {
                SaveCueData(_currentCue);
            }
            RefreshCueList();
        }

        private void SaveCueData(Cue cue)
        {
            CueManager.SaveCueToFile(cue, _playlistFolderPath);
            RefreshCueList();
        }

        private void CueListView_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (CueListView.SelectedItem is Cue selectedCue)
            {
                BindCue(selectedCue);
                CurrentCue.Text = $"Selected: {selectedCue.CueName}";
            }
            else
            {
                CurrentCue.Text = "No Cue Selected";
            }
            RefreshCueList();
        }

        private void KeyDownManager(object sender, KeyEventArgs e)
        {
            if (CueListView.Items.Count == 0)
                return;

            int selectedIndex = CueListView.SelectedIndex;

            switch (e.Key)
            {
                case Key.Up:
                    if (selectedIndex > 0)
                    {
                        CueListView.SelectedIndex = selectedIndex - 1;
                        CueListView.ScrollIntoView(CueListView.SelectedItem);
                    }
                    break;
                case Key.Down:
                    if (selectedIndex < CueListView.Items.Count - 1)
                    {
                        CueListView.SelectedIndex = selectedIndex + 1;
                        CueListView.ScrollIntoView(CueListView.SelectedItem);
                    }
                    break;
                case Key.Escape:
                    CleanupAudio();
                    break;
                case Key.Space:

                    break;
            }
        }


        private void BindCue(Cue cue)
        {
            DataContext = cue;
            _currentCue = cue;

            // Update the DataContext of the InspectorWindow if it is open
            if (_inspectorWindow != null)
            {
                _inspectorWindow.DataContext = cue;
            }
        }
        private Visibility _mainInspectorVisibility = Visibility.Visible;
        public Visibility MainInspectorVisibility
        {
            get => _mainInspectorVisibility;
            set
            {
                if (_mainInspectorVisibility != value)
                {
                    _mainInspectorVisibility = value;
                    OnPropertyChanged(nameof(MainInspectorVisibility));
                }
            }
        }

        public event PropertyChangedEventHandler PropertyChanged;

        private void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        private void ShowMainInspector()
        {
            Log.Info("Showing main inspector.");
            MainInspectorVisibility = Visibility.Visible; // Show Inspector when window is closed
        }

        private void HideMainInspector()
        {
            Log.Info("Hiding main inspector.");
            MainInspectorVisibility = Visibility.Hidden; // Hide Inspector in MainWindow
        }

        private void Pop_Out_Inspector_Click(object sender, RoutedEventArgs e)
        {
            if (_inspectorWindow == null)
            {
                _inspectorWindow = new InspectorWindow
                {
                    Owner = this,
                    DataContext = _currentCue
                };
                _inspectorWindow.Closed += (s, args) =>
                {
                    _inspectorWindow = null;
                    ShowMainInspector(); // Ensure the main inspector is shown
                };
                _inspectorWindow.Show();
                HideMainInspector(); // Ensure the main inspector is hidden
            }
            else
            {
                _inspectorWindow.Focus();
            }
        }
        private void CreateNewCue_Click(object sender, RoutedEventArgs e)
        {
            // Calculate the next cue number based on the current count of cues
            int newCueNumber = (int)(_cues.Count > 0 ? _cues.Max(c => c.CueNumber) + 1 : 0);

            // Use CueManager to create the new cue
            var newCue = CueManager.CreateNewCue(newCueNumber, _playlistFolderPath);

            // Add the new cue to the ObservableCollection
            _cues.Add(newCue);

            // Set it as the currently selected cue in the UI
            CueListView.SelectedItem = newCue;

            // Update the DataContext for the UI binding
            DataContext = newCue;

            // Log the creation
            Log.Info($"Created a new cue: {newCue.CueNumber}");
            RefreshCueList();
        }

        public void RefreshCueList()
        {
            var selectedCue = CueListView.SelectedItem;
            CollectionViewSource.GetDefaultView(CueListView.ItemsSource).Refresh();
            CueListView.SelectedItem = selectedCue;
        }

        private void DeleteSelectedCue_Click(object sender, RoutedEventArgs e)
        {
            if (CueListView.SelectedItem is Cue selectedCue)
            {
                _cues.Remove(selectedCue);
                CueManager.DeleteCueFile(selectedCue, _playlistFolderPath);
                RefreshCueList();
            }
            else
            {
                MessageBox.Show("Please select a cue to delete.", "Delete Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void SaveAllCues_Click(object sender, RoutedEventArgs e)
        {
            foreach (var cue in _cues)
            {
                CueManager.SaveCueToFile(cue, _playlistFolderPath);
            }
        }

        private void RefreshButton_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Refreshing playlist...");
            InitializeCueData();
            RefreshCueList();
        }

        internal static bool EditMode = true;
        internal static bool EditModeCheck()
        {
            if (EditMode == true)
            {
                return true;
            }
            return false;
        }
        private void EditModeToggle_Click(object sender, RoutedEventArgs e)
        {
            // Toggle edit mode
            if (!EditModeToggle.IsChecked == true)
            {
                EditModeToggle.Content = "Show Mode";
                EditMode = false;
                Log.Info("Switched to Show Mode");
                Log.Info("!Some Error and Warning message box will be skiped!");
            }
            else
            {
                EditModeToggle.Content = "Edit Mode";
                EditMode = true;
                Log.Info("Switched to Edit Mode");
            }
            RefreshCueList();
        }

        internal void SelectTargetFile_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Selecting target file...");
            var openFileDialog = new Microsoft.Win32.OpenFileDialog
            {
                Filter = "Audio Files|*.mp3;*.wav;*.flac|All Files|*.*"
            };

            if (openFileDialog.ShowDialog() == true)
            {
                if (CueListView.SelectedItem is Cue selectedCue)
                {
                    string fileNameWoutextention = Path.GetFileNameWithoutExtension(openFileDialog.FileName);
                    selectedCue.TargetFile = openFileDialog.FileName;
                    selectedCue.FileName = fileNameWoutextention;
                    DataContext = selectedCue;
                    CueManager.SaveCueToFile(selectedCue, _playlistFolderPath);
                    Log.Info($"'{openFileDialog.FileName}' added to {CueListView.SelectedItem}");
                }
                else
                {
                    MessageBox.Show("No cue selected. Please select a cue before choosing a target file.", "No Cue Selected", MessageBoxButton.OK, MessageBoxImage.Warning);
                }
            }
        }

        private void DeleteCue_Click(object sender, RoutedEventArgs e)
        {
            if (CueListView.SelectedItem is Cue selectedCue)
            {
                Log.Info($"Delete Cue button clicked. Deleting Cue: {selectedCue.CueNumber} - {selectedCue.CueName}");
                var confirmation = MessageBox.Show($"Are you sure you want to delete Cue {selectedCue.CueNumber}?",
                                                   "Confirm Delete",
                                                   MessageBoxButton.YesNo,
                                                   MessageBoxImage.Warning);

                if (confirmation == MessageBoxResult.Yes)
                {
                    // Remove cue from collection
                    _cues.Remove(selectedCue);

                    // Delete the file
                    CueManager.DeleteCueFile(selectedCue, _playlistFolderPath);
                    Log.Info($"Cue {selectedCue.CueNumber} deleted successfully.");
                    RefreshCueList();
                }
            }
            else
            {
                MessageBox.Show("No cue selected to delete.", "Delete Error", MessageBoxButton.OK, MessageBoxImage.Error);
                Log.Warning("Delete Cue button clicked with no selection.");
            }

        }
        private WaveOutEvent? waveOut; // For audio playback
        private AudioFileReader? audioFileReader; // For reading audio files


        private void StopButton_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Stop button clicked.");
            try
            {
                CleanupAudio();
                Log.Info("Audio playback stopped.");
            }
            catch (Exception ex)
            {
                Log.Error($"Error stopping playback: {ex.Message}");
                MessageBox.Show($"Failed to stop playback: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }
        bool Paused = false;

        private void PauseButton_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Pause button clicked.");
            try
            {
                if (PauseButtonToggle.IsChecked == true) // Pause all
                {
                    Paused = true;
                    PauseButtonToggle.Content = "Play";
                    try
                    {
                        foreach (var waveOut in _activeWaveOuts)
                        {
                            if (waveOut.PlaybackState == PlaybackState.Playing)
                            {
                                waveOut.Pause();
                                Log.Info($"Paused audio track: {waveOut.GetHashCode()}");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Log.Error($"Error pausing all audio: {ex.Message}");
                        MessageBox.Show($"Failed to pause audio: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                }
                else // Resume all
                {
                    Paused = false;
                    PauseButtonToggle.Content = "Pause";
                    try
                    {
                        foreach (var waveOut in _activeWaveOuts)
                        {
                            if (waveOut.PlaybackState == PlaybackState.Paused)
                            {
                                waveOut.Play();
                                Log.Info($"Resumed audio track: {waveOut.GetHashCode()}");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Log.Error($"Error resuming all audio: {ex.Message}");
                        MessageBox.Show($"Failed to resume audio: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Error($"Error toggling playback: {ex.Message}");
                MessageBox.Show($"Failed to toggle playback: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private List<WaveOutEvent> _activeWaveOuts = new List<WaveOutEvent>();
        private Dictionary<WaveOutEvent, System.Timers.Timer> _playbackTimers = new();
        private Dictionary<WaveOutEvent, AudioFileReader> _audioFileReaders = new();

        private void GoButton_Click(object sender, RoutedEventArgs e)
        {
            if (Paused)
            {
                Log.Warning("Cannot start new playback while paused.");
                if (EditMode == true)
                {
                    MessageBox.Show("Playback is currently paused. Resume before starting a new track.",
                                    "Action Blocked", MessageBoxButton.OK, MessageBoxImage.Warning);
                }
                return;
            }

            Log.Info("Go button clicked.");

            if (CueListView.SelectedItem is not Cue selectedCue || string.IsNullOrEmpty(selectedCue.TargetFile))
            {
                MessageBox.Show("No valid cue selected to play.", "Play Error", MessageBoxButton.OK, MessageBoxImage.Warning);
                Log.Warning("Go button clicked with no valid cue selected.");
                return;
            }
            PlayCue(selectedCue);
        }

        private void PlayCue(Cue cue)
        {
            try
            {
                _currentCue = cue;
                // Resolve the TargetFile to an absolute path
                var absoluteTargetFile = cue.GetAbsolutePath(cue.TargetFile);

                // Ensure TargetFile exists
                if (!File.Exists(absoluteTargetFile))
                {
                    if (EditMode == true)
                    {
                        Log.Error($"Target file does not exist: {absoluteTargetFile}");
                        MessageBox.Show($"The file {absoluteTargetFile} could not be found.", "File Not Found", MessageBoxButton.OK, MessageBoxImage.Error);
                        return;
                    }
                    else
                    {
                        Log.Error($"Target file does not exist: {absoluteTargetFile}");
                        Log.Warning("Message box skipped as in show mode.");
                        return;
                    }
                }

                // Initialize playback components
                var audioReader = new AudioFileReader(absoluteTargetFile);
                var newWaveOut = new WaveOutEvent();
                newWaveOut.Init(audioReader);

                // Set the volume to the master volume from PlaylistFileManager
                newWaveOut.Volume = (float)_playlistFileManager.Data.MasterVolume / 100;

                // Ensure collections are initialized
                if (_activeWaveOuts == null) _activeWaveOuts = new List<WaveOutEvent>();
                if (_playbackTimers == null) _playbackTimers = new Dictionary<WaveOutEvent, System.Timers.Timer>();
                if (_audioFileReaders == null) _audioFileReaders = new Dictionary<WaveOutEvent, AudioFileReader>();

                _audioFileReaders[newWaveOut] = audioReader;

                // Validate and parse duration
                bool isDurationValid = false;
                TimeSpan limitDuration = TimeSpan.Zero;

                if (!string.IsNullOrWhiteSpace(cue.Duration))
                {
                    // Try to parse duration as a TimeSpan (e.g., "mm:ss.ff")
                    if (TimeSpan.TryParseExact(cue.Duration, @"mm\:ss\.ff", null, out limitDuration) && limitDuration.TotalMilliseconds > 0)
                    {
                        isDurationValid = true;
                    }
                    // If not, try to parse as plain milliseconds (e.g., "120000")
                    else if (double.TryParse(cue.Duration, out double durationInMilliseconds) && durationInMilliseconds > 0)
                    {
                        limitDuration = TimeSpan.FromMilliseconds(durationInMilliseconds);
                        isDurationValid = true;
                    }
                }

                if (isDurationValid)
                {
                    Log.Info($"Playing Cue {cue.CueNumber}: {absoluteTargetFile} for {limitDuration.TotalSeconds} seconds.");

                    var playbackTimer = new System.Timers.Timer(limitDuration.TotalMilliseconds)
                    {
                        AutoReset = false // Trigger only once
                    };

                    playbackTimer.Elapsed += (s, args) =>
                    {
                        playbackTimer.Stop();
                        playbackTimer.Dispose();
                        Dispatcher.Invoke(() => StopPlayback(newWaveOut));
                        Dispatcher.Invoke(() => PlayNextCueIfAutoFollow(cue));
                    };

                    _playbackTimers[newWaveOut] = playbackTimer;
                    playbackTimer.Start();
                }
                else
                {
                    Log.Warning($"Invalid or zero duration specified for cue {cue.CueNumber}. Playing the full track.");
                    MessageBox.Show($"The duration '{cue.Duration}' is invalid. The full track will be played.",
                                    "Invalid Duration", MessageBoxButton.OK, MessageBoxImage.Warning);

                    // Set the duration back to the audio file's length
                    cue.Duration = audioReader.TotalTime.ToString(@"mm\:ss\.ff");
                }

                newWaveOut.Play();
                _activeWaveOuts.Add(newWaveOut);

                CurrentTrack.Text = $"Playing: {cue.FileName}";

                // Move to next cue without auto follow
                int currentIndex = CueListView.Items.IndexOf(cue);
                bool foundNextCue = false;
                while (currentIndex + 1 < CueListView.Items.Count && !foundNextCue)
                {
                    currentIndex++;
                    var nextCue = CueListView.Items[currentIndex] as Cue;
                    if (nextCue != null && !nextCue.AutoFollow)
                    {
                        foundNextCue = true;
                        CueListView.SelectedIndex = currentIndex;
                        CueListView.ScrollIntoView(nextCue);
                    }
                }

                if (!foundNextCue)
                {
                    Log.Info("Reached the end of the cue list or no non-auto-follow cue found.");
                }

            }
            catch (Exception ex)
            {
                Log.Error($"Error playing cue: {ex.Message}");
                MessageBox.Show($"Failed to play the cue: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }
        private void PlayNextCueIfAutoFollow(Cue currentCue)
        {
            int currentIndex = _cues.IndexOf(currentCue);
            if (currentIndex + 1 < _cues.Count)
            {
                Cue nextCue = _cues[currentIndex + 1];
                if (nextCue.AutoFollow)
                {
                    PlayCue(nextCue);
                }
            }
        }
        private void StopPlayback(WaveOutEvent waveOut)
        {
            try
            {
                if (_playbackTimers.ContainsKey(waveOut))
                {
                    _playbackTimers[waveOut].Stop();
                    _playbackTimers[waveOut].Dispose();
                    _playbackTimers.Remove(waveOut);
                }

                if (_audioFileReaders.ContainsKey(waveOut))
                {
                    _audioFileReaders[waveOut].Dispose();
                    _audioFileReaders.Remove(waveOut);
                }

                CleanupAudio();

                Log.Info($"Playback stopped for {_currentCue.TargetFile}."); //not working TODO
            }
            catch (Exception ex)
            {
                Log.Error($"Error stopping playback for specific track: {ex.Message}");
                MessageBox.Show($"Failed to stop playback for the track: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }


        private void CleanupAudio()
        {
            if (_activeWaveOuts != null)
            {
                foreach (var waveOut in _activeWaveOuts)
                {
                    try
                    {
                        waveOut.Stop();
                        waveOut.Dispose();
                        Log.Info("Disposed of a WaveOutEvent instance.");
                    }
                    catch (Exception ex)
                    {
                        Log.Error($"Error disposing WaveOutEvent: {ex.Message}");
                    }
                }

                _activeWaveOuts.Clear();
            }

            waveOut = null;
            audioFileReader = null;
            CurrentTrack.Text = "No Track Playing";
            Log.Info("Audio resources cleaned up.");
        }

        private void SettingsMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Settings menu item clicked");
            var settingsWindow = new SettingsWindow();
            settingsWindow.Owner = this;
            settingsWindow.ShowDialog();
        }

        private void ImportMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Import menu item clicked.");
            playlistManager.ImportPlaylist();
        }


        private void ExportMenuItem_Click(object sender, RoutedEventArgs e)
        {
            playlistManager.ExportPlaylist(_playlistFolderPath);
        }


        private void SaveMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Save menu item clicked.");
            try
            {
                CueManager.SaveAllCues(_cues, _playlistFolderPath);
                _playlistFileManager.Save();
                MessageBox.Show("All changes have been saved.", "Save Complete", MessageBoxButton.OK, MessageBoxImage.Information);
            }
            catch (Exception ex)
            {
                Log.Error($"Error saving cues: {ex.Message}");
                MessageBox.Show($"Failed to save changes: {ex.Message}", "Save Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
            RefreshCueList();
        }

        private void OpenMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Open menu item clicked.");
            playlistManager.OpenExistingPlaylist();
        }

        private void CloseMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Closing application via menu.");
            // Confirm with the user
            var result = MessageBox.Show("Are you sure you want to close the application?", "Confirm Close", MessageBoxButton.YesNo, MessageBoxImage.Question);

            if (result == MessageBoxResult.Yes)
            {
                // Close the application
                Application.Current.Shutdown();
            }
        }

        internal static void CloseMainWindow()
        {
            Log.Info("Closing Main Window.");
            Application.Current.Dispatcher.Invoke(() => Application.Current.MainWindow.Close());
        }

        protected override void OnClosing(CancelEventArgs e)
        {
            base.OnClosing(e);
            SaveAllCues_Click(this, new RoutedEventArgs());

            // Save playlist data
            _playlistFileManager.Save();
        }


        private void Settings_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Settings button clicked");
            var settingsWindow = new SettingsWindow();
            settingsWindow.Owner = this;
            settingsWindow.ShowDialog();
        }
        internal double _masterVolumeSliderValue;
        internal double MasterVolumeSliderValue
        {
            get => _masterVolumeSliderValue;
            set
            {
                if (_masterVolumeSliderValue != value)
                {
                    _masterVolumeSliderValue = value;
                    OnPropertyChanged(nameof(MasterVolumeSliderValue));
                    if (_playlistFileManager?.Data != null)
                    {
                        _playlistFileManager.Data.MasterVolume = value; // Update PlaylistFileManager
                    }
                    else
                    {
                        Log.Error("PlaylistFileManager or its Data property is not initialized.");
                    }
                }
            }
        }

        private void MasterSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            MasterVolumeSliderValue = MasterVolumeSlider.Value;
            
        }
    }
}
