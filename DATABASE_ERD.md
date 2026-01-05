# Entity Relationship Diagram - Notes Application Database

## Database Schema Visualization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USERS                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ PK  id                    UUID                                              │
│     email                 VARCHAR(255)  UNIQUE                              │
│     password_hash         VARCHAR(255)                                      │
│     first_name            VARCHAR(100)                                      │
│     last_name             VARCHAR(100)                                      │
│     address_line1         VARCHAR(255)                                      │
│     address_line2         VARCHAR(255)                                      │
│     city                  VARCHAR(100)                                      │
│     state                 VARCHAR(100)                                      │
│     country               VARCHAR(100)                                      │
│     postal_code           VARCHAR(20)                                       │
│     email_verified        BOOLEAN       DEFAULT FALSE                       │
│     verification_code     VARCHAR(100)                                      │
│     verification_expires  TIMESTAMP                                         │
│     created_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP           │
│     updated_at            TIMESTAMP     DEFAULT CURRENT_TIMESTAMP           │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        │ *                         │ *                         │ *
        ▼                           ▼                           ▼
┌───────────────────┐      ┌────────────────┐        ┌──────────────────────┐
│  USER_SESSIONS    │      │    LABELS      │        │       NOTES          │
├───────────────────┤      ├────────────────┤        ├──────────────────────┤
│ PK  id       UUID │      │ PK  id    UUID │        │ PK  id          UUID │
│ FK  user_id  UUID │      │ FK  user_id    │        │ FK  user_id     UUID │
│ session_token     │      │     UUID       │        │     title       TEXT │
│ device_info  TEXT │      │ name  VARCHAR  │        │     content     TEXT │
│ ip_address   TEXT │      │ color VARCHAR  │        │     is_deleted  BOOL │
│ user_agent   TEXT │      │ created_at     │        │     deleted_at       │
│ expires_at        │      │                │        │     created_at       │
│ created_at        │      │ UNIQUE(user_id,│        │     updated_at       │
│ last_accessed     │      │        name)   │        └──────────────────────┘
└───────────────────┘      └────────────────┘                  │
                                    │                           │
                                    │                           │
                                    │                           │
                                    └──────────┬────────────────┘
                                               │
                                               │ M:N
                                               ▼
                                     ┌─────────────────────┐
                                     │   NOTE_LABELS       │
                                     │  (Junction Table)   │
                                     ├─────────────────────┤
                                     │ PK,FK note_id  UUID │
                                     │ PK,FK label_id UUID │
                                     │ created_at          │
                                     └─────────────────────┘

                    ┌────────────────────────────────────┐
                    │   USER_SYNC_METADATA               │
                    ├────────────────────────────────────┤
                    │ PK  id                    UUID     │
                    │ FK  user_id               UUID     │
                    │     metadata_type         VARCHAR  │
                    │ FK  label_id (optional)   UUID     │
                    │     last_change_timestamp          │
                    │                                    │
                    │ UNIQUE(user_id, metadata_type,    │
                    │        label_id)                   │
                    └────────────────────────────────────┘
                                    ▲
                                    │
                                    │ *
                    ┌───────────────┴──────────┐
                    │                          │
                    │                          │
            ┌───────┴────────┐        ┌───────┴────────┐
            │     USERS      │        │    LABELS      │
            │  (reference)   │        │  (reference)   │
            └────────────────┘        └────────────────┘
