import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import Login from './components/Login';
import Register from './components/Register';
import Dashboard from './components/Dashboard';
import ShortenerHome from './components/ShortenerHome';
import ShortCodeRedirect from './components/ShortCodeRedirect';

/**
 * ProtectedRoute Component
 * 
 * This component acts as a security guard for specific pages. 
 * If a user is not logged in, it redirects them to the /login page.
 * If they are logged in, it shows the requested content.
 */
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/" />;
};

/**
 * PublicRoute Component
 * 
 * Use this for pages like Login and Register. 
 * If a user is ALREADY logged in, we don't want them to see the login page again.
 * Instead, we redirect them to the home page (/).
 */
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/dashboard" /> : <>{children}</>;
};

/**
 * App Main Component
 * 
 * This is the central hub for routing. It maps URLs to specific components.
 */
function App() {
  return (
    <Router>
      <Routes>
        {/* The /login path: Only for visitors who are NOT logged in. */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <Login />
            </PublicRoute>
          }
        />
        
        {/* The /register path: Only for visitors who are NOT logged in. */}
        <Route
          path="/register"
          element={
            <PublicRoute>
              <Register />
            </PublicRoute>
          }
        />
        
        {/* Public home page for creating short links. */}
        <Route
          path="/"
          element={<ShortenerHome />}
        />

        {/* Authenticated dashboard for a user's own links and stats. */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />

        {/* Public short-code route for redirecting short links. */}
        <Route
          path="/:code"
          element={<ShortCodeRedirect />}
        />
      </Routes>
    </Router>
  );
}

export default App;
