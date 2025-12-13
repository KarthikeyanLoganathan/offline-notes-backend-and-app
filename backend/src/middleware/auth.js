const jwt = require('jsonwebtoken');
const userService = require('../services/userService');

async function authenticate(req, res, next) {
  try {
    // Get token from header or cookie
    let token = req.headers.authorization?.split(' ')[1];
    
    if (!token && req.cookies.session_token) {
      token = req.cookies.session_token;
    }
    
    if (!token) {
      return res.status(401).json({ error: 'Authentication required' });
    }
    
    // Verify JWT
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    
    // Validate session in database
    const session = await userService.validateSession(token);
    
    if (!session) {
      return res.status(401).json({ error: 'Invalid or expired session' });
    }
    
    // Attach user info to request
    req.user = {
      id: decoded.userId,
      email: decoded.email,
      sessionToken: token
    };
    
    next();
  } catch (error) {
    if (error.name === 'JsonWebTokenError') {
      return res.status(401).json({ error: 'Invalid token' });
    }
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Token expired' });
    }
    console.error('Authentication error:', error);
    res.status(500).json({ error: 'Authentication failed' });
  }
}

module.exports = authenticate;
