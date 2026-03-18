import React, { useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { createShortLink } from '../api/links';

const ShortenerHome: React.FC = () => {
  const [originalUrl, setOriginalUrl] = useState('');
  const [createdShortUrl, setCreatedShortUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setCreatedShortUrl(null);

    const value = originalUrl.trim();
    if (!value) {
      setError('Please enter a URL first.');
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await createShortLink({ originalUrl: value });
      setCreatedShortUrl(created.shortUrl);
      setOriginalUrl('');
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message ?? 'Could not create short link.');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Could not create short link.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 sm:p-8">
      <div className="w-full max-w-2xl p-8 space-y-6 bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700">
        <div className="text-center space-y-2">
          <h1 className="text-4xl font-extrabold text-gray-900 dark:text-white tracking-tight">URL Shortener</h1>
          <p className="text-gray-600 dark:text-gray-400">
            Paste a long URL, get a short one instantly.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
            Long URL
          </label>
          <input
            type="url"
            value={originalUrl}
            onChange={(event) => setOriginalUrl(event.target.value)}
            required
            placeholder="https://example.com/very/long/link"
            className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold rounded-lg shadow-md transition"
          >
            {isSubmitting ? 'Shortening...' : 'Shorten URL'}
          </button>
        </form>

        {createdShortUrl && (
          <div className="p-4 rounded-lg bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800">
            <p className="text-sm font-medium text-green-700 dark:text-green-300">Your short URL:</p>
            <a
              href={createdShortUrl}
              target="_blank"
              rel="noreferrer"
              className="text-blue-700 dark:text-blue-300 break-all underline"
            >
              {createdShortUrl}
            </a>
          </div>
        )}

        {error && <p className="text-sm font-medium text-red-500">{error}</p>}

        <div className="flex justify-center gap-3 text-sm">
          <Link to="/login" className="text-blue-600 dark:text-blue-400 hover:underline">Login</Link>
          <span className="text-gray-400">|</span>
          <Link to="/register" className="text-blue-600 dark:text-blue-400 hover:underline">Register</Link>
        </div>
      </div>
    </div>
  );
};

export default ShortenerHome;
