export interface UserProfile {
  dateOfBirth: string
  address: string
  idDocument: string
  occupation: string
  createdAt: string
}

export interface User {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string
  role: string
  active: boolean
  profile?: UserProfile
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}
