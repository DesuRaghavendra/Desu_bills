import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    let message = 'An unexpected error occurred';

    if (error.response) {
      const data = error.response.data;
      if (data && data.message) {
        message = data.message;
      } else if (data && data.errorCode) {
        message = `Error [${data.errorCode}]: ${data.message || 'Request failed'}`;
      } else if (typeof data === 'string' && data.trim()) {
        message = data;
      } else {
        message = `HTTP Error ${error.response.status}: ${error.response.statusText}`;
      }
    } else if (error.request) {
      message = 'Network Error: Service is currently unreachable or timed out. Please check your backend connection.';
    } else if (error.message) {
      message = error.message;
    }

    if (typeof window !== 'undefined') {
      window.dispatchEvent(
        new CustomEvent('app_notification', {
          detail: { message, severity: 'error' }
        })
      );
    }

    return Promise.reject(error);
  }
);

export default api;
