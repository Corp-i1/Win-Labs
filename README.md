# Win-Labs
### My Website: [Corpi1.uk](https://Corpi1.uk)
This project is my window alternative for Q-Labs for Sound technicians that dont want to or have to opertunity to use the real thing.

## Installation
To install please head to the latest relase [here](https://github.com/Corp-i1/Win-Labs/releases).

## Building

If you want to build the project yourself, you can do so by following these steps:

1. Install [Visual Studio](https://visualstudio.microsoft.com/) (Community Edition works plenty fine)
    - When installing, make sure to select the **.NET desktop development** workload as this installs the required components for building WPF applications.
1. Clone the [``master``](https://github.com/Corp-i1/Win-Labs/tree/master) branch repository to your local machine using Git (can be done in Visual Studio if you want) or download it as a ZIP file.
1. Open the folder in Visual Studio as a new project.
1. Build the project by selecting **Build > Build Solution** from the menu or pressing `Ctrl + Shift + B`
    - This should install all the required dependencies and build the project if it doesn't manual install the following as NuGet packages:
    - [Newtonsoft.Json](https://www.nuget.org/packages/Newtonsoft.Json/)
    - [NAudio](https://www.nuget.org/packages/NAudio)
    - This can be done by right clicking on the project in the Solution Explorer and selecting **Manage NuGet Packages**. Then search for the package name and click **Install**.
1. One the build completes you can run it in two ways:
    1. Run from Visual Studio by selecting **Debug > Start Debugging** from the menu or pressing `F5`
    1. Run the built executable from `\"The folder in visual studio"\bin\Debug\net8.0-windows7.0` (replace "The folder in visual studio" with the path to the folder you opened in Visual Studio).
        - If you built a release version the path is the same as above but replace `Debug` with `Release`.
        - The executable will be named `Win-Labs.exe` Just double click it to run it.
1. **Extras**
    - You can also run the project from a different location by copying the entire folder to a different location and running it from there.
        - This is useful if you want to run the project from a USB drive or a different computer.
        - Just make sure to copy the entire folder and not just the executable as it may require other files in the folder to run properly.
    - You can move the entire folder to a different location and run it from there as well as rename the folder.
    - You can also make a shortcut to the executable and place it on your desktop or pin it to the taskbar for easy access. You can do this by:
        1. Right click on the `Win-Labs.exe` file and select **Create shortcut**.
        2. Drag the shortcut to your desktop.
        - Or
        1. Right click on the `Win-Labs.exe` file and select **Pin to taskbar**.
## License  
This project is licensed under the **MPL-2.0 with a No Resale Clause**.  

You are free to use, modify, and distribute the software. **Businesses and individuals can use it**, including in commercial activities. However, **you may not sell, repackage, or monetize the software itself**.  

See the [LICENSE](LICENSE.md) file for full terms.  
 

Time spent in this Repo.
[![wakatime](https://wakatime.com/badge/user/dc5608ba-abdb-4d5d-8789-16bac0be884c/project/c7bf0c09-9db0-426a-bddf-b3340f13bfe7.svg)](https://wakatime.com/badge/user/dc5608ba-abdb-4d5d-8789-16bac0be884c/project/c7bf0c09-9db0-426a-bddf-b3340f13bfe7) + [![wakatime](https://wakatime.com/badge/user/dc5608ba-abdb-4d5d-8789-16bac0be884c/project/02a2cc4a-f0e8-4331-bf77-49a920b17061.svg)](https://wakatime.com/badge/user/dc5608ba-abdb-4d5d-8789-16bac0be884c/project/02a2cc4a-f0e8-4331-bf77-49a920b17061)

![](https://wakatime.com/share/@dc5608ba-abdb-4d5d-8789-16bac0be884c/a9670e22-663a-429b-9519-54c0fbaf7c21.svg)

![](https://wakatime.com/share/@Corpi1/a6bb99e1-94f3-4d97-bbc7-a198ac708f80.svg)
