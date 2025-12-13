# REST Client Test Files

This folder contains `.http` files for testing the Notes Application API using the [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) VS Code extension.

## Setup

1. **Install REST Client Extension**:
   - Open VS Code
   - Go to Extensions (⌘+Shift+X)
   - Search for "REST Client" by Huachao Mao
   - Click Install

2. **Start the Backend Server**:
   ```bash
   cd backend
   npm run dev
   ```

3. **Update Variables**:
   After registration and login, update the `@authToken` variable in each file with your actual JWT token.

## Test Files

### `auth.http`
Authentication endpoints including:
- User registration with address details
- Email verification
- Login/Logout
- Example payloads for multiple users

### `users.http`
User management endpoints:
- Get user profile
- Update profile (full and partial)
- Get active sessions
- Unauthorized access tests

### `notes.http`
Notes CRUD operations:
- Create notes (with/without labels)
- Get all notes (with pagination)
- Get single note
- Search notes (full-text search)
- Update notes (full and partial)
- Soft delete and restore
- Permanent delete
- Filter deleted notes

### `labels.http`
Label management:
- Create labels with colors
- Get all labels
- Get single label
- Update labels (name/color)
- Delete labels
- Duplicate label test

### `sync.http`
Offline sync endpoints:
- Get global sync metadata
- Get label-specific sync metadata
- Get changed notes since timestamp
- Complete sync workflow example

## Usage

1. **Open any `.http` file** in VS Code

2. **Click "Send Request"** link above any request, or:
   - Place cursor on the request
   - Press `⌘+Alt+R` (macOS) or `Ctrl+Alt+R` (Windows/Linux)

3. **View Response** in the right panel

## Workflow Example

1. **Register a user** (`auth.http`):
   ```
   POST http://localhost:3000/api/auth/register
   ```

2. **Check console logs** for verification code

3. **Verify email** (`auth.http`):
   - Update `@verificationCode` variable
   - Send GET request to verify endpoint

4. **Login** (`auth.http`):
   ```
   POST http://localhost:3000/api/auth/login
   ```

5. **Copy JWT token** from response

6. **Update `@authToken`** in all test files

7. **Create labels** (`labels.http`):
   - Create "Work", "Personal", etc.

8. **Create notes** (`notes.http`):
   - Create notes with/without labels

9. **Test sync** (`sync.http`):
   - Get sync metadata
   - Get changes

## Tips

- **Variables**: Define once at the top, use throughout the file with `{{variableName}}`
- **Environments**: Create `rest-client.env.json` for different environments
- **Chaining**: Use response values in subsequent requests
- **Comments**: Lines starting with `#` or `//` are comments
- **Separators**: Use `###` to separate requests

## Common Variables

```http
@baseUrl = http://localhost:3000
@contentType = application/json
@authToken = your-jwt-token-here
@noteId = 1
@labelId = 1
```

Update these after creating resources to test specific operations.

## Troubleshooting

- **Connection refused**: Make sure backend server is running (`npm run dev`)
- **401 Unauthorized**: Update `@authToken` with valid JWT token
- **404 Not Found**: Check if the resource ID exists
- **500 Server Error**: Check backend console logs for details

## VS Code REST Client Features

- **Syntax highlighting** for HTTP requests
- **Auto-completion** for headers and methods
- **Response preview** with formatting
- **History** of requests
- **Code generation** for various languages
- **Environment variables** support
- **Request cancellation**

Enjoy testing! 🚀
