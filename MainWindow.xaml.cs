using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Windows;
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
using Win_Labs.ZIPManagement;
using Win_Labs.Settings.PlaylistSettings;
using System.Windows.Threading;
using System.Windows.Media;

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

        /* Constructor: MainWindow
         * Description: Initializes the MainWindow, sets up the playlist manager, loads cues, and initializes UI components.
         * Parameters:
         *   - playlistFolderPath: The path to the playlist folder.
         */
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
            InitializeResizeEvents();
            Log.Info("Application started.");
        }

        /* Function: Initialize
         * Description: Initializes the application by binding cues, setting up handlers, and refreshing the cue list.
         */
        private void Initialize()
        {
            Log.Info("Initializing application...");
            BindCue(_currentCue);
            SetupCueChangeHandler();
            InitializeCueData();
            _activeWaveOuts = new List<WaveOutEvent>();
            RefreshCueList();

        }

        /* Function: InitializeCueData
         * Description: Loads cues from the playlist folder or creates a default cue if none exist.
         */
        private void InitializeCueData()
        {
            try
            {
                _playlistFolderPath = import.destinationPath;
                if (_playlistFolderPath == null)
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
        }

        /* Function: LoadCues
         * Description: Loads cues from the playlist folder into the ObservableCollection.
         */
        private void LoadCues()
        {
            Cue.IsInitializing = true;
            var loadedCues = new CueManager().LoadCues(_playlistFolderPath);
            _cues.Clear();
            foreach (var cue in loadedCues)
            {
                Log.Info($"Loaded cue: {cue.CueNumber}");
                _cues.Add(cue);
            }
            Cue.IsInitializing = false;
        }

        /* Function: SetupCueChangeHandler
         * Description: Sets up a handler to respond to property changes in the current cue.
         */
        private void SetupCueChangeHandler()
        {
            _currentCue.PropertyChanged += OnCurrentCuePropertyChanged;
        }

        /* Function: Duration_GotFocus
         * Description: Handles the GotFocus event for the Duration field of a cue.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        internal void Duration_GotFocus(object sender, RoutedEventArgs e)
        {
            if (sender is TextBox textBox && textBox.DataContext is Cue cue)
            {
                cue.Duration_GotFocus();
            }
        }

        /* Function: Duration_LostFocus
         * Description: Handles the LostFocus event for the Duration field of a cue.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        internal void Duration_LostFocus(object sender, RoutedEventArgs e)
        {
            if (sender is TextBox textBox && textBox.DataContext is Cue cue)
            {
                cue.Duration_LostFocus();
            }
        }

        /* Function: OnCurrentCuePropertyChanged
         * Description: Handles property changes in the current cue and refreshes the cue list.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void OnCurrentCuePropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName == nameof(Cue.CueNumber))
            {
                SaveCueData(_currentCue);
            }
            RefreshCueList();
        }

        /* Function: SaveCueData
         * Description: Saves the data of the specified cue to a file.
         * Parameters:
         *   - cue: The cue to save.
         */
        private void SaveCueData(Cue cue)
        {
            CueManager.SaveCueToFile(cue, _playlistFolderPath);
            RefreshCueList();
        }

        /* Function: CueListView_SelectionChanged
         * Description: Handles the selection change event for the cue list view.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: KeyDownManager
         * Description: Handles key press events for navigating and managing cues.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: BindCue
         * Description: Binds the specified cue to the DataContext and updates the InspectorWindow if open.
         * Parameters:
         *   - cue: The cue to bind.
         */
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

        /* Property: MainInspectorVisibility
         * Description: Gets or sets the visibility of the main inspector.
         */
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

        /* Event: PropertyChanged
         * Description: Occurs when a property value changes.
         */
        public event PropertyChangedEventHandler PropertyChanged;

        /* Function: OnPropertyChanged
         * Description: Notifies listeners that a property value has changed.
         * Parameters:
         *   - propertyName: The name of the property that changed.
         */
        private void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        /* Function: ShowMainInspector
         * Description: Shows the main inspector and updates the cue list view height.
         */
        private void ShowMainInspector()
        {
            Log.Info("Showing main inspector.");
            MainInspectorVisibility = Visibility.Visible; // Show Inspector when window is closed
            UpdateCueListViewHeight(); // Update the size of the cue listView
        }

        /* Function: HideMainInspector
         * Description: Hides the main inspector and updates the cue list view height.
         */
        private void HideMainInspector()
        {
            Log.Info("Hiding main inspector.");
            MainInspectorVisibility = Visibility.Hidden; // Hide Inspector in MainWindow
            UpdateCueListViewHeight(); // Update the size of the cue listView
        }

        /* Function: Pop_Out_Inspector_Click
         * Description: Handles the click event for the "Pop Out Inspector" button. Opens the InspectorWindow or focuses it if already open.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: CreateNewCue_Click
         * Description: Handles the click event for the "Create New Cue" button. Creates a new cue and adds it to the ObservableCollection.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: RefreshCueList
         * Description: Refreshes the cue list view to reflect any changes.
         */
        public void RefreshCueList()
        {
            var selectedCue = CueListView.SelectedItem;
            CollectionViewSource.GetDefaultView(CueListView.ItemsSource).Refresh();
            CueListView.SelectedItem = selectedCue;
        }

        /* Function: DeleteSelectedCue_Click
         * Description: Handles the click event for the "Delete Selected Cue" button. Deletes the selected cue from the collection and file system.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: SaveAllCues_Click
         * Description: Handles the click event for the "Save All Cues" button. Saves all cues in the collection to the file system.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void SaveAllCues_Click(object sender, RoutedEventArgs e)
        {
            foreach (var cue in _cues)
            {
                CueManager.SaveCueToFile(cue, _playlistFolderPath);
            }
        }

        /* Function: RefreshButton_Click
         * Description: Handles the click event for the "Refresh" button. Reloads cues and refreshes the cue list view.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void RefreshButton_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Refreshing playlist...");
            InitializeCueData();
            RefreshCueList();
        }

        /* Function: EditModeCheck
         * Description: Checks if the application is in edit mode.
         * Returns: True if in edit mode; otherwise, false.
         */
        internal static bool EditMode = true;
        internal static bool EditModeCheck()
        {
            if (EditMode == true)
            {
                return true;
            }
            return false;
        }

        /* Function: EditModeToggle_Click
         * Description: Handles the click event for the "Edit Mode Toggle" button. Toggles between edit mode and show mode.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void EditModeToggle_Click(object sender, RoutedEventArgs e)
        {
            if (!EditModeToggle.IsChecked == true)
            {
                EditModeToggle.Content = "Show Mode";
                EditMode = false;
                Log.Info("Switched to Show Mode");
                Log.Info("!Some Error and Warning message boxes will be skipped!");
            }
            else
            {
                EditModeToggle.Content = "Edit Mode";
                EditMode = true;
                Log.Info("Switched to Edit Mode");
            }
            RefreshCueList();
        }

        /* Function: SelectTargetFile_Click
         * Description: Handles the click event for the "Select Target File" button. Opens a file dialog to select a target file for the selected cue.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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
                    string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(openFileDialog.FileName);
                    selectedCue.TargetFile = openFileDialog.FileName;
                    selectedCue.FileName = fileNameWithoutExtension;
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

        /* Function: ClearTargetFile_Click
         * Description: Handles the click event for the "Clear Target File" button. Clears the target file for the selected cue.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        internal void ClearTargetFile_Click(object sender, RoutedEventArgs e)
        {
            if (CueListView.SelectedItem is Cue selectedCue)
            {
                selectedCue.TargetFile = string.Empty; // Set to an empty string instead of null
            }
        }

        /* Function: DeleteCue_Click
         * Description: Handles the click event for the "Delete Cue" button. Deletes the selected cue after user confirmation.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: StopButton_Click
         * Description: Handles the click event for the "Stop" button. Stops audio playback and cleans up audio resources.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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
        /* Function: PauseButton_Click
         * Description: Handles the click event for the "Pause" button. Toggles between pausing and resuming audio playback.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: GoButton_Click
         * Description: Handles the click event for the "Go" button. Starts playback of the selected cue.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: PlayCue
         * Description: Plays the specified cue. Handles audio playback initialization, duration validation, and auto-follow logic.
         * Parameters:
         *   - cue: The cue to play.
         */
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
        /* Function: PlayNextCueIfAutoFollow
         * Description: Automatically plays the next cue if the current cue has the AutoFollow property set to true.
         * Parameters:
         *   - currentCue: The currently playing cue.
         */
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
        /* Function: StopPlayback
         * Description: Stops playback for the specified WaveOutEvent and cleans up associated resources.
         * Parameters:
         *   - waveOut: The WaveOutEvent instance to stop playback for.
         */
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

                Log.Info($"Playback stopped for {_currentCue.TargetFile}.");
            }
            catch (Exception ex)
            {
                Log.Error($"Error stopping playback for specific track: {ex.Message}");
                MessageBox.Show($"Failed to stop playback for the track: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        /* Function: CleanupAudio
         * Description: Cleans up all active audio playback resources.
         */
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
        /* Function: SettingsMenuItem_Click
        * Description: Handles the click event for the "Settings" menu item. Opens the SettingsWindow as a modal dialog.
        * Parameters:
        *   - sender: The source of the event.
        *   - e: The event data.
        */
        private void SettingsMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Settings menu item clicked");
            var settingsWindow = new SettingsWindow();
            settingsWindow.Owner = this;
            settingsWindow.ShowDialog();
        }

        /* Function: ImportMenuItem_Click
         * Description: Handles the click event for the "Import Playlist" menu item. Initiates the playlist import process.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void ImportMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Import menu item clicked.");
            playlistManager.ImportPlaylist();
        }
        /* Function: ExportMenuItem_Click
 * Description: Handles the click event for the "Export Playlist" menu item. Exports the current playlist to the specified folder path.
 * Parameters:
 *   - sender: The source of the event.
 *   - e: The event data.
 */
        private void ExportMenuItem_Click(object sender, RoutedEventArgs e)
        {
            playlistManager.ExportPlaylist(_playlistFolderPath);
        }

        /* Function: SaveMenuItem_Click
         * Description: Handles the click event for the "Save" menu item. Saves all cues and playlist data to the file system.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: OpenMenuItem_Click
         * Description: Handles the click event for the "Open Playlist" menu item. Opens an existing playlist using the PlaylistManager.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void OpenMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Open menu item clicked.");
            playlistManager.OpenExistingPlaylist();
        }

        /* Function: CloseMenuItem_Click
         * Description: Handles the click event for the "Close" menu item. Prompts the user for confirmation and closes the application if confirmed.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void CloseMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Closing application via menu.");
            var result = MessageBox.Show("Are you sure you want to close the application?", "Confirm Close", MessageBoxButton.YesNo, MessageBoxImage.Question);

            if (result == MessageBoxResult.Yes)
            {
                Application.Current.Shutdown();
            }
        }

        /* Function: CloseMainWindow
         * Description: Closes the main window of the application. Invokes the shutdown process on the application's dispatcher.
         */
        internal static void CloseMainWindow()
        {
            Log.Info("Closing Main Window.");
            Application.Current.Dispatcher.Invoke(() => Application.Current.MainWindow.Close());
        }

        /* Function: OnClosing
         * Description: Overrides the OnClosing method to save all cues and playlist data before the application closes.
         * Parameters:
         *   - e: Provides data for the Closing event.
         */
        protected override void OnClosing(CancelEventArgs e)
        {
            base.OnClosing(e);
            SaveAllCues_Click(this, new RoutedEventArgs());
            _playlistFileManager.Save();
        }

        /* Function: Settings_Click
         * Description: Handles the click event for the "Settings" button. Opens the SettingsWindow as a modal dialog.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
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

        /* Function: MasterSlider_ValueChanged
         * Description: Handles the value change event for the master volume slider. Updates the master volume value in the PlaylistFileManager.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void MasterSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            MasterVolumeSliderValue = MasterVolumeSlider.Value;
        }
        private double _cueListViewHeight;
        public double CueListViewHeight
        {
            get => _cueListViewHeight;
            set
            {
                if (_cueListViewHeight != value)
                {
                    _cueListViewHeight = value;
                    OnPropertyChanged(nameof(CueListViewHeight));
                }
            }
        }
        internal double MainWindowHeight;
        internal double MainWindowMainGridRow0;
        internal double MainWindowMainGridRow1;
        internal double MainWindowMainGridRow2;
        internal double MainWindowMainGridRow3;
        internal double MainWindowMainGridRow4;
        /* Function:UpdateMainWindowRowHeights
         * Description:Sets up values for use in UpdateCueListViewHeight dynamicly.
         */
        internal void UpdateMainWindowRowHeights()
        {
            Log.Info("Updating MainWindow row heights.");
            MainWindowHeight = this.Height;
            MainWindowMainGridRow0 = GetMainWindowMainGridRow(0);
            MainWindowMainGridRow1 = GetMainWindowMainGridRow(1);
            MainWindowMainGridRow2 = GetMainWindowMainGridRow(2);
            MainWindowMainGridRow3 = GetMainWindowMainGridRow(3);
            MainWindowMainGridRow4 = GetMainWindowMainGridRow(4);
            Log.Info($"Row heights updated: \n Row 0: {MainWindowMainGridRow0}, \n Row 1: {MainWindowMainGridRow1}, \n Row 2: {MainWindowMainGridRow2}, \n Row 3: {MainWindowMainGridRow3}, \n Row 4: {MainWindowMainGridRow4}");
            Log.Info("Updated Inpector total row height");
            Log.Info($"Inspector row height: {GetMainWindowInspectorGridTotal()}");

        }
        /* Function: InitializeResizeEvents
         * Description: Creates a subscription to any resize of the main window to the method UpdateCueListViewHeight
         */
        internal void InitializeResizeEvents()
        {
            Log.Info("Initializing resize events.");
            // Subscribe to the SizeChanged event
            this.SizeChanged += UpdateCueListViewHeightEvent;
        }
        /* Method: GetMainWindowGridRow
        * Description: Gets the height of the main grid in the parent xaml file. Doing like this rather than just pasting in the numbers so they are dynamic.
        * Input:Row number (int)
        * Output:Height of row number inputed (double)
        */
        internal double GetMainWindowMainGridRow(int rowNumber)
        {
            return this.MainWindowMainGrid.RowDefinitions[rowNumber].ActualHeight;
        }
        /* Function: GetMainWindowInspectorGridTotal
         * Description: Calculates the total Inspector grid Height for use in the ResizeWindowUpdate function to make it dynamic.
         */
        internal double TotalInspectorHeight;
        private double GetMainWindowInspectorGridTotal()
        {
            double countingTotal = 0;
            foreach (RowDefinition rowDefinition in MainWindowInspectorGrid.RowDefinitions)
            {
                countingTotal += rowDefinition.Height.Value;
            }

            return TotalInspectorHeight = countingTotal;
        }

        /* Function: UpdateCueListViewHeightEvent
         * Description: Just setting up the event to call UpdateCueListViewHeight
         */
        private void UpdateCueListViewHeightEvent(object sender, SizeChangedEventArgs e)
        {
            UpdateCueListViewHeight();
        }
        /* Function: UpdateCueListViewHeight
         * Description: Update the size of the cue ListView depending on factors such as the popout inspector and size of MainWindow
         */
        internal void UpdateCueListViewHeight()
        {
            UpdateMainWindowRowHeights();
            if (_inspectorWindow != null) //true when the popout inspector exists
            {
                CueListViewHeight = MainWindowMainGridRow3;
            }
            else //true when there is no popout inspector
            {
                CueListViewHeight = MainWindowMainGridRow3 - TotalInspectorHeight;
            }
        }
        private void InitializeSorting()
        {
            
        }

        private bool IsSortEnabled = false;
        private bool SortAssending = true;
        private void AscendingCheckBox_Checked(object sender, RoutedEventArgs e)
        {
            SortAssending = true;
            TriggerSort();
        }

        private void AscendingCheckBox_Unchecked(object sender, RoutedEventArgs e)
        {
            SortAssending = false;
            TriggerSort();
        }

        private void SortEnabledCheckBox_Checked(object sender, RoutedEventArgs e)
        {
            IsSortEnabled = true;
            TriggerSort();
        }

        private void SortEnabledCheckBox_Unchecked(object sender, RoutedEventArgs e)
        {
            IsSortEnabled = false;
            TriggerSort();
        }

        private void TriggerSort()
        {
            if (IsSortEnabled)
            {
                string? sortBy = (SortComboBox.SelectedItem as ComboBoxItem)?.Content?.ToString();
                if (!string.IsNullOrEmpty(sortBy))
                {
                    SortListView(sortBy, SortAssending);
                }
                else
                {
                    Log.Warning("SortBy value is null or empty.");
                }
            }
            else
            {
                Log.Info("Sort is not enabled.");
            }
        }

        private void SortListViewComboBoxUpdate(object sender, SelectionChangedEventArgs e)
        {
            TriggerSort();
        }
        /*Funtion: SortListView
         * Description: Sorts the list view as specified by the users.
         * Parameters:
         */
        internal void SortListView(string sortBy, bool ascending = true)
        {
            try
            {
                var sortingHelper = new SortingHelper();

                // Determine the property to sort by
                Func<Cue, object> keySelector = sortBy switch
                {
                    "Cue_Number" => cue => cue.CueNumber,
                    "Cue_Name" => cue => cue.CueName,
                    "Duration" => cue => cue.Duration,
                    _ => throw new ArgumentException($"Invalid sortBy value: {sortBy}")
                };

                // Sort the cues
                var sortedCues = sortingHelper.SortCues(_cues.ToList(), keySelector, ascending);

                // Update the ObservableCollection
                _cues.Clear();
                foreach (var cue in sortedCues)
                {
                    _cues.Add(cue);
                }
                RefreshCueList();
            }
            catch (Exception ex)
            {
                Log.Error($"Error sorting list view: {ex.Message}");

                if (EditMode)
                {
                    MessageBox.Show($"Failed to sort the list view: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                }
                else
                {
                    Log.Warning("Message box skipped as in show mode.");
                }
            }
        }


    }
}
