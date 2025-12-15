const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const notesRoutes = require('./routes/notes');
const labelsRoutes = require('./routes/labels');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors({
  // origin: process.env.FRONTEND_URL || 'http://localhost:8080',
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

app.use(requestResponseLogger);

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/notes', notesRoutes);
app.use('/api/labels', labelsRoutes);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});

function requestResponseLogger(req, res, next) {
  // Request-side log with origin info
  const origin = req.headers.origin || 'no-origin';
  const userAgent = req.headers['user-agent'] || 'no-user-agent';
  const ip = req.ip || req.connection.remoteAddress || 'unknown-ip';

  console.log(`req: ${req.method} ${req.originalUrl}`);
  console.log(`  origin: ${origin}, ip: ${ip}`);
  console.log(`  user-agent: ${userAgent}`);

  // Hook into response finish event
  res.on('finish', () => {
    console.log(
      `res: ${req.method} ${req.originalUrl} -> ${res.statusCode}`
    );
  });

  next();
}

module.exports = app;
