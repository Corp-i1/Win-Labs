using System.Windows;

namespace Win_Labs
{
    public partial class App : Application
    {
        private bool Debug = false;

        /* Function: LaunchDebug
         * Description: Launches debug mode if the Debug flag is set to true. Currently, this function does nothing if Debug is false.
         */
        private void LaunchDebug()
        {
            if (Debug == false) { return; }
        }

        /* Function: Routing
         * Description: Initializes the application's routing system and logs the initialization process.
         */
        private void Routing()
        {
            Log.Info("Routing.Initialised");
        }

        /* Function: Application_DispatcherUnhandledException
         * Description: Handles unhandled exceptions that occur in the application. Logs the exception, displays an error message to the user, and marks the exception as handled.
         * Parameters:
         *   - sender: The source of the exception.
         *   - e: The event arguments containing details about the unhandled exception.
         */
        private void Application_DispatcherUnhandledException(object sender, System.Windows.Threading.DispatcherUnhandledExceptionEventArgs e)
        {
            Log.Warning("Exception.Caught");
            MessageBox.Show("An unhandled exception just occurred: " + e.Exception.Message, "Exception Sample", MessageBoxButton.OK, MessageBoxImage.Error);
            Log.Exception(e.Exception);
            e.Handled = true;
        }

        /* Function: Application_Startup
         * Description: Handles the startup process of the application. Initializes logging, launches debug mode (if enabled), creates and shows the startup window, and initializes routing.
         * Parameters:
         *   - sender: The source of the startup event.
         *   - e: The event arguments containing details about the startup event.
         */
        private void Application_Startup(object sender, StartupEventArgs e)
        {
            // Initialize Logs
            Log.IntLog();

            Log.Info("Program.Start");
            LaunchDebug();
            Log.Info("Creating.StartupWindow");

            // Create the startup window
            StartupWindow startupWindow = new StartupWindow();

            // Initialize Routing
            Routing();

            // Show the startup window
            Log.Info("Showing.StartupWindow");
            startupWindow.Show();
        }
    }
}
