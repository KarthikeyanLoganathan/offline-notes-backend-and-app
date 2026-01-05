const express = require('express');
const { body, query, validationResult } = require('express-validator');
const authenticate = require('../middleware/auth');
const notesService = require('../services/notesService');

const router = express.Router();

// Validation middleware
const validateRequest = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({ errors: errors.array() });
  }
  next();
};

// All routes require authentication
router.use(authenticate);

// Create note
router.post('/',
  [
    body('title').optional().trim(),
    body('content').optional().trim(),
    body('labelIds').optional().isArray(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const note = await notesService.createNote(req.user.id, req.body);
      res.status(201).json(note);
    } catch (error) {
      console.error('Create note error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Get all notes (global list)
router.get('/',
  [
    query('includeDeleted').optional().isBoolean().toBoolean(),
    query('limit').optional().isInt({ min: 1, max: 500 }).toInt(),
    query('offset').optional().isInt({ min: 0 }).toInt(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const notes = await notesService.getNotes(req.user.id, req.query);
      res.json(notes);
    } catch (error) {
      console.error('Get notes error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Get notes by label
router.get('/label/:labelId',
  [
    query('limit').optional().isInt({ min: 1, max: 500 }).toInt(),
    query('offset').optional().isInt({ min: 0 }).toInt(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const notes = await notesService.getNotesByLabel(req.user.id, req.params.labelId, req.query);
      res.json(notes);
    } catch (error) {
      console.error('Get notes by label error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Search notes
router.get('/search',
  [
    query('q').notEmpty().trim(),
    query('includeDeleted').optional().isBoolean().toBoolean(),
    query('limit').optional().isInt({ min: 1, max: 500 }).toInt(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const notes = await notesService.searchNotes(req.user.id, req.query.q, req.query);
      res.json(notes);
    } catch (error) {
      console.error('Search notes error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Get sync metadata (global)
router.get('/sync/global', async (req, res) => {
  try {
    const metadata = await notesService.getGlobalSyncMetadata(req.user.id);
    res.json(metadata);
  } catch (error) {
    console.error('Get global sync metadata error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get sync metadata (label)
router.get('/sync/label/:labelId', async (req, res) => {
  try {
    const metadata = await notesService.getLabelSyncMetadata(req.user.id, req.params.labelId);
    res.json(metadata);
  } catch (error) {
    console.error('Get label sync metadata error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get notes changed since timestamp
router.get('/sync/changes',
  [
    query('since').notEmpty().isISO8601(),
    // labels are UUIDs, not integers
    query('labelId').optional().isUUID(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const notes = await notesService.getNotesChangedSince(
        req.user.id,
        req.query.since,
        req.query.labelId
      );
      res.json(notes);
    } catch (error) {
      console.error('Get changed notes error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Get single note
router.get('/:id', async (req, res) => {
  try {
    const note = await notesService.getNoteById(req.user.id, req.params.id);
    res.json(note);
  } catch (error) {
    console.error('Get note error:', error);
    res.status(404).json({ error: error.message });
  }
});

// Update note
router.put('/:id',
  [
    body('title').optional().trim(),
    body('content').optional().trim(),
    body('labelIds').optional().isArray(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const note = await notesService.updateNote(req.user.id, req.params.id, req.body);
      res.json(note);
    } catch (error) {
      console.error('Update note error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Delete note (soft delete)
router.delete('/:id', async (req, res) => {
  try {
    const result = await notesService.deleteNote(req.user.id, req.params.id);
    res.json(result);
  } catch (error) {
    console.error('Delete note error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Permanently delete note
router.delete('/:id/permanent', async (req, res) => {
  try {
    const result = await notesService.permanentlyDeleteNote(req.user.id, req.params.id);
    res.json(result);
  } catch (error) {
    console.error('Permanent delete note error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Restore deleted note
router.post('/:id/restore', async (req, res) => {
  try {
    const note = await notesService.restoreNote(req.user.id, req.params.id);
    res.json(note);
  } catch (error) {
    console.error('Restore note error:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
