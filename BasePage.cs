using System.Windows.Controls;
using Win_Labs.Themes;

namespace Win_Labs
{
    public class BasePage : Page
    {
        protected AppSettings settings;
        private const string DefaultSplashScreenPath = @"\resources\Win-Labs_Logo_Splash.png";
        private const string DefaultIcon_ico = @"\resources\Icons\Win-Labs_Logo-256x256.ico";
        private const string DefaultIcon_png = @"\resources\Icons\Win-Labs_Logo-256x256.png";

        public BasePage()
        {
            // Load settings
            settings = AppSettingsManager.Settings;
            Console.WriteLine($"Theme: {settings.Theme}, Language: {settings.Language}");

            // Apply theme
            ThemeManager.ApplyTheme(settings.Theme);
        }
    }
}
