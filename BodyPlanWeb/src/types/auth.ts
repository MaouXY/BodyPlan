export interface LoginForm {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  phone?: string | null
  role: string
  status: number
  createdAt?: string | null
}

export interface LoginResponse {
  token: string
  expiresIn: number
  user: UserInfo
}
