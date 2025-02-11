# YTShare

YTShare is a mobile and desktop application that allows users to seamlessly share YouTube video links from their Android phone to a PC connected to the same network. The mobile app is built using **Android Studio (Kotlin)**, while the desktop service is powered by **ASP.NET Core Web API**.

## Features
- Instantly send YouTube video links from mobile to PC.
- Seamless integration between Android and Windows.
- Automatically detects PCs on the same network.
- Runs as a **Windows Service** for background execution.

## Technologies Used
### Mobile App (Android)
- **Language**: Kotlin
- **Framework**: Android SDK
- **Networking**: Volley

### Desktop App (ASP.NET Core Web API)
- **Language**: C#
- **Framework**: ASP.NET Core Web API
- **Hosting**: Windows Service
- **Networking**: HTTP API

---

## Installation
### **Mobile App (Android)**
1. Download and install the **YTShare APK** from the provided source.
2. Ensure your phone is connected to the same Wi-Fi network as your PC.
3. Open the app and scan for available PCs.

### **Desktop App (Windows Service)**
#### **Step 1: Install & Configure (C# version)**
1. Download and extract the **YTShare Desktop App**.
2. Open a **Command Prompt (Admin)** in the extracted folder.

#### **Step 1: Install & Configure (Python version)**
1. Download and extract the **yt_share_server**.
2. Install **pyinstaller** with **pip**.
3. Run the following command:
```sh
pyinstaller --onefile --hidden-import zeroconf._utils.ipaddress --hidden-import zeroconf.asyncio --hidden-import zeroconf.ipc --hidden-import zeroconf._handlers.answers yt_share_server.py --windowed
```

#### **Step 2: Register the Windows Service**
Run the following command to register the desktop app as a Windows Service:

```sh
sc create "YTShareService" binPath="C:\Path\To\YTShareDesktop.exe" start=auto
```

#### **Step 3: Start the Windows Service**
```sh
sc start "YTShareService"
```

#### **Step 4: Verify the Service is Running**
1. Open **Task Manager** → **Services Tab**.
2. Look for **YTShareService**.
3. Ensure the status is **Running**.

#### **Step 5: Stop and Uninstall (If Needed)**
To stop the service:
```sh
sc stop "YTShareService"
```
To uninstall the service:
```sh
sc delete "YTShareService"
```

---

## Usage
1. **Start the YTShare Desktop App** (if not already running as a service).
2. **Open the YTShare Mobile App** on your phone.
3. **Send a YouTube video link** from the mobile app.
4. The desktop app will automatically receive and display the link.
5. Click the link on the PC to open the video in a browser.

---

## Troubleshooting
### **Common Issues & Fixes**
1. **Service is not running**
   - Run `services.msc` and check if `YTShareService` is listed.
   - If stopped, try restarting it: `sc start "YTShareService"`.

2. **PC not detected by mobile app**
   - Ensure both devices are on the same network.
   - Check firewall settings and allow incoming connections.
   - Restart the desktop app and try again.

3. **YouTube link not opening on PC**
   - Verify that the desktop app is receiving data.
   - Check browser settings for default link handling.

---

## Releases

You can find the latest releases [here](https://github.com/MultiTron/YTShare/releases).

![Latest Release](https://img.shields.io/github/v/release/MultiTron/YTShare)

---

## License
YTShare is licensed under the **MIT License**. You are free to modify and distribute the application as per the license terms.

---

## Contributors
- **Ivailo Iliev** - Developer & Maintainer

---

## Contact
For support or feature requests, please contact:
- **Email**: multitron03@duck.com
- **GitHub**: [GitHub Repository](https://github.com/MultiTron/YTShare)
- **Website**: [Your Website](https://multitron.github.io)

