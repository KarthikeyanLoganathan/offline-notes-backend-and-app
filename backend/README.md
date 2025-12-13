# Notes Application Backend

Backend API for the Notes Application with offline sync capabilities.

## Features

- User registration with email verification
- Session-based authentication with JWT
- Notes CRUD operations with soft delete
- Label management
- Full-text search across notes
- Sync metadata for offline support
- Deleted notes retention (configurable, default 90 days)

## Prerequisites

- Node.js 16+
- PostgreSQL 12+

## Setup

1. Install dependencies:
```bash
npm install
```

2. Create a `.env` file based on `.env.example`:
```bash
cp .env.example .env
```

3. Configure your database and email settings in `.env`

4. Create the database:
```bash
dropdb notes_app
createdb notes_app
```

5. Run migrations:
```bash
npm run migrate
```

6. Start the server:
```bash
# Development
npm run dev

# Production
npm start
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `GET /api/auth/verify-email?code=` - Verify email
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout

### Users
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile
- `GET /api/users/sessions` - Get active sessions

### Notes
- `POST /api/notes` - Create note
- `GET /api/notes` - Get all notes (global)
- `GET /api/notes/label/:labelId` - Get notes by label
- `GET /api/notes/search?q=` - Search notes
- `GET /api/notes/:id` - Get single note
- `PUT /api/notes/:id` - Update note
- `DELETE /api/notes/:id` - Delete note (soft)
- `DELETE /api/notes/:id/permanent` - Permanently delete
- `POST /api/notes/:id/restore` - Restore deleted note

### Sync Endpoints
- `GET /api/notes/sync/global` - Get global sync metadata
- `GET /api/notes/sync/label/:labelId` - Get label sync metadata
- `GET /api/notes/sync/changes?since=` - Get changed notes

### Labels
- `POST /api/labels` - Create label
- `GET /api/labels` - Get all labels
- `GET /api/labels/:id` - Get single label
- `PUT /api/labels/:id` - Update label
- `DELETE /api/labels/:id` - Delete label

## Session Management

The application uses a hybrid approach for session management:
- **JWT tokens** for stateless authentication
- **Database sessions** for tracking active devices and enabling remote logout
- **HTTP-only cookies** for web clients (with token fallback for mobile)

This provides the best of both worlds:
- Scalability of JWT
- Control and visibility of database sessions
- Security through HTTP-only cookies

## Database Schema

See `src/db/schema.sql` for the complete database schema including:
- Users table with email verification
- User sessions table for multi-device support
- Notes table with soft delete
- Labels table
- Note-labels junction table (many-to-many)
- User sync metadata for offline support

## License

MIT
