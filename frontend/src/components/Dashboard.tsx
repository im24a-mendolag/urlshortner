import React, { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { useAuth } from '../AuthContext';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import {
  createShortLink,
  getDashboardLinks,
  getLinkStats,
  setLinkDisabled,
  type LinkItem,
  type LinkStats,
} from '../api/links';

/**
 * Dashboard for logged-in URL shortener users.
 */
const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [originalUrl, setOriginalUrl] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingLinks, setIsLoadingLinks] = useState(false);
  const [links, setLinks] = useState<LinkItem[]>([]);
  const [createdLink, setCreatedLink] = useState<LinkItem | null>(null);
  const [selectedStats, setSelectedStats] = useState<LinkStats | null>(null);
  const [statsCode, setStatsCode] = useState<string | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [toggleInProgressCode, setToggleInProgressCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [infoMessage, setInfoMessage] = useState<string | null>(null);

  useEffect(() => {
    void loadLinks();
  }, []);

  const sortedLinks = useMemo(
    () => [...links].sort((a, b) => b.clickCount - a.clickCount),
    [links],
  );

  const loadLinks = async () => {
    setIsLoadingLinks(true);
    try {
      const response = await getDashboardLinks();
      setLinks(response);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message ?? 'Could not load your dashboard links.');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Could not load your dashboard links.');
      }
    } finally {
      setIsLoadingLinks(false);
    }
  };

  const handleLogout = async () => {
    try {
      await api.post('/api/v1/auth/logout');
      navigate('/', { replace: true });
      logout();
    } catch (err) {
      navigate('/', { replace: true });
      logout();
    }
  };

  const handleCreateLink = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setInfoMessage(null);

    const value = originalUrl.trim();
    if (!value) {
      setError('Please provide a URL to shorten.');
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await createShortLink({ originalUrl: value });
      setCreatedLink(created);
      setOriginalUrl('');
      setLinks((current) => [created, ...current.filter((item) => item.shortCode !== created.shortCode)]);
      setInfoMessage('Short link created successfully.');
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || 'Could not create short link.');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Could not create short link.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopyShortUrl = async (shortUrl: string) => {
    try {
      await navigator.clipboard.writeText(shortUrl);
      setInfoMessage('Short URL copied to clipboard.');
    } catch (err: unknown) {
      setError('Clipboard copy failed. Please copy the link manually.');
    }
  };

  const handleLoadStats = async (code: string) => {
    setStatsLoading(true);
    setStatsCode(code);
    setSelectedStats(null);
    setError(null);
    try {
      const stats = await getLinkStats(code);
      setSelectedStats(stats);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || 'Could not load link stats.');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Could not load link stats.');
      }
    } finally {
      setStatsLoading(false);
    }
  };

  const handleToggleDisabled = async (item: LinkItem) => {
    setToggleInProgressCode(item.shortCode);
    setError(null);
    setInfoMessage(null);
    try {
      const updated = await setLinkDisabled(item.shortCode, !item.disabled);
      setLinks((current) => current.map((entry) => (
        entry.shortCode === updated.shortCode ? updated : entry
      )));
      setInfoMessage(updated.disabled ? 'Link disabled.' : 'Link enabled.');
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || 'Could not update link status.');
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Could not update link status.');
      }
    } finally {
      setToggleInProgressCode(null);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 sm:p-8">
      <div className="w-full max-w-5xl p-8 space-y-8 bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700">
        <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="text-center sm:text-left">
            <h1 className="text-4xl font-extrabold text-gray-900 dark:text-white tracking-tight">My URL Dashboard</h1>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              Welcome back, <span className="font-semibold text-gray-900 dark:text-white">{user?.email}</span>
            </p>
          </div>
          <button
            onClick={handleLogout}
            className="px-6 py-2.5 bg-red-500 hover:bg-red-600 text-white font-medium rounded-lg shadow-sm transition duration-200"
          >
            Logout
          </button>
        </div>

        <div className="p-6 bg-gray-50 dark:bg-gray-900/50 rounded-xl border border-gray-100 dark:border-gray-800">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">Create New Short Link</h2>
          <form onSubmit={handleCreateLink} className="mt-4 space-y-3">
            <input
              type="url"
              value={originalUrl}
              onChange={(event) => setOriginalUrl(event.target.value)}
              required
              placeholder="https://example.com/very/long/link"
              className="w-full px-4 py-3 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full sm:w-auto px-6 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold rounded-lg shadow-sm transition"
            >
              {isSubmitting ? 'Creating...' : 'Create short URL'}
            </button>
          </form>
        </div>

        {createdLink && (
          <div className="p-6 bg-green-50 dark:bg-green-900/20 rounded-xl border border-green-200 dark:border-green-800">
            <h3 className="text-lg font-bold text-green-800 dark:text-green-200">New short URL</h3>
            <a
              href={createdLink.shortUrl}
              target="_blank"
              rel="noreferrer"
              className="block mt-2 text-blue-700 dark:text-blue-300 underline break-all"
            >
              {createdLink.shortUrl}
            </a>
            <p className="mt-2 text-sm text-gray-700 dark:text-gray-300 break-all">{createdLink.originalUrl}</p>
            <div className="mt-3 flex gap-3">
              <button
                onClick={() => void handleCopyShortUrl(createdLink.shortUrl)}
                className="px-4 py-2 text-sm font-medium bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
              >
                Copy
              </button>
              <a
                href={createdLink.shortUrl}
                target="_blank"
                rel="noreferrer"
                className="px-4 py-2 text-sm font-medium bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
              >
                Open
              </a>
            </div>
          </div>
        )}

        <div className="space-y-4">
          <h3 className="text-xl font-bold text-gray-900 dark:text-white">Your Links</h3>
          <button
            onClick={() => void loadLinks()}
            className="px-4 py-2 text-sm font-medium bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
          >
            Refresh
          </button>

          {isLoadingLinks ? (
            <p className="text-gray-500 dark:text-gray-400">Loading your links...</p>
          ) : sortedLinks.length === 0 ? (
            <p className="text-gray-500 dark:text-gray-400">No links yet. Create your first short URL above.</p>
          ) : (
            <div className="space-y-3">
              {sortedLinks.map((item) => (
                <div key={item.shortCode} className="p-4 rounded-lg bg-gray-50 dark:bg-gray-900/50 border border-gray-100 dark:border-gray-800">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                    <a
                      href={item.shortUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="font-semibold text-blue-700 dark:text-blue-300 underline break-all"
                    >
                      {item.shortUrl}
                    </a>
                    <div className="flex items-center gap-3 text-sm">
                      <span className="text-gray-500 dark:text-gray-400">Clicks: {item.clickCount}</span>
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                          item.disabled
                            ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                            : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                        }`}
                      >
                        {item.disabled ? 'Disabled' : 'Active'}
                      </span>
                    </div>
                  </div>
                  <p className="mt-1 text-sm text-gray-700 dark:text-gray-300 break-all">{item.originalUrl}</p>
                  <div className="mt-3 flex gap-3">
                    <button
                      onClick={() => void handleCopyShortUrl(item.shortUrl)}
                      className="px-3 py-1.5 text-sm font-medium bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
                    >
                      Copy
                    </button>
                    <button
                      onClick={() => void handleLoadStats(item.shortCode)}
                      className="px-3 py-1.5 text-sm font-medium bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600"
                    >
                      View stats
                    </button>
                    <button
                      onClick={() => void handleToggleDisabled(item)}
                      disabled={toggleInProgressCode === item.shortCode}
                      className="px-3 py-1.5 text-sm font-medium bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-60"
                    >
                      {toggleInProgressCode === item.shortCode
                        ? 'Saving...'
                        : item.disabled
                          ? 'Enable'
                          : 'Disable'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-6 bg-gray-50 dark:bg-gray-900 rounded-xl border-2 border-dashed border-gray-200 dark:border-gray-800 min-h-[120px]">
          <h4 className="text-xs font-bold text-gray-400 uppercase mb-3">Stats / Status</h4>
          {statsLoading && <p className="text-gray-500 dark:text-gray-400">Loading stats for {statsCode}...</p>}
          {selectedStats && (
            <div className="text-sm text-gray-700 dark:text-gray-300 space-y-1">
              <p><span className="font-semibold">Code:</span> {selectedStats.shortCode}</p>
              <p><span className="font-semibold">Clicks:</span> {selectedStats.clickCount}</p>
              <p><span className="font-semibold">First click:</span> {selectedStats.firstClickAt ?? '-'}</p>
              <p><span className="font-semibold">Last click:</span> {selectedStats.lastClickAt ?? '-'}</p>
            </div>
          )}
          {infoMessage && <p className="text-sm font-medium text-green-600 dark:text-green-300">{infoMessage}</p>}
          {error && <p className="text-sm font-medium text-red-500">{error}</p>}
          {!statsLoading && !selectedStats && !infoMessage && !error && (
            <p className="text-sm italic text-gray-500 dark:text-gray-400">
              Select a link to view stats, or create a new short link.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
