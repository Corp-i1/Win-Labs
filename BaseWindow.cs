using System.Windows;
using Win_Labs.Settings.AppSettings;
using Win_Labs.Themes;

namespace Win_Labs
{
    public class BaseWindow : Window
    {
        protected AppSettings settings;
        private const string DefaultSplashScreenPath = @"\resources\Win-Labs_Logo_Splash.png";
        private const string DefaultIcon_ico = @"\resources\Icons\Win-Labs_Logo-256x256.ico";
        private const string DefaultIcon_png = @"\resources\Icons\Win-Labs_Logo-256x256.png";

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
