import { getToken, removeToken, removeUserInfo, setToken, setUserInfo } from './storage'

export function hasToken() {
  return Boolean(getToken())
}

export function saveAuth(token, userInfo) {
  setToken(token)
  setUserInfo(userInfo)
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
}
