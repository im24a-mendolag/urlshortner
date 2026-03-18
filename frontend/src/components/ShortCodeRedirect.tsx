import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { buildShortUrlFromCode } from '../api/links';

const RESERVED_CODES = new Set(['login', 'register', 'dashboard']);

const ShortCodeRedirect: React.FC = () => {
  const { code } = useParams<{ code: string }>();

  useEffect(() => {
    if (!code || RESERVED_CODES.has(code)) {
      return;
    }

    window.location.replace(buildShortUrlFromCode(code));
  }, [code]);

  if (!code || RESERVED_CODES.has(code)) {
    return (
      <div className="flex items-center justify-center min-h-screen p-4">
        <p className="text-gray-600 dark:text-gray-300">Unknown short code.</p>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen p-4">
      <p className="text-gray-600 dark:text-gray-300">Redirecting to your destination...</p>
    </div>
  );
};

export default ShortCodeRedirect;
