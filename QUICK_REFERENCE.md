# Quick Reference Guide

## 🚀 Getting Started (5 Minutes)

### 1. Start Backend (Terminal 1)
```bash
cd backend
npm install
createdb notes_app
cp .env.example .env
# Edit .env with your database credentials
npm run migrate
npm run dev
```

### 2. Start Android App
```bash
# Update frontend/android/app/build.gradle
# Change API_BASE_URL to "http://10.0.2.2:3000/api"

# Open in Android Studio and run
# OR
cd frontend/android
./gradlew installDebug
```

## 📋 Common Commands

### Backend
```bash
# Development
npm run dev              # Start with auto-reload

# Production
npm start                # Start server

# Database
npm run migrate          # Run migrations
psql notes_app           # Connect to database
```

### Android
```bash
./gradlew assembleDebug  # Build APK
./gradlew installDebug   # Install on device
./gradlew clean          # Clean build
```

## 🔑 API Quick Reference

### Base URL
```
http://localhost:3000/api
```

### Authentication
```bash
# Register
POST /auth/register
{
  "email": "demo.hemasri+john.doe@gmail.com",
  "password": "Abcd1234",
  "firstName": "John",
  "lastName": "Doe"
}

# Login
POST /auth/login
{
  "email": "demo.hemasri+john.doe@gmail.com",
  "password": "Abcd1234",
  "deviceInfo": "Android"
}

# Logout
POST /auth/logout
Headers: Authorization: Bearer {token}
```

### Notes
```bash
# Get all notes
GET /notes
Headers: Authorization: Bearer {token}

# Create note
POST /notes
Headers: Authorization: Bearer {token}
{
  "title": "My Note",
  "content": "Note content",
  "labelIds": [1, 2]
}

# Update note
PUT /notes/:id
Headers: Authorization: Bearer {token}
{
  "title": "Updated Title",
  "content": "Updated content"
}

# Delete note
DELETE /notes/:id
Headers: Authorization: Bearer {token}

# Search notes
GET /notes/search?q=keyword
Headers: Authorization: Bearer {token}
```

### Labels
```bash
# Get all labels
GET /labels
Headers: Authorization: Bearer {token}

# Create label
POST /labels
Headers: Authorization: Bearer {token}
{
  "name": "Important",
  "color": "#FF5722"
}
```

## 🗄️ Database Commands

### PostgreSQL
```bash
# Connect
psql notes_app

# View tables
\dt

# View table schema
\d users
\d notes
\d labels

# Query data
SELECT * FROM users;
SELECT * FROM notes WHERE user_id = 1;
SELECT * FROM labels;

# Cleanup old deleted notes (older than 90 days)
SELECT cleanup_old_deleted_notes(90);

# Cleanup expired sessions
SELECT cleanup_expired_sessions();
```

### Android SQLite
```bash
# View via Android Studio
# Tools → App Inspection → Database Inspector

# Or via adb
adb shell
run-as com.notesapp.offline
cd databases
sqlite3 notes_database
.tables
SELECT * FROM notes;
```

## 🐛 Debugging

### Backend Logs
```bash
# Check server logs in terminal
# Look for errors, API calls, database queries
```

### Android Logs
```bash
# Logcat in Android Studio
# Filter: "Notes" or "Sync"

# Or command line
adb logcat | grep "Notes"
```

## 🔧 Troubleshooting

### "Cannot connect to database"
```bash
# Check PostgreSQL is running
brew services list

# Start PostgreSQL
brew services start postgresql@14

# Check connection
psql -h localhost -U postgres -d notes_app
```

### "Cannot connect to backend from app"
```bash
# 1. Check backend is running
curl http://localhost:3000/health

# 2. For emulator, use 10.0.2.2 instead of localhost
# 3. For physical device, use your computer's IP
ifconfig | grep "inet "  # Get your IP
```

### "Email not sending"
```bash
# 1. Check SMTP credentials in .env
# 2. For Gmail, use App Password (not regular password)
# 3. Check firewall settings
```

### "Sync not working"
```bash
# 1. Check network permission in AndroidManifest.xml
# 2. Enable internet on emulator
# 3. Check WorkManager status:
#    Android Studio → App Inspection → WorkManager
# 4. Manual sync: Pull down to refresh in app
```

## 📊 Testing

### Test User Registration
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Abcd1234",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### Test Login
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Abcd1234",
    "deviceInfo": "Test Device"
  }'
```

### Test Create Note
```bash
# Replace {token} with token from login
curl -X POST http://localhost:3000/api/notes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "title": "Test Note",
    "content": "This is a test note"
  }'
```

## 📁 File Structure

```
offline-notes/
├── backend/                 # Node.js API
│   ├── src/
│   │   ├── db/             # Database
│   │   ├── services/       # Business logic
│   │   ├── routes/         # API endpoints
│   │   └── middleware/     # Auth, etc.
│   ├── package.json
│   └── .env
├── frontend/               # Android App
│   └── android/
│       └── app/
│           └── src/main/java/com/notesapp/offline/
│               ├── data/   # Database, API, Repositories
│               ├── ui/     # Activities, Fragments
│               └── util/   # Utilities
├── README.md              # Main documentation
├── SETUP.md              # Setup guide
└── IMPLEMENTATION_SUMMARY.md  # Technical details
```

## 🎯 Quick Test Workflow

1. **Start backend** → See "Server running on port 3000"
2. **Open app** → Should show login screen
3. **Register** → Enter details, check email
4. **Verify email** → Click link in email
5. **Login** → Use registered credentials
6. **Create note** → Click FAB, enter text, save
7. **Test offline** → Turn on Airplane Mode
8. **Create note offline** → Should work
9. **Go online** → Pull to refresh
10. **Verify sync** → Note should be on server

## 💡 Pro Tips

1. **Use Postman** - Import API endpoints for easy testing
2. **Enable Logcat** - Filter by "Notes" to see app logs
3. **Database Inspector** - View SQLite data in real-time
4. **WorkManager Inspector** - Check sync job status
5. **Network Profiler** - Monitor API calls
6. **Emulator Controls** - Simulate network conditions

## 📞 Getting Help

1. Check the logs (backend terminal + Android Logcat)
2. Review the README files
3. Check SETUP.md for common issues
4. Verify all prerequisites are installed
5. Try clean build: `./gradlew clean`
6. Clear app data and reinstall

## 🔐 Environment Variables (.env)

```env
# Required
DB_HOST=localhost
DB_USER=postgres
DB_PASSWORD=your_password
DB_NAME=notes_app
JWT_SECRET=random-secret-key

# Email (for registration)
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=app-password

# Optional
PORT=3000
SESSION_EXPIRES_DAYS=30
DELETED_NOTES_RETENTION_DAYS=90
```

## 🎨 Customization

### Change App Name
```xml
<!-- app/src/main/res/values/strings.xml -->
<string name="app_name">My Notes</string>
```

### Change API URL
```gradle
// app/build.gradle
buildConfigField "String", "API_BASE_URL", "\"http://localhost:3000/api\""
```

### Change Colors
```xml
<!-- app/src/main/res/values/colors.xml -->
<color name="primary">#6200EE</color>
```

---

**Quick Start Checklist:**
- [ ] PostgreSQL installed and running
- [ ] Backend .env configured
- [ ] Database migrated
- [ ] Backend server running
- [ ] Android Studio installed
- [ ] API URL configured in app
- [ ] App built and installed
- [ ] User registered and verified
- [ ] First note created

**Everything working?** You're all set! 🎉
