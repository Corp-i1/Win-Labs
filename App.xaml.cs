using System.Windows;

namespace Win_Labs
{
    public partial class App : Application
    {
        private bool Debug = false;

        private void LaunchDebug()
        {
            if (Debug == false) { return; }
        }

        private void Routing()
        {
            Log.Info("Routing.Initialised");
        }

        private void Application_DispatcherUnhandledException(object sender, System.Windows.Threading.DispatcherUnhandledExceptionEventArgs e)
        {
            Log.Warning("Exception.Caught");
            MessageBox.Show("An unhandled exception just occurred: " + e.Exception.Message, "Exception Sample", MessageBoxButton.OK, MessageBoxImage.Error);
            Log.Exception(e.Exception);
            e.Handled = true;
        }

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