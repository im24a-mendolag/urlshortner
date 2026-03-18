import React, { createContext, useContext, useState, type ReactNode } from 'react';

/**
 * User Interface
 * 
 * This defines the shape of the User object we expect from the backend.
 * It helps TypeScript provide better autocompletion and error checking.
 */
export interface User {
  id: string;
  email: string;
  roles: string[];
}

/**
 * AuthContextType
 * 
 * This interface defines the data and functions that our AuthContext 
 * will make available to any component in the app.
 */
interface AuthContextType {
  user: User | null; // The currently logged-in user, or null if logged out.
  token: string | null; // JWT token used by API and WebSocket calls.
  login: (userData: User, authToken?: string | null) => void; // A function to set the user state.
  logout: () => void; // A function to clear the user state.
  isAuthenticated: boolean; // A helper to check if someone is logged in.
}

// Create the context where the state will live.
const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * AuthProvider Component
 * 
 * This component "provides" the authentication state to the entire app.
 * It's wrapped around the <App /> component in main.tsx.
 */
export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // Initialize user state. We check localStorage first so the user 
  // stays logged in even if they refresh the page.
  const [user, setUser] = useState<User | null>(() => {
    const savedToken = localStorage.getItem('authToken');
    if (!savedToken) {
      localStorage.removeItem('user');
      return null;
    }

    const savedUser = localStorage.getItem('user');
    if (!savedUser) {
      return null;
    }

    try {
      const parsed = JSON.parse(savedUser) as Partial<User>;
      if (typeof parsed.id !== 'string' || typeof parsed.email !== 'string' || !Array.isArray(parsed.roles)) {
        localStorage.removeItem('user');
        return null;
      }

      return {
        id: parsed.id,
        email: parsed.email,
        roles: parsed.roles.filter((role): role is string => typeof role === 'string'),
      };
    } catch {
      localStorage.removeItem('user');
      return null;
    }
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('authToken'));

  /**
   * Sets the logged-in user and saves it to localStorage.
   * @param userData The user data received from the backend.
   */
  const login = (userData: User, authToken?: string | null) => {
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));

    if (authToken && authToken.trim()) {
      setToken(authToken);
      localStorage.setItem('authToken', authToken);
    } else {
      setToken(null);
      localStorage.removeItem('authToken');
    }
  };

  /**
   * Clears the user state and removes it from localStorage.
   */
  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('user');
    localStorage.removeItem('authToken');
  };

  // Simple boolean to track if the user is currently authenticated.
  const isAuthenticated = !!user && !!token;

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
};

/**
 * useAuth Hook
 * 
 * This is a custom hook that any component can use to access the 
 * authentication data. For example: const { user, logout } = useAuth();
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
