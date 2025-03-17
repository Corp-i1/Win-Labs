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
using Win_Labs.Themes;

namespace Win_Labs
{
    /// <summary>
    /// Interaction logic for SettingsWindow.xaml
    /// </summary>
    public partial class SettingsWindow : BaseWindow
    {
        public SettingsWindow()
        {
            InitializeComponent();

            // Load current settings
            var settings = AppSettingsManager.Settings;

            // Set Theme ComboBox
            ThemeComboBox.SelectedItem = ThemeComboBox.Items.OfType<ComboBoxItem>()
                .FirstOrDefault(item => item.Content.ToString() == settings.Theme);

            // Set Language ComboBox
            LanguageComboBox.SelectedItem = LanguageComboBox.Items.OfType<ComboBoxItem>()
                .FirstOrDefault(item => item.Content.ToString() == settings.Language);
        }

        private void SaveButton_Click(object sender, RoutedEventArgs e)
        {
            // Update settings
            var settings = AppSettingsManager.Settings;
            var selectedTheme = ThemeComboBox.SelectedItem as ComboBoxItem;
            var selectedLanguage = LanguageComboBox.SelectedItem as ComboBoxItem;
            
            if (selectedTheme?.Content == null || selectedLanguage?.Content == null)
            {
                MessageBox.Show("Please select both theme and language", "Invalid Selection", 
                    MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }
            
            settings.Theme = selectedTheme.Content.ToString();
            settings.Language = selectedLanguage.Content.ToString();

            // Save settings to file
            AppSettingsManager.SaveSettings();

            // Apply the selected theme
            try
            {
                ThemeManager.ApplyTheme(settings.Theme);
            }
            catch (Exception ex)
            {
                Log.Error($"Failed to apply theme: {ex.Message}");
                System.Windows.MessageBox.Show("Failed to apply theme. Settings were saved.", 
                    "Theme Error", MessageBoxButton.OK, MessageBoxImage.Warning);
            }

            // Close the settings window
            this.Close();
        }
    }
}
