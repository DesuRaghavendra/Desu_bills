import React, { useState, useEffect } from 'react';
import { Snackbar, Alert } from '@mui/material';

export default function NotificationToast() {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [severity, setSeverity] = useState('error');

  useEffect(() => {
    const handleNotification = (e) => {
      if (e.detail) {
        setMessage(e.detail.message || 'An error occurred');
        setSeverity(e.detail.severity || 'error');
        setOpen(true);
      }
    };

    window.addEventListener('app_notification', handleNotification);
    return () => {
      window.removeEventListener('app_notification', handleNotification);
    };
  }, []);

  const handleClose = (event, reason) => {
    if (reason === 'clickaway') return;
    setOpen(false);
  };

  return (
    <Snackbar
      open={open}
      autoHideDuration={6000}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      <Alert onClose={handleClose} severity={severity} variant="filled" sx={{ width: '100%' }}>
        {message}
      </Alert>
    </Snackbar>
  );
}
