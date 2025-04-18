using System.Windows;
using Win_Labs.Properties;

namespace Win_Labs
{
    public partial class InspectorWindow : BaseWindow
    {
        /* Constructor: InspectorWindow
         * Description: Initializes a new instance of the InspectorWindow class, logs the opening of the popout inspector, and initializes the UI components.
         */
        public InspectorWindow()
        {
            Log.Info("Popout Inspector Opened.");
            InitializeComponent();
        }

        /* Function: OnClosing
         * Description: Overrides the OnClosing method to log the closing of the popout inspector.
         * Parameters:
         *   - e: Provides data for the Closing event.
         */
        protected override void OnClosing(CancelEventArgs e)
        {
            Log.Info("Popout Inspector Closing.");
        }

        /* Function: Duration_GotFocus
         * Description: Handles the GotFocus event for the Duration field. Delegates the event to the MainWindow if it is the owner of the InspectorWindow.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void Duration_GotFocus(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.Duration_GotFocus(sender, e);
            }
        }

        /* Function: Duration_LostFocus
         * Description: Handles the LostFocus event for the Duration field. Delegates the event to the MainWindow if it is the owner of the InspectorWindow.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void Duration_LostFocus(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.Duration_LostFocus(sender, e);
            }
        }

        /* Function: SelectTargetFile_Click
         * Description: Handles the Click event for the Select Target File button. Delegates the event to the MainWindow if it is the owner of the InspectorWindow.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void SelectTargetFile_Click(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.SelectTargetFile_Click(sender, e);
            }
        }

        /* Function: ClearTargetFile_Click
         * Description: Handles the Click event for the Clear Target File button. Delegates the event to the MainWindow if it is the owner of the InspectorWindow.
         * Parameters:
         *   - sender: The source of the event.
         *   - e: The event data.
         */
        private void ClearTargetFile_Click(object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.ClearTargetFile_Click(sender, e);
            }
        }

        private void DetailsTab_Click(Object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.DetailsTab_Click(sender, e);
            }
        }
        private void Time_LoopsTab_Click(Object sender, RoutedEventArgs e)
        {
            if (Owner is MainWindow mainWindow)
            {
                mainWindow.Time_LoopsTab_Click(sender, e);
            }
        }
    }
}

