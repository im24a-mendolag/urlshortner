import axios, { AxiosHeaders } from 'axios';
import { redirectToLogin } from '../authNavigation';

/**
 * Axios Instance Configuration
 * 
 * This file creates a reusable 'api' object that is configured to communicate 
 * with our Spring Boot backend. Instead of typing the full URL every time, 
 * we can just use this instance.
 */
const api = axios.create({
  // The base URL of our Spring Boot API
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',

  // This ensures that browser cookies (like session tokens) are sent
  // automatically with every request to the backend.
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');

  if (token) {
    if (config.headers instanceof AxiosHeaders) {
      config.headers.set('Authorization', `Bearer ${token}`);
    } else {
      config.headers = config.headers ?? {};
      (config.headers as Record<string, string>).Authorization = `Bearer ${token}`;
    }
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');

      const requestUrl = error.config?.url ?? '';
      const isAuthRequest = requestUrl.includes('/api/v1/auth/');
      const isLogoutRequest = requestUrl.includes('/api/v1/auth/logout');
      const isAlreadyPublicPage = window.location.pathname === '/'
        || window.location.pathname === '/login'
        || window.location.pathname === '/register';

      if (!isAuthRequest && !isLogoutRequest && !isAlreadyPublicPage) {
        redirectToLogin('Session expired. Please log in again.');
      }
    }

    return Promise.reject(error);
  },
);

export default api;
