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

// Update password
router.put('/password',
  [
    body('currentPassword').notEmpty().withMessage('Current password is required'),
    body('newPassword').isLength({ min: 6 }).withMessage('New password must be at least 6 characters'),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { currentPassword, newPassword } = req.body;
      const result = await userService.updatePassword(req.user.id, currentPassword, newPassword);
      res.json(result);
    } catch (error) {
      console.error('Update password error:', error);
      if (error.message === 'Current password is incorrect') {
        return res.status(400).json({ error: error.message });
      }
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

// Logout all sessions including current
router.post('/sessions/logout-all', async (req, res) => {
  try {
    const result = await userService.logoutAllSessions(req.user.id);
    res.json(result);
  } catch (error) {
    console.error('Logout all sessions error:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
