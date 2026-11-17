
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
    getCartItems,
    addProductToCart,
    updateCartItemQuantity,
    removeCartItem,
} from "../api/cartApi";

interface CartItem {
    productId: number;
    quantity: number;
}

export function useCart() {
    const queryClient = useQueryClient();

    const cartQuery = useQuery<CartItem[]>({
        queryKey: ["cart"],
        queryFn: async () => {
            const response = await getCartItems();
            return response.data;
        },
    });

    const addMutation = useMutation({
        mutationFn: (productId: number) =>
            addProductToCart(productId, 1),
        onSuccess: () =>
            queryClient.invalidateQueries({ queryKey: ["cart"] }),
    });

    const updateMutation = useMutation({
        mutationFn: ({ productId, quantity }: {
            productId: number;
            quantity: number;
        }) => updateCartItemQuantity(productId, quantity),
        onSuccess: () =>
            queryClient.invalidateQueries({ queryKey: ["cart"] }),
    });

    const removeMutation = useMutation({
        mutationFn: (productId: number) =>
            removeCartItem(productId),
        onSuccess: () =>
            queryClient.invalidateQueries({ queryKey: ["cart"] }),
    });

    return {
        cartQuery,
        addMutation,
        updateMutation,
        removeMutation,
    };
}
