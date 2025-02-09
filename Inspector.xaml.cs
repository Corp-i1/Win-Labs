using System.Windows;
using Win_Labs.Properties;

namespace Win_Labs
{
    public partial class InspectorWindow : Window
    {
        public InspectorWindow()
        {
            InitializeComponent();
        }

        private void Duration_GotFocus(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.Duration_GotFocus(sender, e);
            }
        }

        private void Duration_LostFocus(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.Duration_LostFocus(sender, e);
            }
        }

        private void SelectTargetFile_Click(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.SelectTargetFile_Click(sender, e);
            }
        }
    }
}
