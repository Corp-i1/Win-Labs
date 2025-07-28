using Microsoft.VisualBasic.Logging;
using System;
using System.IO;
using Win_Labs.Settings.AppSettings;

namespace Win_Labs
{
    internal static class Log
    {
        // Static fields for log configuration
        private static readonly string LogDirectory = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "Logs");
        private static readonly string LogFilePath = Path.Combine(LogDirectory, $"log_{DateTime.Now:yyyy-MM-dd_HH-mm-ss}.txt");
        private static readonly int MaxLogFiles = AppSettingsManager.Settings.MaxLogFiles;

        /* Function: IntLog
         * Description: Initializes the logging system by creating the log directory (if necessary) and cleaning up old log files.
         */
        internal static void IntLog()
        {
            InitializeLogDirectory();
            CleanUpOldLogFiles();
            Log.Info("Log.Initialised");
        }

        // Public logging methods

        /* Function: Info
         * Description: Logs an informational message.
         * Parameters:
         *   - message: The message to log.
         */
        internal static void Info(string message) => WriteLog(message, LogLevel.Info);
        /* Function: Warning
        * Description: Logs a warning message.
        * Parameters:
        *   - message: The message to log.
        */
        internal static void Warning(string message) => WriteLog(message, LogLevel.Warning);
        /* Function: Error
         * Description: Logs an error message.
         * Parameters:
         *   - message: The message to log.
         */
        internal static void Error(string message) => WriteLog(message, LogLevel.Error);


        /* Function: Exception
         * Description: Logs an exception, including its message and stack trace.
         * Parameters:
         *   - ex: The exception to log.
         */
        public static void Exception(Exception ex)
        {
            var exceptionMessage = $"Exception: {ex.Message}\nStack Trace: {ex.StackTrace}";
            WriteLog(exceptionMessage, LogLevel.Error);
        }

        // Private helper methods

        /* Function: InitializeLogDirectory
         * Description: Ensures the log directory exists. If it does not, the directory is created, and a log entry is written.
         */
        private static void InitializeLogDirectory()
        {
            if (!Directory.Exists(LogDirectory))
            {
                Directory.CreateDirectory(LogDirectory);
                WriteLog($"Log directory created at: {LogDirectory}", LogLevel.Info);
            }
        }
        /* Function: CleanUpOldLogFiles
         * Description: Deletes old log files to ensure the number of log files does not exceed the maximum allowed.
         */
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

        /* Function: WriteLog
         * Description: Writes a log message to both the console and the log file.
         * Parameters:
         *   - message: The message to log.
         *   - level: The log level (Info, Warning, or Error).
         */
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

        /* Enum: LogLevel
         * Description: Represents the severity level of a log message.
         * Values:
         *   - Info: Informational messages.
         *   - Warning: Warning messages.
         *   - Error: Error messages.
         */
        internal enum LogLevel
        {
            Info,
            Warning,
            Error
        }
    }
}
