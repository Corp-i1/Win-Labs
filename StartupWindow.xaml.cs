﻿using Microsoft.Win32;
using System;
using System.IO;
using System.Windows;
using System.Windows.Forms;
using System.Windows.Shapes;
using Win_Labs;

namespace Win_Labs
{
    public partial class StartupWindow : Window
    {
        public static string playlistFolderPath;
        public static string playlistImportFolderPath;

        public StartupWindow()
        {
            InitializeComponent();
        }

        private void CreateNewPlaylist_Click(object sender, RoutedEventArgs e)
        {
            
            var folderDialog = new System.Windows.Forms.FolderBrowserDialog();
            folderDialog.Description = "Select a folder to create a playlist in.";
            if (folderDialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                playlistFolderPath = folderDialog.SelectedPath;
                Log.log("Selected Path: "+playlistFolderPath, Log.LogLevel.Info);
                // Open the MainWindow with the selected playlist folder
                var mainWindow = new MainWindow(playlistFolderPath);
                Log.log("MainWindow.Created", Log.LogLevel.Info);
                CueManager.startUpFinished = true;
                Log.log("StartUp Finished");
                mainWindow.Show();
                Log.log("Showing.MainWindow", Log.LogLevel.Info);
                this.Close();
            }
        }

        private void OpenExistingPlaylist_Click(object sender, RoutedEventArgs e)
        {
            var folderDialog = new System.Windows.Forms.FolderBrowserDialog();
            folderDialog.Description = "Select a playlist to load.";
            if (folderDialog.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                playlistFolderPath = folderDialog.SelectedPath;
                Log.log("Selected Path: " + playlistFolderPath, Log.LogLevel.Info);
                // Open the MainWindow with the selected playlist folder
                var mainWindow = new MainWindow(playlistFolderPath);
                Log.log("MainWindow.Created", Log.LogLevel.Info);
                CueManager.startUpFinished = true;
                Log.log("StartUp Finished", Log.LogLevel.Info);
                mainWindow.Show();
                Log.log("Showing.MainWindow", Log.LogLevel.Info);
                this.Close();
            }
        }

        private void ImportPlayist_Click(Object sender, RoutedEventArgs e) {
            var openFileDialog = new System.Windows.Forms.OpenFileDialog
            {
                Filter = "Zip files (*.zip)|*.zip"
            };
            openFileDialog.Title = "Select the playlist you want to import.";
            if(openFileDialog.ShowDialog()== System.Windows.Forms.DialogResult.OK)
            {
                Log.log("Dialog.Opened");
                string playlistImportFilePath = openFileDialog.FileName;
                Log.log("Selected Import Path: " + playlistImportFilePath);
                import.openZIP();
                // Open the MainWindow with the selected playlist folder
                var mainWindow = new MainWindow(playlistFolderPath);
                Log.log("MainWindow.Created", Log.LogLevel.Info);
                CueManager.startUpFinished = true;
                Log.log("StartUp Finished", Log.LogLevel.Info);
                mainWindow.Show();
                Log.log("Showing.MainWindow", Log.LogLevel.Info);
                this.Close();
            }
        }

        private void CloseMenuItem_Click(object sender, RoutedEventArgs e)
        {
            Log.log("Close.Clicked");
            Log.log("Starting Close Protocol");
            Log.log("Closing.Window");
            this.Close();
        }
    }
}
