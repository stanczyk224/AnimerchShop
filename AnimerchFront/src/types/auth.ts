
export type UserRole = "CUSTOMER" | "ADMIN";

export interface RegisterRequest {
    name: string;
    phone: string;
    email: string;
    password: string;
}

export interface RegisterResponse {
    userId: number;
    email: string;
    role: UserRole;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface LoginResponse {
    accessToken: string;
    tokenType: string;
    role: UserRole;
}
