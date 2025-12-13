const nodemailer = require('nodemailer');
require('dotenv').config();

class EmailService {
  constructor() {
    this.transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: process.env.SMTP_PORT,
      secure: false,
      auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASSWORD,
      },
    });
  }
  
  async sendVerificationEmail(email, verificationCode, userName) {
    const verificationUrl = `${process.env.APP_URL}/api/auth/verify-email?code=${verificationCode}`;
    
    const mailOptions = {
      from: process.env.EMAIL_FROM,
      to: email,
      subject: 'Verify Your Email - Notes App',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2>Welcome to Notes App, ${userName}!</h2>
          <p>Thank you for registering. Please verify your email address by clicking the link below:</p>
          <p style="margin: 30px 0;">
            <a href="${verificationUrl}" 
               style="background-color: #4CAF50; color: white; padding: 12px 24px; 
                      text-decoration: none; border-radius: 4px; display: inline-block;">
              Verify Email
            </a>
          </p>
          <p>Or copy and paste this link in your browser:</p>
          <p style="color: #666; word-break: break-all;">${verificationUrl}</p>
          <p style="color: #999; font-size: 12px; margin-top: 30px;">
            This link will expire in 24 hours. If you didn't create an account, please ignore this email.
          </p>
        </div>
      `,
    };
    
    try {
      await this.transporter.sendMail(mailOptions);
      console.log('Verification email sent to:', email);
    } catch (error) {
      console.error('Error sending verification email:', error);
      throw new Error('Failed to send verification email');
    }
  }
  
  async sendPasswordChangeNotification(email, userName) {
    const mailOptions = {
      from: process.env.EMAIL_FROM,
      to: email,
      subject: 'Password Changed - Notes App',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2>Password Changed Successfully</h2>
          <p>Hello ${userName},</p>
          <p>Your password has been changed successfully.</p>
          <p style="margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #4CAF50;">
            <strong>Date:</strong> ${new Date().toLocaleString()}<br>
          </p>
          <p>If you didn't make this change, please contact our support team immediately.</p>
          <p style="color: #999; font-size: 12px; margin-top: 30px;">
            This is an automated notification. Please do not reply to this email.
          </p>
        </div>
      `,
    };
    
    try {
      await this.transporter.sendMail(mailOptions);
      console.log('Password change notification sent to:', email);
    } catch (error) {
      console.error('Error sending password change notification:', error);
      // Don't throw error - password change already succeeded
    }
  }
}

module.exports = new EmailService();
