export type Delivery = {
    recipientName: string; phone: string; addressLine1: string; addressLine2: string;
    city: string; postalCode: string; country: string;
};
export type FieldErrors = Record<string, string>;
const namePattern = /^[\p{L}\p{M}][\p{L}\p{M} .’'-]*$/u;
export function nameError(value: string, max = 100): string {
    if (!value.trim()) return 'This field is required.';
    if (value.length > max) return `Use no more than ${max} characters.`;
    return namePattern.test(value.trim()) ? '' : 'Use letters, spaces, apostrophes, periods or hyphens.';
}
export function phoneError(value: string): string {
    const phone = value.trim();
    if (!phone) return 'Enter your phone number.';
    if (phone.length > 30 || !/^\+?[1-9][0-9 ()-]*$/.test(phone)) return 'Use digits with an optional +, spaces, parentheses or hyphens. No letters.';
    const digits = phone.replace(/\D/g, '');
    return digits.length >= 7 && digits.length <= 15 ? '' : 'Enter a phone number with 7–15 digits.';
}
export function normalizedPhone(value: string): string { return value.replace(/[ ()-]/g, ''); }
export function emailError(value: string): string {
    return value.length <= 254 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ? '' : 'Enter a valid email, such as you@example.com.';
}
export function passwordError(value: string, register: boolean): string {
    if (!value.trim()) return 'Enter your password.';
    if (register && value.length < 8) return 'Use at least 8 characters.';
    if (new TextEncoder().encode(value).length > 72) return 'Use a password of no more than 72 UTF-8 bytes (usually 72 characters).';
    return '';
}
export function validateAccount(form: FormData, register: boolean): FieldErrors {
    const errors: FieldErrors = {};
    errors.email = emailError(String(form.get('email') || ''));
    errors.password = passwordError(String(form.get('password') || ''), register);
    if (register) {
        errors.name = nameError(String(form.get('name') || ''));
        errors.phone = phoneError(String(form.get('phone') || ''));
        if (form.get('password') !== form.get('confirmPassword')) errors.confirmPassword = 'Passwords do not match.';
    }
    return Object.fromEntries(Object.entries(errors).filter(([, value]) => value));
}
export function validateDelivery(address: Delivery): FieldErrors {
    const errors: FieldErrors = {
        recipientName: nameError(address.recipientName, 255), phone: phoneError(address.phone),
        city: nameError(address.city), country: nameError(address.country),
    };
    for (const field of ['addressLine1', 'addressLine2'] as const) {
        const value = address[field];
        if (field === 'addressLine2' && !value) continue;
        if (!value.trim()) errors[field] = 'Enter your street address.';
        else if (value.length > 255 || /[\u0000-\u001f\u007f]/.test(value) || !/[\p{L}\p{N}]/u.test(value)) errors[field] = 'Enter a valid address, up to 255 characters.';
    }
    if (!address.postalCode || address.postalCode.length > 20 || !/^[\p{L}\p{N}][\p{L}\p{N} -]*$/u.test(address.postalCode)) errors.postalCode = 'Use letters, numbers, spaces or hyphens (up to 20 characters).';
    return Object.fromEntries(Object.entries(errors).filter(([, value]) => value));
}
