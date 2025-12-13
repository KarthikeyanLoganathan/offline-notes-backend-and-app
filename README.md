# Notes Application - Offline-Capable Notes with Sync

A comprehensive notes application with offline capabilities, featuring a Node.js/PostgreSQL backend and Android mobile app with SQLite local storage and intelligent synchronization.

## 🌟 Features

### Backend (Node.js + PostgreSQL)
- **User Management**
  - Self-service registration with email verification
  - Secure authentication with JWT tokens
  - Session tracking across multiple devices
  - Profile management with address details

- **Notes Management**
  - Create, read, update, delete (CRUD) operations
  - Soft delete with configurable retention (90 days default)
  - Full-text search across notes
  - Labels/tags with many-to-many relationships
  - Timestamp tracking (created, last modified)

- **Sync Support**
  - Global and label-specific sync metadata
  - Incremental sync based on timestamps
  - Change tracking for efficient synchronization

- **Session Management**
  - Hybrid approach: JWT + database sessions
  - HTTP-only cookies for web security
  - Multi-device support with device tracking
  - Automatic session cleanup

### Android Mobile App
- **Offline-First Architecture**
  - Full SQLite local database
  - Create, edit, delete notes offline
  - Automatic background sync every 15 minutes
  - Manual sync via pull-to-refresh
  - Visual sync status indicators

- **Notes Features**
  - Rich text notes with title and content
  - Multi-label support
  - Search across all notes
  - Filter by labels
  - Deleted notes recovery

- **User Experience**
  - Material Design 3 UI
  - Persistent login (remembered sessions)
  - Real-time search
  - Optimistic UI updates

## 📋 Prerequisites

### Backend
- Node.js 16+ 
- PostgreSQL 12+
- SMTP server for email verification

### Android App
- Android Studio Hedgehog+
- Android SDK 24+ (Android 7.0+)
- Physical device or emulator

## 🚀 Quick Start

### 1. Backend Setup

```bash
cd backend

# Install dependencies
npm install

# Create database
createdb notes_app

# Configure environment
cp .env.example .env
# Edit .env with your database and email settings

# Run migrations
npm run migrate

# Start server
npm run dev
```

Backend runs on `http://localhost:3000`

### 2. Android App Setup

```bash
cd frontend/android

# Update API URL in app/build.gradle
buildConfigField "String", "API_BASE_URL", "\"http://localhost:3000/api\""

# Build and run
./gradlew assembleDebug
./gradlew installDebug
```

**Important**: For Android emulator, use `10.0.2.2` instead of `localhost`

## 📐 Architecture

### Database Schema

#### Backend (PostgreSQL)
```
users               - User accounts and profiles
user_sessions       - Active sessions across devices
notes               - Notes with soft delete support
labels              - User-defined labels
note_labels         - Many-to-many note-label mapping
user_sync_metadata  - Sync timestamps (global/per-label)
```

#### Mobile (SQLite)
```
users               - Cached user profile
user_session        - Persistent login session
notes               - All notes with sync status flags
labels              - User labels
note_labels         - Note-label relationships
sync_metadata       - Last sync timestamps
```

### Sync Mechanism

The application uses a sophisticated two-way sync mechanism:

1. **Local Changes Tracking**
   - Sync Status: 0=Synced, 1=PendingCreate, 2=PendingUpdate, 3=PendingDelete
   - Changes queued until network available
   - Automatic retry with exponential backoff

2. **Server-Side Change Detection**
   - Timestamp-based incremental sync
   - Global and per-label sync metadata
   - Efficient delta synchronization

3. **Conflict Resolution**
   - Server wins in conflicts
   - Local changes pushed first
   - Atomic operations prevent data loss

### API Endpoints

#### Authentication
```
POST   /api/auth/register          - Register new user
GET    /api/auth/verify-email      - Verify email with code
POST   /api/auth/login             - Login user
POST   /api/auth/logout            - Logout user
```

#### Notes
```
GET    /api/notes                  - Get all notes
GET    /api/notes/label/:labelId   - Get notes by label
GET    /api/notes/search?q=        - Search notes
GET    /api/notes/:id              - Get single note
POST   /api/notes                  - Create note
PUT    /api/notes/:id              - Update note
DELETE /api/notes/:id              - Delete note (soft)
POST   /api/notes/:id/restore      - Restore deleted note
```

