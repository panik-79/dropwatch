/**
 * Production environment configuration.
 * Used when building with `ng build` (production configuration).
 * The apiBaseUrl points to the Render-deployed backend.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://dropwatch-f39b.onrender.com/api/v1',
  sseStreamUrl: 'https://dropwatch-f39b.onrender.com/api/v1/stream/live',
};
