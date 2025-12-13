const db = require('../db');

class NotesService {
  // Create a new note
  async createNote(userId, noteData) {
    const { id, title, content, labelIds } = noteData;
    
    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');
      
      // Insert note (accept client-generated UUID if provided for offline sync)
      let noteResult;
      if (id) {
        noteResult = await client.query(
          `INSERT INTO notes (id, user_id, title, content)
           VALUES ($1, $2, $3, $4)
           RETURNING id, user_id, title, content, is_deleted, created_at, updated_at`,
          [id, userId, title, content]
        );
      } else {
        noteResult = await client.query(
          `INSERT INTO notes (user_id, title, content)
           VALUES ($1, $2, $3)
           RETURNING id, user_id, title, content, is_deleted, created_at, updated_at`,
          [userId, title, content]
        );
      }
      
      const note = noteResult.rows[0];
      
      // Attach labels if provided
      if (labelIds && labelIds.length > 0) {
        for (const labelId of labelIds) {
          await client.query(
            'INSERT INTO note_labels (note_id, label_id) VALUES ($1, $2)',
            [note.id, labelId]
          );
        }
      }
      
      await client.query('COMMIT');
      
      // Fetch complete note with labels
      return await this.getNoteById(userId, note.id);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
  
  // Get note by ID
  async getNoteById(userId, noteId) {
    const noteResult = await db.query(
      `SELECT id, user_id, title, content, is_deleted, created_at, updated_at
       FROM notes
       WHERE id = $1 AND user_id = $2`,
      [noteId, userId]
    );
    
    if (noteResult.rows.length === 0) {
      throw new Error('Note not found');
    }
    
    const note = noteResult.rows[0];
    
    // Get labels
    const labelsResult = await db.query(
      `SELECT l.id, l.name, l.color
       FROM labels l
       JOIN note_labels nl ON l.id = nl.label_id
       WHERE nl.note_id = $1`,
      [noteId]
    );
    
    note.labels = labelsResult.rows;
    
    return note;
  }
  
  // Update note
  async updateNote(userId, noteId, updates) {
    const { title, content, labelIds } = updates;
    
    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');
      
      // Update note
      const noteResult = await client.query(
        `UPDATE notes
         SET title = COALESCE($1, title),
             content = COALESCE($2, content),
             updated_at = NOW()
         WHERE id = $3 AND user_id = $4 AND is_deleted = FALSE
         RETURNING id, user_id, title, content, is_deleted, created_at, updated_at`,
        [title, content, noteId, userId]
      );
      
      if (noteResult.rows.length === 0) {
        throw new Error('Note not found or already deleted');
      }
      
      // Update labels if provided
      if (labelIds !== undefined) {
        // Remove existing labels
        await client.query('DELETE FROM note_labels WHERE note_id = $1', [noteId]);
        
        // Add new labels
        if (labelIds.length > 0) {
          for (const labelId of labelIds) {
            await client.query(
              'INSERT INTO note_labels (note_id, label_id) VALUES ($1, $2)',
              [noteId, labelId]
            );
          }
        }
      }
      
      await client.query('COMMIT');
      
      return await this.getNoteById(userId, noteId);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
  
  // Delete note (soft delete)
  async deleteNote(userId, noteId) {
    const result = await db.query(
      `UPDATE notes
       SET is_deleted = TRUE, deleted_at = NOW(), updated_at = NOW()
       WHERE id = $1 AND user_id = $2 AND is_deleted = FALSE
       RETURNING id`,
      [noteId, userId]
    );
    
    if (result.rows.length === 0) {
      throw new Error('Note not found or already deleted');
    }
    
    return { message: 'Note deleted successfully' };
  }
  
  // Permanently delete note
  async permanentlyDeleteNote(userId, noteId) {
    const result = await db.query(
      'DELETE FROM notes WHERE id = $1 AND user_id = $2 RETURNING id',
      [noteId, userId]
    );
    
    if (result.rows.length === 0) {
      throw new Error('Note not found');
    }
    
    return { message: 'Note permanently deleted' };
  }
  
  // Restore deleted note
  async restoreNote(userId, noteId) {
    const result = await db.query(
      `UPDATE notes
       SET is_deleted = FALSE, deleted_at = NULL, updated_at = NOW()
       WHERE id = $1 AND user_id = $2 AND is_deleted = TRUE
       RETURNING id`,
      [noteId, userId]
    );
    
    if (result.rows.length === 0) {
      throw new Error('Note not found or not deleted');
    }
    
    return await this.getNoteById(userId, noteId);
  }
  
  // Get all notes (global list) sorted by last changed time
  async getNotes(userId, options = {}) {
    const { includeDeleted = false, limit = 100, offset = 0 } = options;
    
    const query = `
      SELECT n.id, n.user_id, n.title, n.content, n.is_deleted, n.created_at, n.updated_at,
             json_agg(json_build_object('id', l.id, 'name', l.name, 'color', l.color)) 
             FILTER (WHERE l.id IS NOT NULL) as labels
      FROM notes n
      LEFT JOIN note_labels nl ON n.id = nl.note_id
      LEFT JOIN labels l ON nl.label_id = l.id
      WHERE n.user_id = $1 AND n.is_deleted = $2
      GROUP BY n.id
      ORDER BY n.updated_at DESC
      LIMIT $3 OFFSET $4
    `;
    
    const result = await db.query(query, [userId, includeDeleted, limit, offset]);
    
    return result.rows.map(note => ({
      ...note,
      labels: note.labels || []
    }));
  }
  
  // Get notes by label
  async getNotesByLabel(userId, labelId, options = {}) {
    const { limit = 100, offset = 0 } = options;
    
    const query = `
      SELECT n.id, n.user_id, n.title, n.content, n.is_deleted, n.created_at, n.updated_at,
             json_agg(json_build_object('id', l.id, 'name', l.name, 'color', l.color)) 
             FILTER (WHERE l.id IS NOT NULL) as labels
      FROM notes n
      JOIN note_labels nl ON n.id = nl.note_id
      LEFT JOIN note_labels nl2 ON n.id = nl2.note_id
      LEFT JOIN labels l ON nl2.label_id = l.id
      WHERE n.user_id = $1 AND nl.label_id = $2 AND n.is_deleted = FALSE
      GROUP BY n.id
      ORDER BY n.updated_at DESC
      LIMIT $3 OFFSET $4
    `;
    
    const result = await db.query(query, [userId, labelId, limit, offset]);
    
    return result.rows.map(note => ({
      ...note,
      labels: note.labels || []
    }));
  }
  
  // Search notes
  async searchNotes(userId, searchQuery, options = {}) {
    const { includeDeleted = false, limit = 100 } = options;
    
    const query = `
      SELECT n.id, n.user_id, n.title, n.content, n.is_deleted, n.created_at, n.updated_at,
             json_agg(json_build_object('id', l.id, 'name', l.name, 'color', l.color)) 
             FILTER (WHERE l.id IS NOT NULL) as labels
      FROM notes n
      LEFT JOIN note_labels nl ON n.id = nl.note_id
      LEFT JOIN labels l ON nl.label_id = l.id
      WHERE n.user_id = $1 
        AND n.is_deleted = $2
        AND (n.title ILIKE $3 OR n.content ILIKE $3)
      GROUP BY n.id
      ORDER BY n.updated_at DESC
      LIMIT $4
    `;
    
    const searchPattern = `%${searchQuery}%`;
    const result = await db.query(query, [userId, includeDeleted, searchPattern, limit]);
    
    return result.rows.map(note => ({
      ...note,
      labels: note.labels || []
    }));
  }
  
  // Get sync metadata for global list
  async getGlobalSyncMetadata(userId) {
    const result = await db.query(
      `SELECT last_change_timestamp
       FROM user_sync_metadata
       WHERE user_id = $1 AND metadata_type = 'global' AND label_id IS NULL`,
      [userId]
    );
    
    if (result.rows.length === 0) {
      // Initialize if doesn't exist
      await db.query(
        `INSERT INTO user_sync_metadata (user_id, metadata_type, last_change_timestamp)
         VALUES ($1, 'global', NOW())
         ON CONFLICT (user_id, metadata_type, label_id) DO NOTHING`,
        [userId]
      );
      return { lastChangeTimestamp: new Date() };
    }
    
    return { lastChangeTimestamp: result.rows[0].last_change_timestamp };
  }
  
  // Get sync metadata for label
  async getLabelSyncMetadata(userId, labelId) {
    const result = await db.query(
      `SELECT last_change_timestamp
       FROM user_sync_metadata
       WHERE user_id = $1 AND metadata_type = 'label' AND label_id = $2`,
      [userId, labelId]
    );
    
    if (result.rows.length === 0) {
      // Initialize if doesn't exist
      await db.query(
        `INSERT INTO user_sync_metadata (user_id, metadata_type, label_id, last_change_timestamp)
         VALUES ($1, 'label', $2, NOW())
         ON CONFLICT (user_id, metadata_type, label_id) DO NOTHING`,
        [userId, labelId]
      );
      return { lastChangeTimestamp: new Date() };
    }
    
    return { lastChangeTimestamp: result.rows[0].last_change_timestamp };
  }
  
  // Get notes changed since timestamp
  async getNotesChangedSince(userId, timestamp, labelId = null) {
    let query;
    let params;
    
    if (labelId) {
      query = `
        SELECT n.id, n.user_id, n.title, n.content, n.is_deleted, n.created_at, n.updated_at,
               json_agg(json_build_object('id', l.id, 'name', l.name, 'color', l.color)) 
               FILTER (WHERE l.id IS NOT NULL) as labels
        FROM notes n
        JOIN note_labels nl ON n.id = nl.note_id
        LEFT JOIN note_labels nl2 ON n.id = nl2.note_id
        LEFT JOIN labels l ON nl2.label_id = l.id
        WHERE n.user_id = $1 AND nl.label_id = $2 AND n.updated_at > $3
        GROUP BY n.id
        ORDER BY n.updated_at DESC
      `;
      params = [userId, labelId, timestamp];
    } else {
      query = `
        SELECT n.id, n.user_id, n.title, n.content, n.is_deleted, n.created_at, n.updated_at,
               json_agg(json_build_object('id', l.id, 'name', l.name, 'color', l.color)) 
               FILTER (WHERE l.id IS NOT NULL) as labels
        FROM notes n
        LEFT JOIN note_labels nl ON n.id = nl.note_id
        LEFT JOIN labels l ON nl.label_id = l.id
        WHERE n.user_id = $1 AND n.updated_at > $2
        GROUP BY n.id
        ORDER BY n.updated_at DESC
      `;
      params = [userId, timestamp];
    }
    
    const result = await db.query(query, params);
    
    return result.rows.map(note => ({
      ...note,
      labels: note.labels || []
    }));
  }
}

module.exports = new NotesService();
