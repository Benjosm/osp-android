# osp-android

## OSP - Open Source Panopticon

**OSP (Open Source Panopticon)** is a truth verification platform that empowers users to capture and share verifiable media evidence. By combining immediate media capture with cryptographic metadata and a calculated trust score, OSP creates an auditable archive of documented reality for use in journalism, civic accountability, and crisis reporting.

---

## 🔧 Project Overview

OSP is a full-stack application with the following components:

- **Mobile Apps (Android/iOS)**: Users capture images/videos with embedded metadata (time, location, orientation) and upload them securely.
- **Backend API (FastAPI)**: Handles media processing, trust scoring, authentication, and data persistence.
- **Web Platform**: A public-facing interface to explore, search, and interact with verified media via an interactive map and filter tools.

> ⚠️ **Note**: This repository contains the Android app. Full project structure spans multiple repositories:
>
> - `osp-backend/` - FastAPI backend
> - `osp-web/` - Web interface
> - `osp-ios/` - iOS application
> - `osp-android/` - Android application (**this repo**)

---

## 📱 Mobile App (Android)

### Features
- Secure sign-in via **Google Play authentication**
- Camera flow:
  1. Capture photo/video
  2. Attach metadata: timestamp, GPS coordinates, device orientation
  3. Upload to backend with trust scoring
- Local media caching before upload
- Upload confirmation screen with:
  - Trust score
  - Upload metrics (e.g., latency, file size)
- Account creation only allowed on mobile

> ❌ *Digital watermarking (Digimark)* is intentionally **skipped** in this version.

### Requirements
- Android 8.0 (API 26) or higher
- Internet connection for upload and auth
- Location services enabled
- Camera permission

### Key Flows
#### Login & Authentication
- Uses Google Identity Services (Play Store sign-in)
- Exchanges Google ID token with backend `/auth/signin` endpoint
- Receives JWT tokens (access + refresh)

#### Media Capture & Upload
1. User opens camera via app
2. System captures metadata:
   - `capture_time`: ISO 8601 datetime
   - `lat`/`lng`: GPS coordinates (validated ±90 / ±180)
   - `orientation`: `'portrait'` or `'landscape'`
3. File stored locally with UUID name
4. On upload:
   - Sent to `/api/v1/media/upload`
   - Includes metadata and JWT token
   - Response contains:
     ```json
     {
       "media_id": "uuid",
       "trust_score": 92,
       "upload_time": "2025-04-05T12:00:00Z"
     }
     ```

---

## 🔐 Security & Trust Model

### Trust Score Calculation
The system calculates **trustworthiness** of media based on timeliness of upload:

```
trust_score = max(0, 100 - (upload_time - capture_time).total_seconds() / 60)
```

- **100**: Uploaded instantly
- **50**: Uploaded after 50 minutes
- **0**: Anything over 100 minutes old

> Example: A photo taken at `10:00` and uploaded at `10:30` → Score = `70`

### Authentication
- JWT-based sessions
- Access Token: 15-minute lifetime
- Refresh Token: 7-day lifetime
- Protected endpoints use dependency injection (`Depends(get_current_user)`)

---

## 🛠 Development Setup

### Prerequisites
- Android Studio (Hedgehog or later)
- Gradle 8.0+
- Google Services account (for authentication)
- Connected device or physical hardware (emulator unsupported)

### Environment Variables
Create a `local.properties` file:
```properties
backend_url=http://10.0.2.2:8000  # Android emulator localhost alias
auth_client_id=your-google-oauth-client-id
```

### Build & Run
```bash
# In Android Studio:
# 1. Open project
# 2. Sync Gradle
# 3. Build > Build Bundle(s) / APK(s)
# 4. Install on device
```

> 🚫 Emulator execution not supported due to GPS/camera constraints.

---

## 📦 Backend API Reference (Used by App)

### Base URL
`https://api.osp.local/v1` (or local during dev)

### Endpoints
| Method | Route | Description |
|-------|------|-------------|
| POST | `/auth/signin` | Sign in with Google/Apple ID token |
| POST | `/media/upload` | Upload media with metadata |
| GET | `/media/me` | List user's own media |
| DELETE | `/media/{id}` | Delete one's own media |
| POST | `/media/{id}/comment` | Add comment to media |

### Sample Upload Request
```http
POST /api/v1/media/upload
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data

File: video.mp4
Fields:
  capture_time: "2025-04-05T09:30:00Z"
  lat: 40.7128
  lng: -74.0060
  orientation: portrait
```

### Sample Response
```json
{
  "media_id": "a1b2c3d4-...",
  "trust_score": 95,
  "message": "Upload successful"
}
```

---

## 🗂 Project Structure (Android)
```
osp-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/osp/android/
│   │   │   ├── MainActivity.kt
│   │   │   ├── auth/AuthService.kt
│   │   │   ├── camera/CameraCapture.kt
│   │   │   ├── api/OspApiClient.kt
│   │   │   ├── model/MediaCapture.kt
│   │   │   └── ui/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
└── build.gradle
```

---

## ✅ Completion Criteria

OSP is considered **production-ready** when:

- [x] Mobile app captures media with correct metadata
- [x] Trust score returns accurately from backend
- [x] All API interactions use JWT protection
- [x] Location and time validation enforced
- [x] Local storage uses UUID-safe paths
- [x] Full test suite passes (`./gradlew test`)
- [x] Manual verification of upload and display flow

---

## 📄 License

Open Source Panopticon (OSP) is released under the MIT License. See `LICENSE` for details.

---

## 💬 Contact

Project maintained by the OSP Core Team.  
For issues or contributions, please open a ticket in the main `osp-backend` repository.