```

## Relationships

### 1. **users → user_sessions** (1:N)
- **Type:** One-to-Many
- **Description:** A user can have multiple active sessions (different devices)
- **Cascade:** ON DELETE CASCADE (sessions deleted when user is deleted)

### 2. **users → labels** (1:N)
- **Type:** One-to-Many
- **Description:** A user can create multiple labels
- **Cascade:** ON DELETE CASCADE (labels deleted when user is deleted)
- **Constraint:** UNIQUE(user_id, name) - No duplicate label names per user

### 3. **users → notes** (1:N)
- **Type:** One-to-Many
- **Description:** A user can create multiple notes
- **Cascade:** ON DELETE CASCADE (notes deleted when user is deleted)

### 4. **notes ↔ labels** (M:N)
- **Type:** Many-to-Many
- **Junction Table:** note_labels
- **Description:** A note can have multiple labels, and a label can be applied to multiple notes
- **Cascade:** ON DELETE CASCADE (junction records deleted when note or label is deleted)

### 5. **users → user_sync_metadata** (1:N)
- **Type:** One-to-Many
- **Description:** Tracks sync timestamps for users (global sync and per-label sync)
- **Cascade:** ON DELETE CASCADE (metadata deleted when user is deleted)
- **Optional FK:** label_id (NULL for global metadata, set for label-specific metadata)

## Key Features

### UUID Primary Keys
- All tables use UUID as primary keys
- Prevents ID collisions in offline sync scenarios
- Client can generate UUIDs independently

### Soft Delete Pattern
- `notes.is_deleted` and `notes.deleted_at` enable soft deletion
- Notes can be restored before permanent deletion

### Sync Metadata
- Tracks last change timestamps
- Supports incremental sync (only fetch changes since last sync)
- Global metadata for all notes
- Per-label metadata for filtered sync

### Indexes
- `idx_users_email` - Fast user lookup by email
- `idx_user_sessions_token` - Fast session validation
- `idx_user_sessions_user` - Fast session lookup per user
- `idx_notes_user` - Fast notes retrieval per user
- `idx_notes_updated` - Efficient ordering by update time
- `idx_notes_deleted` - Quick filtering of deleted notes
- `idx_labels_user` - Fast label retrieval per user
- `idx_note_labels_note` - Efficient label lookup for notes
- `idx_note_labels_label` - Efficient note lookup by label
- `idx_sync_metadata_user` - Fast sync metadata retrieval

## Cardinality Summary

```
users (1) ──────── (*) user_sessions
users (1) ──────── (*) labels
users (1) ──────── (*) notes
users (1) ──────── (*) user_sync_metadata

notes (*) ──────── (*) labels   [via note_labels junction]

labels (1) ──────── (*) user_sync_metadata  [optional]
```

## Database Triggers

### Auto-Update Timestamps
- `update_users_updated_at` - Updates `users.updated_at` on UPDATE
- `update_notes_updated_at` - Updates `notes.updated_at` on UPDATE

### Sync Metadata Triggers
- `notes_sync_metadata_trigger` - Automatically updates sync metadata when notes change
  - Updates global metadata for the user
  - Updates label-specific metadata for attached labels

## Data Integrity Constraints

1. **Email Uniqueness:** `users.email` must be unique
2. **Session Token Uniqueness:** `user_sessions.session_token` must be unique
3. **Label Name per User:** UNIQUE(user_id, name) in `labels` table
4. **Sync Metadata Uniqueness:** UNIQUE(user_id, metadata_type, label_id)
5. **Foreign Key Constraints:** All foreign keys have ON DELETE CASCADE

## Typical Query Patterns

### User Authentication
```sql
SELECT * FROM users WHERE email = ? AND email_verified = TRUE;
SELECT * FROM user_sessions WHERE session_token = ? AND expires_at > NOW();
```

### Note Retrieval
```sql
-- Get all notes for a user
SELECT * FROM notes WHERE user_id = ? AND is_deleted = FALSE ORDER BY updated_at DESC;

-- Get notes with labels
SELECT n.*, l.* FROM notes n
LEFT JOIN note_labels nl ON n.id = nl.note_id
LEFT JOIN labels l ON nl.label_id = l.id
WHERE n.user_id = ? AND n.is_deleted = FALSE;
```

### Sync Operations
```sql
-- Get notes changed since timestamp
SELECT * FROM notes 
WHERE user_id = ? AND updated_at > ?;

-- Update sync metadata
INSERT INTO user_sync_metadata (user_id, metadata_type, last_change_timestamp)
VALUES (?, 'global', NOW())
ON CONFLICT (user_id, metadata_type, label_id)
DO UPDATE SET last_change_timestamp = NOW();
```
