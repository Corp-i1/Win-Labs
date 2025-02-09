using System.Windows;

namespace Win_Labs
{
    public class BaseWindow : Window
    {
        protected AppSettings settings;

        public BaseWindow()
        {
            try
            {
                // Load settings
                settings = AppSettingsManager.Settings;
                Console.WriteLine($"Theme: {settings.Theme}, Language: {settings.Language}");

                // Apply theme
                ThemeManager.ApplyTheme(settings.Theme);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error initializing BaseWindow: {ex.Message}");
                // Optionally, set a default theme
                ThemeManager.ApplyTheme("DefaultTheme");
            }
        }
    }
}
