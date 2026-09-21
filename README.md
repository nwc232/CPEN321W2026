# CPEN321_26W1_ProjectName

_Keep this README up to date with the steps required to build and run the frontend and backend (including any scripts, config files, and environment variables). TAs ill follow these instructions._

## Requirements

Install the following before the frontend or backend setup steps:

- [git](https://git-scm.com/install/)


--- 

## Frontend Setup

### Requirements

- [Android Studio](https://developer.android.com/studio) (latest version)
- [Java 17](https://adoptium.net/temurin/releases/?version=17)
- [Android SDK](https://developer.android.com/studio#command-tools) with API level 36+ (Android 16)

### Setup

1. **Open project**: Open the `frontend/` directory in Android Studio
2. **Sync Gradle**: Android Studio will automatically prompt you to sync the project. Click "Sync Now". You can also manually run `cd frontend && ./gradlew build` to trigger the sync and download the necessary dependencies.
3. **Configure Android SDK**: Ensure you have Android SDK 36 installed.
4. **Set up emulator/device**:
   - Create a new AVD (Android Virtual Device) by selecting Pixel 9 as the device and Android Baklava (API level 36) as the system image.
   - Alternatively, connect a physical Android device running Android 16 (API level 36).
5. **Setup app config**: Copy the example file, then fill in local values:
   ```bash
   cp frontend/local.properties.example frontend/local.properties
   ```
   Set at least:
   - `sdk.dir`: path to your Android SDK. Android Studio usually writes this the first time you open `frontend/`. On Mac it is often `sdk.dir=/Users/<username>/Library/Android/sdk`.
   - `API_BASE_URL`: backend URL baked into the APK. Use `https://8.235.69.138`. This is an always live cloud server so nothing needs to be ran locally on the backend.
   - `GOOGLE_CLIENT_ID`: Google OAuth Web client ID: `114242111483-15d5lpph9bv7gncpginqv32ls8q4nf5b.apps.googleusercontent.com`


### Build and Run

- **Debug build**: Click the green play button in the toolbar, to compile the code, package a debug APK, and install it on the connected device or running emulator. Alternatively, from the project root, run `./scripts/run-frontend.sh`.
- **Release build**: Go to Build -> Generate Signed App Bundle or APK -> APK. Follow the on-screen instructions to create a key, and select the "release" build variant. You will then have to manually install the generated APK on your device or the running emulator.


### Backend Configuration

The backend is deployed and running on the cloud at `https://8.235.69.138`. The app will connect to this backend automatically.

---
## Backend Setup

The backend is already deployed on the cloud as mentioned, but it can be run locally. Note that it doesn't have a database or persistent state right now. 

To run the backend locally the env file must be configured properly. The deployed server is already configured.

### Environment configuration

From the project root:

```bash
cp backend/.env.example backend/.env
```

Set at least:
- `PORT`: 443. If using HTTP locally, use 3000
- `SERVER_PUBLIC_IP`: 8.235.69.138
- `SERVER_NAME_FIRST`: set to your name or a filler
- `SERVER_NAME_LAST`: set to your name or a filler
- `TLS_KEY_PATH`: (optional): file path to the server's TLS private key (`server.key`). This and `TLS_CERT_PATH` need to be set to run over HTTPS. Otherwise server runs on HTTP.
- `TLS_CERT_PATH`: (optional) file path to the server's TLS certificate (`server.crt`).


### Option 1: Run locally

**Requirements:** 
- [Node.js](https://nodejs.org/en/download/) 22+
- [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) 10+

**Setup:** 
1. Install dependencies:

   ```bash
   cd backend
   npm install
   ```

2. **Production build**:

   ```bash
   npm run build
   npm start
   ```
