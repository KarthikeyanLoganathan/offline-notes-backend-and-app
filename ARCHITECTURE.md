# System Architecture

## Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Mobile App                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Presentation Layer                   │  │
│  │  ├─ LoginActivity                                      │  │
│  │  ├─ RegisterActivity                                   │  │
│  │  ├─ MainActivity (Notes List)                          │  │
│  │  └─ NoteDetailActivity (Create/Edit)                   │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    Domain Layer                        │  │
│  │  ├─ NotesRepository                                    │  │
│  │  └─ AuthRepository                                     │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌──────────────────────┬────────────────────────────────┐  │
│  │   Local Data Source  │   Remote Data Source           │  │
│  │  ┌────────────────┐  │  ┌──────────────────────────┐  │  │
│  │  │  Room Database │  │  │  Retrofit + OkHttp       │  │  │
│  │  │  ┌──────────┐  │  │  │  ┌────────────────────┐  │  │  │
│  │  │  │ SQLite   │  │  │  │  │ ApiService         │  │  │  │
│  │  │  │ (Offline)│  │  │  │  │ (REST API calls)   │  │  │  │
│  │  │  └──────────┘  │  │  │  └────────────────────┘  │  │  │
│  │  └────────────────┘  │  └──────────────────────────┘  │  │
│  └──────────────────────┴────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Background Processing                     │  │
│  │  └─ SyncWorker (WorkManager - Every 15 min)           │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               │
                               │ HTTPS/REST API
                               │ JSON
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Backend API Server                       │
│                      (Node.js + Express)                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                    API Routes                          │  │
│  │  ├─ /api/auth/* (Authentication)                      │  │
│  │  ├─ /api/users/* (User Management)                    │  │
│  │  ├─ /api/notes/* (Notes CRUD + Search)               │  │
│  │  ├─ /api/labels/* (Labels Management)                │  │
│  │  └─ /api/notes/sync/* (Sync Endpoints)               │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Middleware Layer                     │  │
│  │  ├─ Authentication (JWT Validation)                   │  │
│  │  ├─ Request Validation (express-validator)           │  │
│  │  ├─ CORS Handler                                      │  │
│  │  └─ Error Handler                                     │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Service Layer                        │  │
│  │  ├─ UserService (Registration, Login, Profile)       │  │
│  │  ├─ NotesService (CRUD, Search, Sync)                │  │
│  │  ├─ LabelsService (Label Management)                 │  │
│  │  └─ EmailService (Verification Emails)               │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Database Layer                       │  │
│  │  └─ PostgreSQL Connection Pool (pg)                  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               │
                               │ SQL
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Tables:                                              │  │
│  │  ├─ users (User accounts + verification)             │  │
│  │  ├─ user_sessions (Active sessions)                  │  │
│  │  ├─ notes (Notes with soft delete)                   │  │
│  │  ├─ labels (User labels)                             │  │
│  │  ├─ note_labels (Many-to-many junction)              │  │
│  │  └─ user_sync_metadata (Sync timestamps)             │  │
│  │                                                        │  │
│  │  Triggers:                                            │  │
│  │  ├─ update_updated_at_column()                       │  │
│  │  └─ update_sync_metadata()                           │  │
│  │                                                        │  │
│  │  Functions:                                           │  │
│  │  ├─ cleanup_old_deleted_notes()                      │  │
│  │  └─ cleanup_expired_sessions()                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. User Registration Flow
```
User fills form → RegisterActivity
                      ↓
                  Validates input
                      ↓
                  AuthRepository
                      ↓
              POST /api/auth/register
                      ↓
                  UserService
                      ↓
              Hash password (bcrypt)
                      ↓
              Generate verification code
                      ↓
              Insert into users table
                      ↓
              EmailService sends email
                      ↓
              User receives email
                      ↓
              Click verification link
                      ↓
              GET /api/auth/verify-email
                      ↓
              Update email_verified = true
                      ↓
              Account activated!
```

### 2. Login Flow
```
User enters credentials → LoginActivity
                              ↓
                          Validates input
                              ↓
                          AuthRepository
                              ↓
                      POST /api/auth/login
                              ↓
                          UserService
                              ↓
                  Verify password (bcrypt)
                              ↓
                  Generate JWT token
                              ↓
                  Create session record
                              ↓
                  Return token + user data
                              ↓
                  Save to Room database
                              ↓
                  Set token in RetrofitClient
                              ↓
                  Navigate to MainActivity
```

### 3. Create Note (Offline) Flow
```
User writes note → NoteDetailActivity
                          ↓
                      Clicks Save
                          ↓
                  NotesRepository.createNote()
                          ↓
                  Check network status
                          ↓
                  [OFFLINE DETECTED]
                          ↓
              Save to Room with syncStatus=1
                          ↓
              Generate local UUID
                          ↓
              Insert into local notes table
                          ↓
              Show success to user
                          ↓
              [Background - 15 min later]
                          ↓
              SyncWorker runs
                          ↓
              Detect unsaved notes
                          ↓
              POST /api/notes
                          ↓
              Server assigns real ID
                          ↓
              Update local note with server ID
                          ↓
              Set syncStatus=0 (synced)
                          ↓
              Note now on server!
```

### 4. Sync Flow (Detailed)
```
WorkManager schedules sync (every 15 min)
                ↓
        Check network available
                ↓
        [NETWORK AVAILABLE]
                ↓
    Get current user session
                ↓
    NotesRepository.syncNotes()
                ↓
┌───────────────┴────────────────┐
│                                │
▼ PULL (Server → Local)          ▼ PUSH (Local → Server)
│                                │
Get last sync timestamp          Get unsynced notes (syncStatus != 0)
│                                │
GET /api/notes/sync/changes      ├─ syncStatus=1 (pending_create)
│                                │  POST /api/notes
Fetch notes since last sync      │  → Get server ID
│                                │  → Update local note
Insert/Update in Room            │  → Set syncStatus=0
│                                │
Delete local notes not in server ├─ syncStatus=2 (pending_update)
│                                │  PUT /api/notes/:id
Update labels                    │  → Confirm update
│                                │  → Set syncStatus=0
└────────────────┬───────────────┤
                 ▼               └─ syncStatus=3 (pending_delete)
    Update last sync timestamp      DELETE /api/notes/:id
                 ↓                  → Confirm deletion
    Sync completed!                 → Set syncStatus=0
                 ↓
    Update UI (LiveData)
```

## Database Schemas

### Backend (PostgreSQL)

```sql
users
├─ id (PK)
├─ email (UNIQUE)
├─ password_hash
├─ first_name
├─ last_name
├─ address_line1
├─ address_line2
├─ city, state, country, postal_code
├─ email_verified
├─ verification_code
├─ verification_expires
├─ created_at
└─ updated_at

user_sessions
├─ id (PK)
├─ user_id (FK → users)
├─ session_token (UNIQUE)
├─ device_info
├─ ip_address
├─ user_agent
├─ expires_at
├─ created_at
└─ last_accessed

notes
├─ id (PK)
├─ user_id (FK → users)
├─ title
├─ content
├─ is_deleted
├─ deleted_at
├─ created_at
└─ updated_at

labels
├─ id (PK)
├─ user_id (FK → users)
├─ name
├─ color
└─ created_at

note_labels
├─ note_id (FK → notes)
└─ label_id (FK → labels)
    (Composite PK)

user_sync_metadata
├─ id (PK)
├─ user_id (FK → users)
├─ metadata_type ('global' | 'label')
├─ label_id (FK → labels, nullable)
└─ last_change_timestamp
```

### Mobile (SQLite via Room)

```kotlin
UserSessionEntity
├─ userId (PK)
├─ email
├─ sessionToken
├─ firstName
├─ lastName
├─ deviceInfo
├─ expiresAt
└─ createdAt

NoteEntity
├─ id (PK)
├─ userId
├─ title
├─ content
├─ isDeleted
├─ deletedAt
├─ createdAt
├─ updatedAt
├─ syncStatus (0=synced, 1=create, 2=update, 3=delete)
└─ localId (UUID for offline notes)

LabelEntity
├─ id (PK)
├─ userId
├─ name
├─ color
├─ createdAt
└─ syncStatus

NoteLabelCrossRef
├─ noteId (FK → NoteEntity)
└─ labelId (FK → LabelEntity)
    (Composite PK)

SyncMetadataEntity
├─ id (PK, auto)
├─ userId
├─ metadataType ('global' | 'label')
├─ labelId (nullable)
├─ lastChangeTimestamp
└─ lastSyncTimestamp
```

## Network Architecture

```
┌─────────────┐
│ Android App │
└──────┬──────┘
       │
       │ HTTPS
       │ REST API
       │ JSON
       │
┌──────▼──────────────────────┐
│   Reverse Proxy (Nginx)     │ (Production)
│   - SSL/TLS Termination     │
│   - Load Balancing          │
└──────┬──────────────────────┘
       │
┌──────▼──────────────────────┐
│   Node.js API Server(s)     │
│   - Express                 │
│   - JWT Authentication      │
│   - Request Validation      │
└──────┬──────────────────────┘
       │
       │ Connection Pool
       │
┌──────▼──────────────────────┐
│   PostgreSQL Database       │
│   - Primary/Replica         │
│   - Automated Backups       │
└─────────────────────────────┘
```

## Security Architecture

```
┌──────────────────────────────────────────┐
│             Request Flow                 │
└──────────────────────────────────────────┘

Client Request
    ↓
[1] HTTPS/TLS Encryption
    ↓
[2] CORS Validation
    ↓
[3] JWT Token Extraction
    ├─ From Authorization header
    └─ Or from HTTP-only cookie
    ↓
[4] JWT Signature Verification
    ├─ Verify with JWT_SECRET
    └─ Check expiration
    ↓
[5] Database Session Validation
    ├─ Query user_sessions table
    ├─ Check expires_at
    └─ Update last_accessed
    ↓
[6] Request Authorization
    ├─ Extract userId from token
    └─ Verify resource ownership
    ↓
[7] Input Validation
    ├─ express-validator
    └─ Type checking
    ↓
[8] SQL Injection Prevention
    └─ Parameterized queries
    ↓
[9] Business Logic
    └─ Service layer processing
    ↓
[10] Response
    └─ JSON data (sensitive fields filtered)
```

## Deployment Architecture (Production)

```
┌────────────────────────────────────────────────┐
│              Load Balancer (AWS ELB)           │
└───────────────────┬────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │                       │
┌───────▼────────┐     ┌───────▼────────┐
│  API Server 1  │     │  API Server 2  │
│  (EC2/ECS)     │     │  (EC2/ECS)     │
└───────┬────────┘     └───────┬────────┘
        │                       │
        └───────────┬───────────┘
                    │
        ┌───────────▼────────────┐
        │  PostgreSQL (RDS)      │
        │  - Multi-AZ            │
        │  - Automated Backups   │
        │  - Read Replicas       │
        └────────────────────────┘

┌────────────────────────────────────────────────┐
│              Supporting Services               │
├────────────────────────────────────────────────┤
│  - CloudWatch (Monitoring)                     │
│  - SES (Email Service)                         │
│  - S3 (Logs, Backups)                          │
│  - CloudFront (CDN for static assets)          │
│  - Route53 (DNS)                               │
└────────────────────────────────────────────────┘
```

---

This architecture provides:
- ✅ Scalability (horizontal scaling)
- ✅ High availability (multi-instance)
- ✅ Security (multiple layers)
- ✅ Performance (caching, connection pooling)
- ✅ Reliability (backups, monitoring)
- ✅ Offline-first capability (local SQLite)
