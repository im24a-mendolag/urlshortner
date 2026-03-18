import api from './axios';

type UnknownRecord = Record<string, unknown>;

export interface CreateLinkPayload {
  originalUrl: string;
}

export interface LinkItem {
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  clickCount: number;
}

export interface LinkStats {
  shortCode: string;
  clickCount: number;
  firstClickAt: string | null;
  lastClickAt: string | null;
}

const isRecord = (value: unknown): value is UnknownRecord => {
  return typeof value === 'object' && value !== null;
};

const toNumber = (value: unknown, fallback = 0) => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === 'string') {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }

  return fallback;
};

const getText = (record: UnknownRecord, keys: string[]) => {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === 'string' && value.trim()) {
      return value;
    }
  }
  return '';
};

const removeTrailingSlash = (value: string) => value.replace(/\/+$/, '');

const getApiOrigin = () => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  if (apiBaseUrl && /^https?:\/\//i.test(apiBaseUrl)) {
    return removeTrailingSlash(apiBaseUrl);
  }

  if (import.meta.env.DEV) {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }

  return window.location.origin;
};

export const buildShortUrlFromCode = (shortCode: string) => {
  const configuredBase = import.meta.env.VITE_SHORT_BASE_URL;
  const base = configuredBase ? removeTrailingSlash(configuredBase) : getApiOrigin();
  return `${base}/${shortCode}`;
};

const SHORTEN_ENDPOINT = import.meta.env.VITE_SHORTEN_ENDPOINT ?? '/shorten';
const DASHBOARD_ENDPOINT = import.meta.env.VITE_DASHBOARD_ENDPOINT ?? '/dashboard';
const STATS_BASE_ENDPOINT = import.meta.env.VITE_STATS_BASE_ENDPOINT ?? '/stats';

const normalizeLinkItem = (value: unknown): LinkItem | null => {
  if (!isRecord(value)) {
    return null;
  }

  const shortCode = getText(value, ['shortCode', 'short_code', 'code']);
  const originalUrl = getText(value, ['originalUrl', 'original_url', 'url']);

  if (!shortCode || !originalUrl) {
    return null;
  }

  const providedShortUrl = getText(value, ['shortUrl', 'short_url']);
  const clickCount = toNumber(value.clickCount ?? value.clicks ?? value.totalClicks, 0);

  return {
    shortCode,
    shortUrl: providedShortUrl || buildShortUrlFromCode(shortCode),
    originalUrl,
    clickCount,
  };
};

const normalizeStats = (value: unknown): LinkStats | null => {
  if (!isRecord(value)) {
    return null;
  }

  const shortCode = getText(value, ['shortCode', 'short_code', 'code']);
  if (!shortCode) {
    return null;
  }

  const first = value.firstClickAt ?? value.first_click_at ?? value.firstClick;
  const last = value.lastClickAt ?? value.last_click_at ?? value.lastClick;

  return {
    shortCode,
    clickCount: toNumber(value.clickCount ?? value.clicks ?? value.totalClicks, 0),
    firstClickAt: typeof first === 'string' ? first : null,
    lastClickAt: typeof last === 'string' ? last : null,
  };
};

export const createShortLink = async (payload: CreateLinkPayload): Promise<LinkItem> => {
  const response = await api.post(SHORTEN_ENDPOINT, payload);
  const normalized = normalizeLinkItem(response.data);

  if (!normalized) {
    throw new Error('Backend response did not include a valid short link.');
  }

  return normalized;
};

export const getDashboardLinks = async (): Promise<LinkItem[]> => {
  const response = await api.get(DASHBOARD_ENDPOINT);
  const source = isRecord(response.data) && Array.isArray(response.data.links)
    ? response.data.links
    : response.data;

  if (!Array.isArray(source)) {
    throw new Error('Backend response did not include a links list.');
  }

  return source
    .map(normalizeLinkItem)
    .filter((entry): entry is LinkItem => entry !== null);
};

export const getLinkStats = async (code: string): Promise<LinkStats> => {
  const response = await api.get(`${STATS_BASE_ENDPOINT}/${encodeURIComponent(code)}`);
  const normalized = normalizeStats(response.data);

  if (!normalized) {
    throw new Error('Backend response did not include valid link stats.');
  }

  return normalized;
};
