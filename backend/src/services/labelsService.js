const db = require('../db');

class LabelsService {
  // Create a new label
  async createLabel(userId, labelData) {
    const { id, name, color } = labelData;
    
    try {
      // Accept client-generated UUID if provided (for offline sync)
      let result;
      if (id) {
        result = await db.query(
          `INSERT INTO labels (id, user_id, name, color)
           VALUES ($1, $2, $3, $4)
           RETURNING id, user_id, name, color, created_at`,
          [id, userId, name, color || '#808080']
        );
      } else {
        result = await db.query(
          `INSERT INTO labels (user_id, name, color)
           VALUES ($1, $2, $3)
           RETURNING id, user_id, name, color, created_at`,
          [userId, name, color || '#808080']
        );
      }
      
      return result.rows[0];
    } catch (error) {
      if (error.code === '23505') { // Unique constraint violation
        throw new Error('Label with this name already exists');
      }
      throw error;
    }
  }
  
  // Get all labels for user
  async getUserLabels(userId) {
    const result = await db.query(
      `SELECT l.id, l.name, l.color, l.created_at,
              COUNT(nl.note_id) as note_count
       FROM labels l
       LEFT JOIN note_labels nl ON l.id = nl.label_id
       LEFT JOIN notes n ON nl.note_id = n.id AND n.is_deleted = FALSE
       WHERE l.user_id = $1
       GROUP BY l.id
       ORDER BY l.name`,
      [userId]
    );
    
    return result.rows;
  }
  
  // Get label by ID
  async getLabelById(userId, labelId) {
    const result = await db.query(
      `SELECT l.id, l.name, l.color, l.created_at,
              COUNT(nl.note_id) as note_count
       FROM labels l
       LEFT JOIN note_labels nl ON l.id = nl.label_id
       LEFT JOIN notes n ON nl.note_id = n.id AND n.is_deleted = FALSE
       WHERE l.id = $1 AND l.user_id = $2
       GROUP BY l.id`,
      [labelId, userId]
    );
    
    if (result.rows.length === 0) {
      throw new Error('Label not found');
    }
    
    return result.rows[0];
  }
  
  // Update label
  async updateLabel(userId, labelId, updates) {
    const { name, color } = updates;
    
    try {
      const result = await db.query(
        `UPDATE labels
         SET name = COALESCE($1, name),
             color = COALESCE($2, color)
         WHERE id = $3 AND user_id = $4
         RETURNING id, user_id, name, color, created_at`,
        [name, color, labelId, userId]
      );
      
      if (result.rows.length === 0) {
        throw new Error('Label not found');
      }
      
      return result.rows[0];
    } catch (error) {
      if (error.code === '23505') {
        throw new Error('Label with this name already exists');
      }
      throw error;
    }
  }
  
  // Delete label
  async deleteLabel(userId, labelId) {
    const result = await db.query(
      'DELETE FROM labels WHERE id = $1 AND user_id = $2 RETURNING id',
      [labelId, userId]
    );
    
    if (result.rows.length === 0) {
      throw new Error('Label not found');
    }
    
    return { message: 'Label deleted successfully' };
  }
}

module.exports = new LabelsService();
