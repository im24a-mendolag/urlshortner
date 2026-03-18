/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_SHORT_BASE_URL?: string;
  readonly VITE_SHORTEN_ENDPOINT?: string;
  readonly VITE_DASHBOARD_ENDPOINT?: string;
  readonly VITE_STATS_BASE_ENDPOINT?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
