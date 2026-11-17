
import { api } from "./api";

import {
    type RegisterRequest,
    type RegisterResponse,
    type LoginRequest,
    type LoginResponse,
} from "../types/auth";

export const register = async (
    data: RegisterRequest
): Promise<RegisterResponse> => {

    const response = await api.post<RegisterResponse>(
        "/auth/register",
        data
    );

    return response.data;
};

export const login = async (
    data: LoginRequest
): Promise<LoginResponse> => {

    const response = await api.post<LoginResponse>(
        "/auth/login",
        data
    );

    return response.data;
};
