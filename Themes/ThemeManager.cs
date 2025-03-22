using System;
using System.Windows;

namespace Win_Labs.Themes
{
    public static class ThemeManager
    {
        public static void ApplyTheme(string themeName)
        {
            if (string.IsNullOrWhiteSpace(themeName))
            {
                throw new ArgumentException("Theme name cannot be null or empty", nameof(themeName));
            }

            var themePath = $"/Themes/{themeName}Theme.xaml";
    
            // Clear existing merged dictionaries
            Application.Current.Resources.MergedDictionaries.Clear();
 
            // Load the selected theme
            try
            {
                var themeDict = new ResourceDictionary
                {
                    Source = new Uri(themePath, UriKind.Relative)
                };
                Application.Current.Resources.MergedDictionaries.Add(themeDict);
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException($"Failed to load theme: {themePath}", ex);
            }
        }
    }
}
