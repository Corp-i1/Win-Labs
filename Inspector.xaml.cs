﻿using System.Windows;
using Win_Labs.Properties;

namespace Win_Labs
{
    public partial class InspectorWindow : BaseWindow
    {
        public InspectorWindow()
        {
            Log.Info("Popout Inspector Opened.");
            InitializeComponent();
        }

        protected override void OnClosing(CancelEventArgs e)
        {
            Log.Info("Popout Inspector Closing.");
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
