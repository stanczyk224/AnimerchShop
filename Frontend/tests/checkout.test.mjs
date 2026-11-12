import test from 'node:test';
import assert from 'node:assert/strict';
import { phoneError, nameError, passwordError, validateAccount, validateDelivery } from '../src/validation.ts';
import { prepareCart } from '../src/checkoutService.ts';

test('phones reject text, invalid prefixes and digit counts, and accept international formatting', () => {
    for (const value of ['hello', '+998abc1234567', '12345', '1234567890123456', '0000000', '++998901234567']) assert.ok(phoneError(value), value);
    for (const value of ['+998901234567', '+998 90 123 45 67', '1 (234) 567-8900']) assert.equal(phoneError(value), '', value);
});
test('names support international letters while rejecting numeric junk and markup', () => {
    for (const name of ['José O’Neill', '李 明', 'Ташкент']) assert.equal(nameError(name), '');
    for (const name of ['123!', '<script>', '']) assert.ok(nameError(name));
});
test('account validation checks email, password, confirmation and signup phone', () => {
    const form = new FormData();
    for (const [key, value] of Object.entries({ name: 'User', phone: 'nonsense', email: 'invalid', password: 'short', confirmPassword: 'different' })) form.set(key, value);
    const errors = validateAccount(form, true);
    for (const key of ['phone', 'email', 'password', 'confirmPassword']) assert.ok(errors[key]);
    assert.equal(passwordError('é'.repeat(36), true), '');
    assert.ok(passwordError('é'.repeat(37), true));
});
test('delivery requires valid field formats and rejects control characters', () => {
    const address = { recipientName: 'Test User', phone: '+998901234567', addressLine1: '12 Main Street', addressLine2: '', city: 'Tashkent', country: 'Uzbekistan', postalCode: '100000' };
    assert.deepEqual(validateDelivery(address), {});
    const errors = validateDelivery({ ...address, phone: 'gibberish', city: '123!', addressLine1: 'bad\naddress', postalCode: '<bad>' });
    for (const key of ['phone', 'city', 'addressLine1', 'postalCode']) assert.ok(errors[key]);
});
test('cart preparation retries a lost add response without incrementing twice', async () => {
    const lines = [{ product: { id: 1, name: 'Hoodie', price: 100, quantity: 10 }, quantity: 2 }];
    let cart = [{ productId: 99, quantity: 1 }];
    let loseResponse = true;
    const calls = [];
    const call = async (path, options) => {
        calls.push(options.method || 'GET');
        if (path === '/cart/items') return structuredClone(cart);
        if (options.method === 'DELETE') { cart = []; return; }
        cart = [{ productId: 1, quantity: Number(new URL('http://test' + path).searchParams.get('quantity')) }];
        if (loseResponse) { loseResponse = false; throw new Error('response lost'); }
    };
    await assert.rejects(prepareCart(lines, 'token', call));
    await prepareCart(lines, 'token', call);
    assert.deepEqual(cart, [{ productId: 1, quantity: 2 }]);
    assert.equal(calls.filter(method => method === 'POST').length, 1);
});
test('checkout stops if another tab changes the cart during preparation', async () => {
    let reads = 0;
    const call = async (path) => path === '/cart/items' ? (++reads === 1 ? [{ productId: 1, quantity: 1 }] : []) : undefined;
    await assert.rejects(prepareCart([{ product: { id: 1 }, quantity: 1 }], 'token', call), /changed/);
});
