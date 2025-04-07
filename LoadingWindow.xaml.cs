using System;

namespace Win_Labs
{
    public partial class LoadingWindow : BaseWindow, INotifyPropertyChanged
    {
        private string _progressText;
        public event PropertyChangedEventHandler PropertyChanged;

        public LoadingWindow ()
        {
            InitializeComponent();
            DataContext = this;
            ProgressText = "0 / 0";
        }

        public string ProgressText
        {
            get => _progressText;
            set
            {
                _progressText = value;
                OnPropertyChanged(nameof(ProgressText));
            }
        }

        public void SetProgressBarMaximum(int value)
        {
            ProgressBar.Maximum = value;
            UpdateProgressText();
        }

        public void SetProgressBarValue(int value)
        {
            ProgressBar.Value = value;
            UpdateProgressText();
        }


        private void UpdateProgressText()
        {
            ProgressText = $"{ProgressBar.Value} / {ProgressBar.Maximum}";
        }

        protected virtual void OnPropertyChanged(string propertyName)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }
    }
}