#### Sync
```
GET    /api/notes/sync/global           - Get global sync metadata
GET    /api/notes/sync/label/:labelId   - Get label sync metadata
GET    /api/notes/sync/changes?since=   - Get notes changed since timestamp
```

#### Labels
```
GET    /api/labels                 - Get all labels
POST   /api/labels                 - Create label
PUT    /api/labels/:id             - Update label
DELETE /api/labels/:id             - Delete label
```

## 🔐 Security

### Backend
- Passwords hashed with bcrypt (10 rounds)
- JWT tokens with configurable expiration
- HTTP-only cookies for web clients
- SQL injection prevention via parameterized queries
- Email verification required for account activation

### Mobile
- JWT tokens stored securely in Room database
- No password storage on device
- TLS/HTTPS for all API communication (production)
- Input validation on all forms

## 🎯 Session Management Solution

The application uses a **hybrid approach** combining the best of both worlds:

### Why Hybrid?
1. **JWT Benefits**: Stateless, scalable, works great for mobile
2. **Database Session Benefits**: Tracking, remote logout, device management
3. **Cookie Benefits**: HTTP-only security for web clients

### Implementation
- Server issues JWT token on login
- Session record created in database with token, device info, expiry
- Mobile app stores token in SQLite
- Web clients receive token in HTTP-only cookie (+ response body)
- Each request validated against JWT AND database session
- Expired sessions auto-cleaned via database function

This approach provides:
- ✅ Scalability (JWT)
- ✅ Device tracking (DB)
- ✅ Remote logout (DB)
- ✅ Security (HTTP-only cookies)
- ✅ Flexibility (works for web & mobile)

## 📱 Mobile App Screenshots

### Key Screens Implemented
1. **Registration** - Self-service with validation
2. **Login** - Persistent session
3. **Notes List** - All notes sorted by last modified
4. **Note Detail** - Create/edit with auto-save
5. **Search** - Real-time filtering
6. **Labels** - Filter by label

## 🧪 Testing

### Backend
```bash
cd backend

# Test API health
curl http://localhost:3000/health

# Test registration
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Abcd1234","firstName":"John","lastName":"Doe"}'
```

### Mobile
- Run on emulator with network conditions
- Test offline mode (airplane mode)
- Verify background sync in WorkManager inspector
- Check Room database in App Inspection

## 🔧 Configuration

### Backend Environment Variables
```env
PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_NAME=notes_app
DB_USER=postgres
DB_PASSWORD=your_password
JWT_SECRET=your-secret-key
JWT_EXPIRES_IN=7d
SESSION_EXPIRES_DAYS=30
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
DELETED_NOTES_RETENTION_DAYS=90
```

### Mobile Build Config
```gradle
buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:3000\""
```

## 📊 Database Maintenance

### Cleanup Operations
```sql
-- Clean old deleted notes (run monthly)
SELECT cleanup_old_deleted_notes(90);

-- Clean expired sessions (run daily)
SELECT cleanup_expired_sessions();
```

Consider setting up cron jobs for automatic cleanup.

## 🚢 Deployment

### Backend (Production)
1. Set `NODE_ENV=production`
2. Use strong `JWT_SECRET`
3. Enable HTTPS
4. Configure CORS properly
5. Set up database backups
6. Use process manager (PM2)
7. Enable SSL for database

### Mobile (Production)
1. Update API URL to production
2. Enable ProGuard
3. Sign APK with release keystore
4. Test on multiple devices
5. Enable crash reporting
6. Optimize database queries

## 🤝 Contributing

This is a demonstration project showcasing:
- Full-stack development
- Offline-first mobile architecture
- Sync mechanisms
- Database design
- API development
- Android development with Kotlin

## 📝 License

VIT

## 👤 Author

HemaSri K J


## 🙏 Acknowledgments

- Node.js and Express.js communities
- Android and Kotlin teams
- PostgreSQL project
- Room persistence library
- Material Design guidelines
