import { STORAGE_TOKEN, STORAGE_USERNAME } from './config'

export function getToken() {
	return uni.getStorageSync(STORAGE_TOKEN) || ''
}

export function setToken(token, username) {
	uni.setStorageSync(STORAGE_TOKEN, token)
	if (username) {
		uni.setStorageSync(STORAGE_USERNAME, username)
	}
}

export function removeToken() {
	uni.removeStorageSync(STORAGE_TOKEN)
	uni.removeStorageSync(STORAGE_USERNAME)
}

export function getUsername() {
	return uni.getStorageSync(STORAGE_USERNAME) || ''
}

export function isLoggedIn() {
	return !!getToken()
}
