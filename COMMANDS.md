# Essential Commands Cheat Sheet

## Backend Commands

### Initial Setup
```bash
cd backend
npm install
dropdb -U postgres notes_app
createdb -U postgres notes_app
cp .env.example .env
# Edit .env with your settings
npm run migrate
```

### Development
```bash
npm run dev          # Start with auto-reload (nodemon)
npm start            # Start production mode
```

### Database
```bash
# PostgreSQL Commands
createdb -U postgres notes_app                    # Create database
dropdb -U postgres notes_app                      # Delete database
psql notes_app                        # Connect to database
npm run migrate                       # Run migrations

# Inside psql
\dt                                   # List tables
\d users                              # Describe users table
SELECT * FROM users;                  # Query users
SELECT * FROM notes WHERE user_id=1;  # Query notes
SELECT cleanup_old_deleted_notes(90); # Cleanup deleted notes
SELECT cleanup_expired_sessions();    # Cleanup sessions
\q                                    # Quit psql
```

## Android Commands

### Build
```bash
cd frontend/android
./gradlew build                # Full build
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew clean                # Clean build
```

### Install & Run
```bash
./gradlew installDebug         # Install debug build
./gradlew uninstallDebug       # Uninstall app
adb install app-debug.apk      # Manual install
```

### Debugging
```bash
adb devices                    # List connected devices
adb logcat                     # View logs
adb logcat | grep "Notes"      # Filter logs
adb shell                      # Open shell
```

### Database Inspection
```bash
adb shell
run-as com.notesapp.offline
cd databases
sqlite3 notes_database
.tables                        # List tables
SELECT * FROM notes;           # Query notes
.quit                          # Exit sqlite
```

## API Testing with curl

### Health Check
```bash
curl http://localhost:3000/health
```

### Register User
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo.hemasri+john.doe@gmail.com",
    "password": "Abcd1234",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Login
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo.hemasri+john.doe@gmail.com",
    "password": "Abcd1234",
    "deviceInfo": "Test Device"
  }'
```

### Get Notes (with auth)
```bash
TOKEN="your-jwt-token-here"
curl -X GET http://localhost:3000/api/notes \
  -H "Authorization: Bearer $TOKEN"
```

### Create Note
```bash
curl -X POST http://localhost:3000/api/notes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "My Note",
    "content": "Note content here"
  }'
```

### Search Notes
```bash
curl -X GET "http://localhost:3000/api/notes/search?q=keyword" \
  -H "Authorization: Bearer $TOKEN"
```

### Create Label
```bash
curl -X POST http://localhost:3000/api/labels \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Important",
    "color": "#FF5722"
  }'
```

## Git Commands

### Initial Commit
```bash
git init
git add .
git commit -m "Initial commit: Notes app with offline sync"
```

### Version Control
```bash
git status                     # Check status
git add .                      # Stage all changes
git commit -m "message"        # Commit changes
git log --oneline              # View history
```

### Branching
```bash
git branch feature/new-feature # Create branch
git checkout feature/new-feature # Switch branch
git merge feature/new-feature  # Merge branch
```

## npm/Node Commands

### Package Management
```bash
npm install                    # Install dependencies
npm install package-name       # Add new package
npm uninstall package-name     # Remove package
npm update                     # Update all packages
npm outdated                   # Check outdated packages
```

### Scripts
```bash
npm run dev                    # Run dev script
npm start                      # Run start script
npm test                       # Run tests
```

## Environment Setup

### Node.js Version Management (nvm)
```bash
nvm install 16                 # Install Node 16
nvm use 16                     # Use Node 16
nvm ls                         # List installed versions
```

### PostgreSQL Service (macOS)
```bash
brew services start postgresql@14  # Start PostgreSQL
brew services stop postgresql@14   # Stop PostgreSQL
brew services restart postgresql@14 # Restart PostgreSQL
brew services list                 # List all services
```

## Android Emulator

### Create Emulator
```bash
# In Android Studio
Tools → Device Manager → Create Device
# Or command line
avdmanager create avd -n Pixel_5_API_30 -k "system-images;android-30;google_apis;x86_64"
```

### Run Emulator
```bash
emulator -avd Pixel_5_API_30  # Start emulator
emulator -list-avds           # List available AVDs
```

## Production Deployment

### Backend (PM2)
```bash
npm install -g pm2             # Install PM2
pm2 start src/index.js --name notes-api
pm2 status                     # Check status
pm2 logs                       # View logs
pm2 restart notes-api          # Restart app
pm2 stop notes-api             # Stop app
```

### Database Backup
```bash
# Backup
pg_dump notes_app > backup.sql
pg_dump -Fc notes_app > backup.dump

# Restore
psql notes_app < backup.sql
pg_restore -d notes_app backup.dump
```

### Docker (if using)
```bash
docker build -t notes-api .    # Build image
docker run -p 3000:3000 notes-api # Run container
docker ps                      # List containers
docker logs container-id       # View logs
```

## Troubleshooting

### Clear Everything and Start Fresh

#### Backend
```bash
cd backend
rm -rf node_modules package-lock.json
npm install
dropdb -U postgres notes_app
createdb -U postgres notes_app
npm run migrate
npm run dev
```

#### Android
```bash
cd frontend/android
./gradlew clean
# In Android Studio
File → Invalidate Caches / Restart
# Uninstall app from device
adb uninstall com.notesapp.offline
./gradlew installDebug
```

### Reset Database
```bash
# PostgreSQL
dropdb -U postgres notes_app && createdb -U postgres notes_app && npm run migrate

# Android (via adb)
adb shell
pm clear com.notesapp.offline  # Clear app data
```

### Check Ports
```bash
lsof -i :3000                  # Check what's using port 3000
kill -9 PID                    # Kill process by PID
```

### Network Issues
```bash
# Check if backend is accessible
curl http://localhost:3000/health

# For Android emulator, use 10.0.2.2 instead of localhost
curl http://10.0.2.2:3000/health

# Get your computer's IP for physical device
ifconfig | grep "inet "
# or
ipconfig getifaddr en0
```

## Development Workflow

### Daily Development
```bash
# Terminal 1 - Backend
cd backend
npm run dev

# Terminal 2 - Android Studio
# Open project and run on emulator

# Terminal 3 - Testing/Logs
cd backend
# Watch logs
# Or
cd frontend/android
adb logcat | grep "Notes"
```

### Before Committing
```bash
# Check for errors
npm run dev                    # Test backend
./gradlew build                # Test Android build
git status                     # Check changes
git diff                       # Review changes
git add .
git commit -m "Descriptive message"
```

## Useful Aliases (Add to ~/.zshrc or ~/.bashrc)

```bash
# Backend aliases
alias notes-backend="cd ~/path/to/backend && npm run dev"
alias notes-migrate="cd ~/path/to/backend && npm run migrate"
alias notes-db="psql notes_app"

# Android aliases
alias notes-android="cd ~/path/to/frontend/android"
alias notes-build="cd ~/path/to/frontend/android && ./gradlew assembleDebug"
alias notes-install="cd ~/path/to/frontend/android && ./gradlew installDebug"
alias notes-logs="adb logcat | grep Notes"

# Database aliases
alias notes-backup="pg_dump notes_app > ~/backups/notes_$(date +%Y%m%d).sql"
alias notes-cleanup="psql notes_app -c 'SELECT cleanup_old_deleted_notes(90);'"
```

---

**Pro Tip**: Keep a terminal open for backend logs and another for Android logs during development!
