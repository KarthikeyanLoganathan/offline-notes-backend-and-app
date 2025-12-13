const express = require('express');
const { body, validationResult } = require('express-validator');
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

// Register new user
router.post('/register',
  [
    body('email').isEmail().normalizeEmail({
      gmail_remove_subaddress: false,
      gmail_remove_dots: false,
      outlookdotcom_remove_subaddress: false,
    }),
    body('password').isLength({ min: 8 }),
    body('firstName').trim().notEmpty(),
    body('lastName').trim().notEmpty(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const result = await userService.registerUser(req.body);
      res.status(201).json(result);
    } catch (error) {
      console.error('Registration error:', error);
      res.status(400).json({ error: error.message });
    }
  }
);

// Verify email
router.get('/verify-email', async (req, res) => {
  try {
    const { code } = req.query;
    
    if (!code) {
      return res.status(400).json({ error: 'Verification code is required' });
    }
    
    const user = await userService.verifyEmail(code);
    res.json({ 
      message: 'Email verified successfully',
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name
      }
    });
  } catch (error) {
    console.error('Email verification error:', error);
    res.status(400).json({ error: error.message });
  }
});

// Login
router.post('/login',
  [
    body('email').isEmail().normalizeEmail({
      gmail_remove_subaddress: false,
      gmail_remove_dots: false,
      outlookdotcom_remove_subaddress: false,
    }),
    body('password').notEmpty(),
  ],
  validateRequest,
  async (req, res) => {
    try {
      const { email, password } = req.body;
      
      const deviceInfo = {
        deviceInfo: req.body.deviceInfo || 'Unknown',
        ipAddress: req.ip,
        userAgent: req.headers['user-agent']
      };
      
      const result = await userService.loginUser(email, password, deviceInfo);
      
      // Set cookie
      res.cookie('session_token', result.token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        maxAge: 30 * 24 * 60 * 60 * 1000, // 30 days
        sameSite: 'lax'
      });
      
      res.json(result);
    } catch (error) {
      console.error('Login error:', error);
      res.status(401).json({ error: error.message });
    }
  }
);

// Logout
router.post('/logout', async (req, res) => {
  try {
    const token = req.headers.authorization?.split(' ')[1] || req.cookies.session_token;
    
    if (token) {
      await userService.logoutUser(token);
    }
    
    res.clearCookie('session_token');
    res.json({ message: 'Logged out successfully' });
  } catch (error) {
    console.error('Logout error:', error);
    res.status(500).json({ error: 'Logout failed' });
  }
});

module.exports = router;
