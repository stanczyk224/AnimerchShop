
import { jwtDecode } from "jwt-decode";
import { type UserRole } from "../types/auth";

interface JwtPayload {
    sub: string;
    role?: UserRole;
    exp: number;
    iat: number;
}

export function getUserRole(token: string): UserRole | null {
    try {
        const decoded = jwtDecode<JwtPayload>(token);

        // Check token expiration
        if (!decoded.exp || decoded.exp * 1000 <= Date.now()) {
            return null;
        }

        // Check role
        if (
            decoded.role !== "CUSTOMER" &&
            decoded.role !== "ADMIN"
        ) {
            return null;
        }

        return decoded.role;

    } catch {
        return null;
    }
}
