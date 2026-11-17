
import { api } from "./api";

export const getCart = () =>
    api.get("/cart");

export const getCartItems = () =>
    api.get("/cart/items");

export const addProductToCart = (productId: number, quantity = 1) =>
    api.post(`/cart/items/${productId}`, null, {
        params: { quantity },
    });

export const updateCartItemQuantity = (
    productId: number,
    quantity: number
) =>
    api.put(`/cart/items/${productId}`, null, {
        params: { quantity },
    });

export const removeCartItem = (productId: number) =>
    api.delete(`/cart/items/${productId}`);

export const clearCart = () =>
    api.delete("/cart");
