using Newtonsoft.Json;
using System.ComponentModel;
using System.IO;
using System.Reflection.Metadata;
using System.Windows;
using System.Xml.Serialization;
using Win_Labs.Properties;
using System.ComponentModel;
using System.Windows;
using System.Windows.Input;


namespace Win_Labs
{
    public partial class StartupWindow : BaseWindow
    {
        private PlaylistManager playlistManager;
        public string playlistFolderPath { get; private set; } = string.Empty;
        public string playlistImportFilePath { get; private set; } = string.Empty;
        public bool StartupWindowClosing { get; set; }
        /* Constructor: StartupWindow
 * Description: Initializes a new instance of the StartupWindow class, sets up the PlaylistManager, 
 *              and initializes the StartupWindowClosing flag.
 */
        public StartupWindow()
        {
            InitializeComponent();
            StartupWindowClosing = false;
            playlistManager = new PlaylistManager(this);
        }

        /* Function: Window_MouseDown
         * Description: Handles the MouseDown event for the window. Allows the user to drag the window by clicking anywhere on it.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void Window_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (Mouse.LeftButton == MouseButtonState.Pressed)
                this.DragMove();
        }

        /* Function: OnClosing
         * Description: Overrides the OnClosing method to prompt the user for confirmation before closing the window.
         * Parameters:
         *   - e: Provides data for the Closing event.
         */
        protected override void OnClosing(CancelEventArgs e)
        {
            if (!StartupWindowClosing)
            {
                Log.Info("StartupWindow.Close.Detected");

                var result = System.Windows.MessageBox.Show(
                    "Are you sure you want to close?",
                    "",
                    MessageBoxButton.YesNo,
                    MessageBoxImage.Warning);

                if (result == MessageBoxResult.No)
                {
                    Log.Info("User canceled the closing action.");
                    e.Cancel = true; // Prevent the window from closing
                    return;
                }

                Log.Info("Proceeding with closing the window.");
                StartupWindowClosing = true; // Mark that the window is closing
            }

            base.OnClosing(e);
        }

        /* Function: CreateNewPlaylist_Click
         * Description: Handles the click event for the "Create New Playlist" button. Initiates the creation of a new playlist.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void CreateNewPlaylist_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Create New Playlist clicked.");
            playlistManager.CreateNewPlaylist();
        }

        /* Function: OpenExistingPlaylist_Click
         * Description: Handles the click event for the "Open Existing Playlist" button. Opens an existing playlist.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void OpenExistingPlaylist_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Open Playlist clicked.");
            playlistManager.OpenExistingPlaylist();
        }

        /* Function: ImportPlaylist_Click
         * Description: Handles the click event for the "Import Playlist" button. Initiates the import of a playlist.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void ImportPlaylist_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Import Playlist clicked.");
            playlistManager.ImportPlaylist();
        }

        /* Function: CloseMenuItem_Click
         * Description: Handles the click event for the "Close" menu item. Closes the StartupWindow.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void CloseMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Close menu item clicked.");
            Close();
        }

        /* Function: CloseButton_Click
         * Description: Handles the click event for the "Close" button. Closes the StartupWindow.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void CloseButton_Click(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            Log.Info("Close Button clicked.");
            Close();
        }

        /* Function: TitleBarIcon_Click
         * Description: Handles the click event for the title bar icon. Displays an Easter egg message box.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void TitleBarIcon_Click(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            System.Windows.MessageBox.Show("Hi there.", "!!EasterEgg!!", MessageBoxButton.OK, MessageBoxImage.Exclamation);
        }

        /* Function: Settings_Click
         * Description: Handles the click event for the "Settings" button. Opens the SettingsWindow as a modal dialog.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void Settings_Click(object sender, RoutedEventArgs e)
        {
            Log.Info("Settings button clicked.");
            var settingsWindow = new SettingsWindow();
            settingsWindow.Owner = this;
            settingsWindow.ShowDialog();
        }
    }
}
