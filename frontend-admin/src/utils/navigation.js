export function safeAdminRedirect(value, fallback = '/dashboard') {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) return fallback
  try {
    const url = new URL(value, window.location.origin)
    const paths = ['/dashboard', '/users', '/pets', '/adoptions', '/health', '/vaccine', '/announcements']
    return url.origin === window.location.origin && paths.includes(url.pathname) ? url.pathname + url.search + url.hash : fallback
  } catch { return fallback }
}

export function safeSourceUrl(value) {
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch { return null }
}
