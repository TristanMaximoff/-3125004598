import { defineStore } from 'pinia'

export default defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  actions: {
    setLogin(token, user) {
      this.token = token
      this.userInfo = user
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.userInfo = {}
      localStorage.clear()
    }
  }
})