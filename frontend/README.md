# Notes Application - Android Mobile App

A feature-rich offline-first notes application for Android with cloud synchronization capabilities.

## Features

### 📝 Notes Management
- Create, edit, and delete notes
- Rich text support with title and content
- Soft delete with 90-day retention
- Full-text search across all notes
- Organize notes with multiple labels
- Sort by last modified time

### 🏷️ Labels & Organization
- Create custom labels with colors
- Attach multiple labels to notes
- Filter notes by label
- View notes grouped by labels

### 🔄 Offline Capabilities
- **Full offline support** - Create, edit, delete notes without internet
- **SQLite local database** - All data cached locally
- **Intelligent sync** - Automatic background synchronization every 15 minutes
- **Conflict resolution** - Smart merging of local and remote changes
- **Sync status tracking** - Visual indicators for pending changes
- **Manual sync** - Pull-to-refresh for on-demand sync

### 🔐 User Management
- Self-service registration
- Email verification
- Secure authentication with JWT tokens
- Session management across devices
- Persistent login (remembered across app restarts)

### 🔍 Search
- Real-time search across all notes
- Search in titles and content
- Works offline with cached data

## Architecture

### Local Database (SQLite + Room)
```
- users (cached user profile)
- user_session (persistent login)
- notes (all notes with sync status)
- labels (user labels)
- note_labels (many-to-many relationship)
- sync_metadata (last sync timestamps)
```

### Sync Mechanism
The app uses a sophisticated sync mechanism:

1. **Sync Status Flags**
   - `0` = Synced
   - `1` = Pending Create (created offline)
   - `2` = Pending Update (modified offline)
   - `3` = Pending Delete (deleted offline)

2. **Two-way Sync**
   - Pull: Fetch changes from server since last sync
   - Push: Upload local changes to server
   - Automatic conflict resolution

3. **Background Sync**
   - WorkManager schedules periodic sync (15 min)
   - Only syncs when network is available
   - Battery-optimized with backoff policy

## Tech Stack

- **Language**: Kotlin
- **UI**: View Binding, Material Design 3
- **Database**: Room (SQLite)
- **Networking**: Retrofit, OkHttp
- **Async**: Coroutines, Flow
- **Background**: WorkManager
- **Architecture**: Repository Pattern, MVVM
- **DI**: Manual dependency injection

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0+)
- Backend API running (see backend/README.md)

### Configuration

1. Update `API_BASE_URL` in `app/build.gradle`:
```gradle
buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:3000\""
```

For Android Emulator, use `10.0.2.2` instead of `localhost`.

### Build & Run

1. Open project in Android Studio
2. Sync Gradle files
3. Run on emulator or device

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Usage

### First Time Setup
1. Launch app
2. Click "Register" and fill in details
3. Check email for verification code
4. Click verification link
5. Login with credentials

### Creating Notes
1. Click FAB (+) button
2. Enter title and content
3. (Optional) Add labels
4. Click Save

### Offline Mode
- All features work offline
- Changes sync automatically when online
- Visual indicators show sync status

### Search
- Use search icon in toolbar
- Type to filter notes in real-time
- Works on cached data

## Project Structure

```
app/src/main/java/com/notesapp/offline/
├── data/
│   ├── local/
│   │   ├── entity/          # Room entities
│   │   ├── dao/             # Data Access Objects
│   │   └── NotesDatabase.kt
│   ├── remote/
│   │   ├── model/           # API models
│   │   ├── ApiService.kt    # Retrofit interface
│   │   └── RetrofitClient.kt
│   └── repository/          # Repository layer
├── ui/
│   ├── auth/                # Login & Register
│   ├── notes/               # Notes screens
│   └── MainActivity.kt
├── util/                    # Utilities
└── NotesApplication.kt      # Application class
```

## API Integration

The app communicates with the backend API:

- **Base URL**: Configured in build.gradle
- **Authentication**: JWT token in Authorization header
- **Endpoints**: See backend/README.md for API documentation

## Security

- Passwords never stored locally
- JWT tokens stored in Room database
- HTTPS recommended for production
- SQL injection prevention via Room
- Input validation on all forms

## Offline-First Design

The app follows offline-first principles:

1. **Read**: Always from local database (instant)
2. **Write**: Always to local database first
3. **Sync**: Background sync when online
4. **UI**: No loading spinners for cached data

## Troubleshooting

### Cannot connect to backend
- Check `API_BASE_URL` in build.gradle
- Use `10.0.2.2` for emulator (not `localhost`)
- Ensure backend is running
- Check firewall settings

### Sync not working
- Grant network permission
- Check internet connectivity
- View sync logs in Logcat (filter: "Sync")

### Database issues
- Clear app data in Settings
- Reinstall app
- Check Logcat for Room errors

## Future Enhancements

- [ ] Rich text editor
- [ ] Image attachments
- [ ] Voice notes
- [ ] Reminders & notifications
- [ ] Export notes (PDF, MD)
- [ ] Dark theme
- [ ] Biometric authentication
- [ ] Widget support
- [ ] Wear OS companion

## License

MIT
