# Offline Notes App (Flutter)

A Flutter implementation of the offline-first notes application, matching the features of the native Android app.

## Project Structure

This directory contains the source code (`lib/`) and configuration (`pubspec.yaml`) for the Flutter app.

Since the project was generated via source code, you need to initialize the platform scaffolding.

## Setup Instructions

### 1. Prerequisites
- Flutter SDK installed (3.0+)
- Android Studio / VS Code
- Android Emulator or Physical Device

### 2. Initialize Project
Open a terminal in this directory (`frontend/flutter/`) and run:

```bash
# Download dependencies
flutter pub get

# Initialize Android/iOS platform files (Critical step!)
flutter create .
```

*Note: The `.` at the end is important. It tells Flutter to recreate the platform folders in the current directory.*

### 3. Configuration

#### API URL
By default, the app is configured for Android Emulator:
`lib/config/api_config.dart`:
```dart
static const String baseUrl = 'http://10.0.2.2:3000/api';
```
If running on a physical device, update this to your computer's local IP address (e.g., `http://192.168.1.100:3000/api`).

#### Permissions
After running `flutter create .`, ensure `android/app/src/main/AndroidManifest.xml` has the INTERNET permission. Flutter usually adds this to debug mode, but for release/production, verify:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

### 4. Run the App

```bash
flutter run
```

## Features Implemented
- **Offline First**: Uses SQLite (`sqflite`) to store data locally.
- **Sync**: Background sync every 15 minutes using `workmanager`.
- **Authentication**: JWT-based login/register with session persistence.
- **Notes**: Create, Edit, Delete notes with offline support.
- **UI**: Material Design 3.

## Sync Architecture
- **Push**: Local changes (Creates, Updates, Deletes) are pushed to server when online.
- **Pull**: Server changes are fetched based on `last_synced_timestamp`.
- **Conflict Resolution**: Server wins (simple strategy).
