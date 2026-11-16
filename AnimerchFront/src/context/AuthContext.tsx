import { createContext } from "react";
import { type LoginRequest } from "../types/auth";

export interface AuthContextType {
    isAuthenticated: boolean;
    login: (data: LoginRequest) => Promise<void>;
    logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);