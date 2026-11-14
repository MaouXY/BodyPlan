const TOKEN_KEY = 'bodyplan-token'
const NAME_KEY = 'bodyplan-display-name'
const ROLE_KEY = 'bodyplan-role'

export const getRole = () => localStorage.getItem(ROLE_KEY) ?? ''
export const setRole = (role: string) => localStorage.setItem(ROLE_KEY, role)

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(NAME_KEY)
  localStorage.removeItem(ROLE_KEY)
}

export function getDisplayName() {
  return localStorage.getItem(NAME_KEY) ?? ''
}

export function setDisplayName(name: string) {
  localStorage.setItem(NAME_KEY, name)
}
