import { api } from './api.ts';
export type BasketLine = { product: { id: number; name: string; price: number; quantity: number }; quantity: number };
type CartItem = { productId: number; quantity: number };
type Api = typeof api;
// Set absolute quantities after reading the server cart. A retry never blindly increments.
export async function prepareCart(lines: BasketLine[], token: string, call: Api = api): Promise<void> {
    const desired = new Map(lines.map(line => [line.product.id, line.quantity]));
    const existing = await call<CartItem[]>('/cart/items', { token });
    for (const item of existing) {
        if (!desired.has(item.productId)) await call('/cart/items/' + item.productId, { token, method: 'DELETE' });
    }
    for (const line of lines) {
        const item = existing.find(entry => entry.productId === line.product.id);
        if (item?.quantity === line.quantity) continue;
        await call('/cart/items/' + line.product.id + '?quantity=' + line.quantity, { token, method: item ? 'PUT' : 'POST' });
    }
    const final = await call<CartItem[]>('/cart/items', { token });
    if (final.length !== desired.size || final.some(item => desired.get(item.productId) !== item.quantity))
        throw new Error('Your account cart changed. Review your basket and try again.');
}

