using Microsoft.VisualBasic.Logging;
using System;
using System.IO;
using Win_Labs.Settings.AppSettings;

namespace Win_Labs
{
    public static class Log
    {
        private static readonly string LogDirectory = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Logs");
        private static readonly string LogFilePath = Path.Combine(LogDirectory, $"log_{DateTime.Now:yyyy-MM-dd_HH-mm-ss}.txt");
        private static readonly int MaxLogFiles = AppSettingsManager.Settings.MaxLogFiles;

        internal static void IntLog()
        {
            Log.Info("Log.Initialised");
            InitializeLogDirectory();
            CleanUpOldLogFiles();
        }

        // Public logging methods
        public static void Info(string message) => WriteLog(message, LogLevel.Info);

        public static void Warning(string message) => WriteLog(message, LogLevel.Warning);

        public static void Error(string message) => WriteLog(message, LogLevel.Error);

        public static void Exception(Exception ex)
        {
            var exceptionMessage = $"Exception: {ex.Message}\nStack Trace: {ex.StackTrace}";
            WriteLog(exceptionMessage, LogLevel.Error);
        }

        // Private helper methods
        private static void InitializeLogDirectory()
        {
            if (!Directory.Exists(LogDirectory))
            {
                Directory.CreateDirectory(LogDirectory);
                WriteLog($"Log directory created at: {LogDirectory}", LogLevel.Info);
            }
        }

        private static void CleanUpOldLogFiles()
        {
            var oldFiles = Directory.GetFiles(LogDirectory, "log_*.txt")
                                    .OrderByDescending(File.GetCreationTime)
                                    .Skip(MaxLogFiles);

            foreach (var file in oldFiles)
            {
                try
                {
                    File.Delete(file);
                    WriteLog($"Deleted old log file: {file}", LogLevel.Info);
                }
                catch (Exception ex)
                {
                    WriteLog($"Failed to delete old log file {file}: {ex.Message}", LogLevel.Error);
                }
            }
        }

        private static void WriteLog(string message, LogLevel level)
        {
            var logMessage = $"{DateTime.Now:yyyy-MM-dd HH:mm:ss} [{level}] {message}";

            try
            {
                Console.WriteLine(logMessage);
                File.AppendAllText(LogFilePath, logMessage + Environment.NewLine);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Failed to write log: {ex.Message}");
            }
        }

        // Enum for log levels
        public enum LogLevel
        {
            Info,
            Warning,
            Error
        }
    }
}
