
import { useState, type ReactNode } from "react";
import { AuthContext } from "./AuthContext";
import { login as loginApi } from "../api/authApi";
import { type LoginRequest } from "../types/auth";

export function AuthProvider({ children }: { children: ReactNode }) {

    const [isAuthenticated, setIsAuthenticated] = useState(
        () => !!localStorage.getItem("accessToken")
    );

    const login = async (data: LoginRequest) => {
        const response = await loginApi(data);

        localStorage.setItem("accessToken", response.accessToken);
        localStorage.setItem("tokenType", response.tokenType);

        setIsAuthenticated(true);
    };

    const logout = () => {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("tokenType");

        setIsAuthenticated(false);
    };

    return (
        <AuthContext.Provider
            value={{
                isAuthenticated,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}
