using System;

namespace Win_Labs
{
    public partial class LoadingWindow : BaseWindow, INotifyPropertyChanged
    {
        private string _progressText;
        public event PropertyChangedEventHandler PropertyChanged;

        /* Constructor: LoadingWindow
         * Description: Initializes a new instance of the LoadingWindow class, sets the DataContext, and initializes the progress text.
         */
        public LoadingWindow()
        {
            InitializeComponent();
            DataContext = this;
            ProgressText = "0 / 0";
        }

        /* Property: ProgressText
         * Description: Gets or sets the progress text displayed in the UI. Notifies the UI when the value changes.
         */
        public string ProgressText
        {
            get => _progressText;
            set
            {
                _progressText = value;
                OnPropertyChanged(nameof(ProgressText));
            }
        }

        /* Function: SetProgressBarMaximum
         * Description: Sets the maximum value of the progress bar and updates the progress text.
         * Parameters:
         *   - value: The maximum value to set for the progress bar.
         */
        public void SetProgressBarMaximum(int value)
        {
            ProgressBar.Maximum = value;
            UpdateProgressText();
        }

        /* Function: SetProgressBarValue
         * Description: Sets the current value of the progress bar and updates the progress text.
         * Parameters:
         *   - value: The current value to set for the progress bar.
         */
        public void SetProgressBarValue(int value)
        {
            ProgressBar.Value = value;
            UpdateProgressText();
        }

        /* Function: UpdateProgressText
         * Description: Updates the progress text to reflect the current and maximum values of the progress bar.
         */
        private void UpdateProgressText()
        {
            ProgressText = $"{ProgressBar.Value} / {ProgressBar.Maximum}";
        }

        /* Function: OnPropertyChanged
         * Description: Notifies listeners that a property value has changed.
         * Parameters:
         *   - propertyName: The name of the property that changed.
         */
        protected virtual void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }
}

