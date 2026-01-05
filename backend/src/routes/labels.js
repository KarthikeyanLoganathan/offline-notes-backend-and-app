const express = require('express');
const { body, validationResult } = require('express-validator');
const authenticate = require('../middleware/auth');
const labelsService = require('../services/labelsService');

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

// Create label
router.post('/',
  [
    body('name').trim().notEmpty(),
    body('color').optional().matches(/^#[0-9A-Fa-f]{6}$/),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const label = await labelsService.createLabel(req.user.id, req.body);
      res.status(201).json(label);
    } catch (error) {
      console.error('Create label error:', error);
      res.status(400).json({ error: error.message });
    }
  }
);

// Get all labels
router.get('/', async (req, res) => {
  try {
    const labels = await labelsService.getUserLabels(req.user.id);
    res.json(labels);
  } catch (error) {
    console.error('Get labels error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get single label
router.get('/:id', async (req, res) => {
  try {
    const label = await labelsService.getLabelById(req.user.id, req.params.id);
    res.json(label);
  } catch (error) {
    console.error('Get label error:', error);
    res.status(404).json({ error: error.message });
  }
});

// Update label
router.put('/:id',
  [
    body('name').optional().trim().notEmpty(),
    body('color').optional().matches(/^#[0-9A-Fa-f]{6}$/),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const label = await labelsService.updateLabel(req.user.id, req.params.id, req.body);
      res.json(label);
    } catch (error) {
      console.error('Update label error:', error);
      res.status(400).json({ error: error.message });
    }
  }
);

// Delete label
router.delete('/:id', async (req, res) => {
  try {
    const result = await labelsService.deleteLabel(req.user.id, req.params.id);
    res.json(result);
  } catch (error) {
    console.error('Delete label error:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
