const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const db = require('../db');
const emailService = require('./emailService');

class UserService {
  // Register new user
  async registerUser(userData) {
    const { email, password, firstName, lastName, addressLine1, addressLine2, city, state, country, postalCode } = userData;
    
    // Check if user already exists
    const existingUser = await db.query('SELECT id FROM users WHERE email = $1', [email]);
    if (existingUser.rows.length > 0) {
      throw new Error('User with this email already exists');
    }
    
    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);
    
    // Generate verification code
    const verificationCode = crypto.randomBytes(32).toString('hex');
    const verificationExpires = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24 hours
    
    // Insert user
    const result = await db.query(
      `INSERT INTO users (email, password_hash, first_name, last_name, address_line1, 
       address_line2, city, state, country, postal_code, verification_code, verification_expires)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
       RETURNING id, email, first_name, last_name`,
      [email, passwordHash, firstName, lastName, addressLine1, addressLine2, 
       city, state, country, postalCode, verificationCode, verificationExpires]
    );
    
    const user = result.rows[0];
    
    // Send verification email
    await emailService.sendVerificationEmail(email, verificationCode, `${firstName} ${lastName}`);
    
    return {
      id: user.id,
      email: user.email,
      firstName: user.first_name,
      lastName: user.last_name,
      message: 'Registration successful. Please check your email to verify your account.'
    };
  }
  
  // Verify email
  async verifyEmail(verificationCode) {
    const result = await db.query(
      `UPDATE users 
       SET email_verified = TRUE, verification_code = NULL, verification_expires = NULL
       WHERE verification_code = $1 AND verification_expires > NOW()
       RETURNING id, email, first_name, last_name`,
      [verificationCode]
    );
    
    if (result.rows.length === 0) {
      throw new Error('Invalid or expired verification code');
    }
    
    return result.rows[0];
  }
  
  // Login user
  async loginUser(email, password, deviceInfo) {
    // Get user
    const userResult = await db.query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );
    
    if (userResult.rows.length === 0) {
      throw new Error('Invalid email or password');
    }
    
    const user = userResult.rows[0];
    
    // Check if email is verified
    if (!user.email_verified) {
      throw new Error('Please verify your email before logging in');
    }
    
    // Verify password
    const validPassword = await bcrypt.compare(password, user.password_hash);
    if (!validPassword) {
      throw new Error('Invalid email or password');
    }
    
    // Generate JWT token
    const token = jwt.sign(
      { userId: user.id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );
    
    // Create session
    const expiresAt = new Date(Date.now() + (parseInt(process.env.SESSION_EXPIRES_DAYS) || 30) * 24 * 60 * 60 * 1000);
    await db.query(
      `INSERT INTO user_sessions (user_id, session_token, device_info, ip_address, user_agent, expires_at)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [user.id, token, deviceInfo.deviceInfo, deviceInfo.ipAddress, deviceInfo.userAgent, expiresAt]
    );
    
    return {
      token,
      user: {
        id: user.id,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name,
        emailVerified: user.email_verified
      }
    };
  }
  
  // Logout user
  async logoutUser(token) {
    await db.query('DELETE FROM user_sessions WHERE session_token = $1', [token]);
  }
  
  // Validate session
  async validateSession(token) {
    const result = await db.query(
      `SELECT us.*, u.id as user_id, u.email, u.first_name, u.last_name
       FROM user_sessions us
       JOIN users u ON us.user_id = u.id
       WHERE us.session_token = $1 AND us.expires_at > NOW()`,
      [token]
    );
    
    if (result.rows.length === 0) {
      return null;
    }
    
    // Update last accessed
    await db.query(
      'UPDATE user_sessions SET last_accessed = NOW() WHERE session_token = $1',
      [token]
    );
    
    return result.rows[0];
  }
  
  // Get user profile
  async getUserProfile(userId) {
    const result = await db.query(
      `SELECT id, email, first_name, last_name, address_line1, address_line2,
       city, state, country, postal_code, email_verified, created_at
       FROM users WHERE id = $1`,
      [userId]
    );
    
    if (result.rows.length === 0) {
      throw new Error('User not found');
    }
    
    return result.rows[0];
  }
  
  // Update user profile
  async updateUserProfile(userId, updates) {
    const { firstName, lastName, addressLine1, addressLine2, city, state, country, postalCode } = updates;
    
    const result = await db.query(
      `UPDATE users 
       SET first_name = COALESCE($1, first_name),
           last_name = COALESCE($2, last_name),
           address_line1 = COALESCE($3, address_line1),
           address_line2 = COALESCE($4, address_line2),
           city = COALESCE($5, city),
           state = COALESCE($6, state),
           country = COALESCE($7, country),
           postal_code = COALESCE($8, postal_code)
       WHERE id = $9
       RETURNING id, email, first_name, last_name, address_line1, address_line2,
                 city, state, country, postal_code`,
      [firstName, lastName, addressLine1, addressLine2, city, state, country, postalCode, userId]
    );
    
    return result.rows[0];
  }
  
  // Get user sessions
  async getUserSessions(userId) {
    const result = await db.query(
      `SELECT id, device_info, ip_address, user_agent, created_at, last_accessed, expires_at
       FROM user_sessions
       WHERE user_id = $1 AND expires_at > NOW()
       ORDER BY last_accessed DESC`,
      [userId]
    );
    
    return result.rows;
  }
  
  // Update password
  async updatePassword(userId, currentPassword, newPassword) {
    // Get user's current password hash
    const userResult = await db.query(
      'SELECT password_hash, email, first_name, last_name FROM users WHERE id = $1',
      [userId]
    );
    
    if (userResult.rows.length === 0) {
      throw new Error('User not found');
    }
    
    const user = userResult.rows[0];
    
    // Verify current password
    const validPassword = await bcrypt.compare(currentPassword, user.password_hash);
    if (!validPassword) {
      throw new Error('Current password is incorrect');
    }
    
    // Hash new password
    const newPasswordHash = await bcrypt.hash(newPassword, 10);
    
    // Update password
    await db.query(
      'UPDATE users SET password_hash = $1 WHERE id = $2',
      [newPasswordHash, userId]
    );
    
    // Send notification email
    await emailService.sendPasswordChangeNotification(
      user.email,
      `${user.first_name} ${user.last_name}`
    );
    
    return { message: 'Password updated successfully' };
  }
  
  // Logout all sessions including current
  async logoutAllSessions(userId) {
    const result = await db.query(
      'DELETE FROM user_sessions WHERE user_id = $1',
      [userId]
    );
    
    return { 
      message: 'All sessions logged out successfully',
      sessionsRemoved: result.rowCount 
    };
  }
}

module.exports = new UserService();
