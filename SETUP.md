# Project Setup Guide

## Backend Setup (Node.js + PostgreSQL)

### 1. Install PostgreSQL
```bash
# macOS
brew install postgresql@14
brew services start postgresql@14

# Create database

createdb -U postgres notes_app
```


#### disable PostgreSQL login password request for local users

edit C:\Program Files\PostgreSQL\18\data\pg_hba.conf

from
```
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# "local" is for Unix domain socket connections only
local   all             all                                     scram-sha-256
# IPv4 local connections:
host    all             all             127.0.0.1/32            scram-sha-256
# IPv6 local connections:
host    all             all             ::1/128                 scram-sha-256
```    

to
```
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# "local" is for Unix domain socket connections only
local   all             all                                     trust
# IPv4 local connections:
host    all             all             127.0.0.1/32            trust
# IPv6 local connections:
host    all             all             ::1/128                 trust
```


delete PostgreSQL default user (postgres) password

`echo "ALTER USER username WITH PASSWORD NULL;" | psql -U postgres`

using echo otherwise, we get codepage errors.


### 2. Setup Backend
```bash
cd backend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env with your settings:
# - Database credentials
# - JWT secret (generate a random string)
# - SMTP settings for email

# Run database migration
npm run migrate

# Start development server
npm run dev
```

Server will run on http://localhost:3000

### 3. Test Backend
```bash
# Health check
curl http://localhost:3000/health

# Should return: {"status":"ok","timestamp":"..."}
```

## Android App Setup

### 1. Prerequisites
- Install PostgreSQL Database 
  - `brew install postgresql` for mac
  - `choco install postgresql` for Windows
- Install OpenJDK 17 
  - `brew install openjdk@17` for mac 
  - `choco install microsoft-openjdk17` for Windows
- Install Android Studio
  - https://developer.android.com/studio
- Set up Android SDK (API 24+)
  - Mostly automatic
- Create an emulator or connect a physical device
  - You get *Medium Phone* by default

### 2. Configure API URL

Open `frontend/android/app/build.gradle` and update:

```gradle
buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:3000\""
```

**Important Notes:**
- Use `10.0.2.2` for Android Emulator (not `localhost`)
  - Android Emulator running in its own Virutal Machine connects to the host (laptop) using `10.0.2.2`
- Use your computer's IP address for physical device (e.g., `192.168.1.100`)
- Make sure backend is running before testing the app

### 3. Build and Run

```bash
cd frontend/android

# Open in Android Studio
# OR build from command line:

./gradlew assembleDebug
./gradlew installDebug
```

## Email Configuration (for Registration)

### Using Gmail

1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password:
   - Go to Google Account Settings
   - Security → 2-Step Verification → App Passwords
   - Generate password for "Mail"
3. Use the app password in `.env`:
```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=generated-app-password
```

### Using Other SMTP Providers

Update SMTP settings accordingly:
- Outlook: `smtp-mail.outlook.com:587`
- Yahoo: `smtp.mail.yahoo.com:587`
- Custom SMTP: Use your provider's settings

## Testing the Full Flow

### 1. Register a User
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Abcd1234",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Check Email
- You should receive a verification email
- Click the verification link or copy the code

### 3. Verify Email
```bash
curl "http://localhost:3000/api/auth/verify-email?code=YOUR_CODE"
```

### 4. Login from Mobile App
- Open the app
- Click "Register" or "Login"
- Use the credentials from step 1
- After verification, you can login

### 5. Test Offline Mode
- Create a note
- Turn on Airplane Mode
- Create/edit notes (they should work offline)
- Turn off Airplane Mode
- Pull to refresh (notes should sync)

## Common Issues

### Backend

**Database connection error**
- Check PostgreSQL is running: `brew services list`
- Verify database exists: `psql -l`
- Check credentials in `.env`

**Email not sending**
- Verify SMTP credentials
- Check firewall settings
- Try a different SMTP port (465 for SSL)

### Android App

**Cannot connect to backend**
- Use `10.0.2.2` for emulator, not `localhost`
- For physical device, use computer's IP address
- Check backend is running: `curl http://localhost:3000/health`
- Verify firewall allows connections

**Build errors**
- Clean project: Build → Clean Project
- Invalidate caches: File → Invalidate Caches
- Sync Gradle: File → Sync Project with Gradle Files

**Room database errors**
- Clear app data in device settings
- Uninstall and reinstall app

## Development Tips

### Backend
- Use `npm run dev` for auto-restart with nodemon
- Check logs in terminal for errors
- Use Postman or curl for API testing

### Android
- Enable Logcat filtering: Set filter to "Notes" or "Sync"
- Use Database Inspector to view SQLite data
- Test offline mode with emulator network settings
- Use WorkManager Inspector to see sync jobs

## Next Steps

1. **Customize the App**
   - Change app name and icon
   - Customize colors in themes
   - Add additional features

2. **Deploy to Production**
   - Set up cloud PostgreSQL (AWS RDS, Heroku, etc.)
   - Deploy backend to cloud (Heroku, DigitalOcean, AWS)
   - Update mobile app with production API URL
   - Generate signed APK for release

3. **Enhancements**
   - Add rich text editor
   - Implement image attachments
   - Add reminder notifications
   - Create widgets

## Support

For issues or questions:
1. Check the README files in `backend/` and `frontend/`
2. Review the code comments
3. Check error logs
4. Verify all prerequisites are installed
