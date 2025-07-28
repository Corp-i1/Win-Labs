using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using Win_Labs.Settings.AppSettings;
using Win_Labs.Themes;
using Win_Labs.Settings;

namespace Win_Labs
{
    public partial class SettingsWindow : BaseWindow
    {
        // UserControl instances for each category
        private GeneralSettingsControl generalSettingsControl;
        private AppearanceSettingsControl appearanceSettingsControl;
        private AdvancedSettingsControl advancedSettingsControl;

        /* Constructor: SettingsWindow
         * Description: Initializes a new instance of the SettingsWindow class, loads current application settings, 
         *              and populates the UI elements with the loaded settings.
         */
        public SettingsWindow()
        {
            InitializeComponent();

            // Initialize category controls
            generalSettingsControl = new GeneralSettingsControl();
            appearanceSettingsControl = new AppearanceSettingsControl();
            advancedSettingsControl = new AdvancedSettingsControl();

            // Set default content
            CategoryContentControl.Content = generalSettingsControl;

            // Load settings into controls
            LoadSettings();
        }

        // Handles switching the main content area when a category is selected
        private void CategoryListBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (CategoryContentControl == null || generalSettingsControl == null) return;

            switch (CategoryListBox.SelectedIndex)
            {
                case 0:
                    CategoryContentControl.Content = generalSettingsControl;
                    break;
                case 1:
                    CategoryContentControl.Content = appearanceSettingsControl;
                    break;
                case 2:
                    CategoryContentControl.Content = advancedSettingsControl;
                    break;
            }
        }

        // Loads current settings into the controls
        private void LoadSettings()
        {
            var settings = AppSettingsManager.Settings;

            // General
            generalSettingsControl.LanguageComboBox.SelectedItem =
                generalSettingsControl.LanguageComboBox.Items.OfType<ComboBoxItem>()
                .FirstOrDefault(item => item.Content.ToString() == settings.Language);
            generalSettingsControl.MaxLogFilesTextBox.Text = settings.MaxLogFiles.ToString();

            // Appearance
            appearanceSettingsControl.ThemeComboBox.SelectedItem =
                appearanceSettingsControl.ThemeComboBox.Items.OfType<ComboBoxItem>()
                .FirstOrDefault(item => item.Content.ToString() == settings.Theme);
        }

        // Add this method to encapsulate the save logic
        private bool SaveSettings()
        {
            var settings = AppSettingsManager.Settings;

            // General
            var selectedLanguage = (generalSettingsControl.LanguageComboBox.SelectedItem as ComboBoxItem)?.Content?.ToString();
            var maxLogFilesText = generalSettingsControl.MaxLogFilesTextBox.Text;

            // Appearance
            var selectedTheme = (appearanceSettingsControl.ThemeComboBox.SelectedItem as ComboBoxItem)?.Content?.ToString();

            // Validate and assign
            if (string.IsNullOrEmpty(selectedTheme) || string.IsNullOrEmpty(selectedLanguage))
            {
                MessageBox.Show("Please select both theme and language", "Invalid Selection", MessageBoxButton.OK, MessageBoxImage.Warning);
                return false;
            }

            if (!int.TryParse(maxLogFilesText, out int maxLogFiles))
            {
                MessageBox.Show("Please enter a valid number for Max Log Files", "Invalid Input", MessageBoxButton.OK, MessageBoxImage.Warning);
                return false;
            }

            settings.Theme = selectedTheme;
            settings.Language = selectedLanguage;
            settings.MaxLogFiles = maxLogFiles;

            // Save and apply
            AppSettingsManager.SaveSettings();
            try
            {
                ThemeManager.ApplyTheme(settings.Theme);
            }
            catch (Exception ex)
            {
                Log.Error($"Failed to apply theme: {ex.Message}");
                MessageBox.Show("Failed to apply theme. Settings were saved.",
                    "Theme Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            }

            return true;
        }

        // Save button: just save, do not close
        private void SaveButton_Click(object sender, RoutedEventArgs e)
        {
            SaveSettings();
        }

        // Save & Exit button: save and close if successful
        private void SaveAndExitButton_Click(object sender, RoutedEventArgs e)
        {
            if (SaveSettings())
            {
                this.Close();
            }
        }
        private void CancelButton_Click(object sender, RoutedEventArgs e)
        {
            this.Close();
        }

        // Allow window dragging from the top bar
        private void Window_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left)
                this.DragMove();
        }

        // Handle close button click
        private void CloseButton_Click(object sender, MouseButtonEventArgs e)
        {
            this.Close();
        }
    }
}
