const express = require('express');
const { body, validationResult } = require('express-validator');
const authenticate = require('../middleware/auth');
const userService = require('../services/userService');

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

// Get user profile
router.get('/profile', async (req, res) => {
  try {
    const profile = await userService.getUserProfile(req.user.id);
    res.json(profile);
  } catch (error) {
    console.error('Get profile error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Update user profile
router.put('/profile',
  [
    body('firstName').optional().trim().notEmpty(),
    body('lastName').optional().trim().notEmpty(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const updated = await userService.updateUserProfile(req.user.id, req.body);
      res.json(updated);
    } catch (error) {
      console.error('Update profile error:', error);
      res.status(500).json({ error: error.message });
    }
  }
);

// Get user sessions
router.get('/sessions', async (req, res) => {
  try {
    const sessions = await userService.getUserSessions(req.user.id);
    res.json(sessions);
  } catch (error) {
    console.error('Get sessions error:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
