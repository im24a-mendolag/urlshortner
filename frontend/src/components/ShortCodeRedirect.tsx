import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from 'axios';
import api from '../api/axios';

const RESERVED_CODES = new Set(['login', 'register', 'dashboard']);
type RedirectState = 'loading' | 'not_found' | 'error';

const ShortCodeRedirect: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const [state, setState] = useState<RedirectState>('loading');
  const [message, setMessage] = useState<string>('Redirecting to your destination...');

  useEffect(() => {
    if (!code || RESERVED_CODES.has(code)) {
      setState('not_found');
      setMessage('That short link does not exist.');
      return;
    }

    let isMounted = true;

    const resolveLink = async () => {
      try {
        const response = await api.get(`/resolve/${encodeURIComponent(code)}`);
        const originalUrl = response.data?.originalUrl;

        if (typeof originalUrl === 'string' && originalUrl.trim()) {
          window.location.replace(originalUrl);
          return;
        }

        if (isMounted) {
          setState('error');
          setMessage('Could not resolve this short link.');
        }
      } catch (err: unknown) {
        if (!isMounted) {
          return;
        }

        if (axios.isAxiosError(err) && err.response?.status === 404) {
          setState('not_found');
          setMessage('Link not found.');
          return;
        }

        setState('error');
        setMessage('Something went wrong while resolving this link.');
      }
    };

    void resolveLink();

    return () => {
      isMounted = false;
    };
  }, [code]);

  if (state !== 'loading') {
    return (
      <div className="flex items-center justify-center min-h-screen p-4">
        <div className="w-full max-w-md rounded-2xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-8 text-center shadow-lg">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Link unavailable</h1>
          <p className="mt-3 text-gray-600 dark:text-gray-300">{message}</p>
          <Link
            to="/"
            className="inline-block mt-6 px-5 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold transition"
          >
            Go to URL Shortener
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen p-4">
      <p className="text-gray-600 dark:text-gray-300">{message}</p>
    </div>
  );
};

export default ShortCodeRedirect;
