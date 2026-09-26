const TOKEN_KEY_ADMIN = 'token_admin';
const TOKEN_KEY_USER = 'token_user';

function keyFor(role) {
  return role === 'user' ? TOKEN_KEY_USER : TOKEN_KEY_ADMIN;
}

export function isLoggedIn(role) {
  if (role) return !!localStorage.getItem(keyFor(role));
  return !!localStorage.getItem(TOKEN_KEY_ADMIN) || !!localStorage.getItem(TOKEN_KEY_USER);
}

export function saveToken(token, role = 'admin') {
  localStorage.setItem(keyFor(role), token);
}

export function removeToken(role) {
  if (role) {
    localStorage.removeItem(keyFor(role));
  } else {
    localStorage.removeItem(TOKEN_KEY_ADMIN);
    localStorage.removeItem(TOKEN_KEY_USER);
  }
}

export function getToken(role) {
  if (role) return localStorage.getItem(keyFor(role));
  return localStorage.getItem(TOKEN_KEY_ADMIN) || localStorage.getItem(TOKEN_KEY_USER);
}

export function parseJWT(token) {
  try {
    if (!token || typeof token !== 'string' || token.split('.').length !== 3) {
      return {};
    }

    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');

    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join('')
    );

    const payload = JSON.parse(jsonPayload);

    if (payload.exp && payload.exp * 1000 < Date.now()) {
      return {};
    }

    return payload;
  } catch (error) {
    return {};
  }
}

export function saveRememberedUsername(username) {
  localStorage.setItem('rememberedUsername', username)
}

export function getRememberedUsername() {
  return localStorage.getItem('rememberedUsername')
}

export function removeRememberedUsername() {
  localStorage.removeItem('rememberedUsername')
}

export function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function maskCardKey(key) {
  if (!key || typeof key !== 'string') return key
  if (key.length <= 6) return key
  return key.substring(0, 3) + '***' + key.substring(key.length - 3)
}
