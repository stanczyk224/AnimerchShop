import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { api, ApiError } from './api';
import { prepareCart } from './checkoutService';
import type { BasketLine } from './checkoutService';
import { normalizedPhone, validateDelivery } from './validation';
import type { Delivery, FieldErrors } from './validation';

type Order = { id: number; totalPrice: number; deliveryAddress: Delivery; items: { productName: string; quantity: number; unitPrice: number }[] };
type Attempt = { key: string; owner: string; address: Delivery; lines: BasketLine[] };
const STORAGE = 'animerch.pending-checkout.v1';
const emptyAddress: Delivery = { recipientName: '', phone: '', addressLine1: '', addressLine2: '', city: '', postalCode: '', country: 'Uzbekistan' };
const money = (value: number) => new Intl.NumberFormat('en-US').format(value) + ' UZS';
function owner(token: string) {
    try { return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).sub as string; }
    catch { return ''; }
}
function readAttempt(): Attempt | null {
    try {
        const attempt = JSON.parse(sessionStorage.getItem(STORAGE) || 'null');
        if (!attempt || typeof attempt.key !== 'string' || typeof attempt.owner !== 'string' || !Array.isArray(attempt.lines) || !attempt.lines.length || Object.keys(validateDelivery(attempt.address)).length) return null;
        return attempt;
    } catch { return null; }
}
type Props = {
    open: boolean; token: string; lines: BasketLine[]; onClose: () => void;
    onAuth: () => void; onComplete: () => void; onLock: (locked: boolean) => void;
    onHistory: () => void;
};

export default function Checkout({ open, token, lines, onClose, onAuth, onComplete, onLock, onHistory }: Props) {
    const [attempt, setAttempt] = useState<Attempt | null>(readAttempt);
    const [address, setAddress] = useState<Delivery>(() => readAttempt()?.address || emptyAddress);
    const [errors, setErrors] = useState<FieldErrors>({});
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);
    const [stage, setStage] = useState('');
    const [consent, setConsent] = useState(false);
    const [order, setOrder] = useState<Order | null>(null);
    const inFlight = useRef(false);
    const displayedLines = attempt?.lines || lines;
    const total = displayedLines.reduce((sum, line) => sum + line.product.price * line.quantity, 0);

    useEffect(() => { onLock(!!attempt || busy); }, [attempt, busy, onLock]);
    useEffect(() => {
        if (!open) return;
        const escape = (event: KeyboardEvent) => {
            if (event.key !== 'Escape' || busy) return;
            if (order) { setOrder(null); setAddress(emptyAddress); }
            onClose();
        };
        document.addEventListener('keydown', escape);
        return () => document.removeEventListener('keydown', escape);
    }, [open, busy, order, onClose]);
    useEffect(() => {
        if (!attempt && !busy) return;
        const prevent = (event: BeforeUnloadEvent) => { event.preventDefault(); };
        window.addEventListener('beforeunload', prevent);
        return () => window.removeEventListener('beforeunload', prevent);
    }, [attempt, busy]);

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (inFlight.current) return;
        if (!token) { onAuth(); return; }
        if (attempt && attempt.owner !== owner(token)) { setError('Sign in with the account that started this order before retrying.'); return; }
        const clean = Object.fromEntries(Object.entries(address).map(([key, value]) => [key, value.trim()])) as Delivery;
        const invalid = validateDelivery(clean);
        setErrors(invalid); setError('');
        if (!attempt && Object.keys(invalid).length) {
            document.getElementById('delivery-' + Object.keys(invalid)[0])?.focus(); return;
        }
        if (!attempt && !consent) { setErrors({ consent: 'Confirm the basket and delivery details before placing the order.' }); return; }
        if (!displayedLines.length) { setError('Your basket is empty. Add a product before checking out.'); return; }
        inFlight.current = true; setBusy(true);
        let submitted = false;
        try {
            let current = attempt;
            if (!current) {
                setStage('Preparing your cart…');
                await prepareCart(lines, token);
                current = { key: crypto.randomUUID(), owner: owner(token), address: { ...clean, phone: normalizedPhone(clean.phone) }, lines: structuredClone(lines) };
                // Save before POST, so a reload can retry the same order safely.
                sessionStorage.setItem(STORAGE, JSON.stringify(current));
                setAttempt(current);
                setAddress(current.address);
            }
            setStage('Placing your order…');
            submitted = true;
            const result = await api<Order>('/order/checkout', {
                token, body: { deliveryAddress: current.address }, headers: { 'Idempotency-Key': current.key },
            });
            setOrder(result); setAttempt(null); sessionStorage.removeItem(STORAGE);
            setConsent(false); onComplete();
        } catch (cause) {
            if (submitted && cause instanceof ApiError && [400, 403, 404, 409, 422].includes(cause.status)) {
                setAttempt(null); sessionStorage.removeItem(STORAGE);
            }
            setError(cause instanceof Error ? cause.message : 'Checkout could not be completed.');
            if (cause instanceof ApiError && cause.status === 401) onAuth();
        } finally { inFlight.current = false; setBusy(false); setStage(''); }
    }

    if (!open) return null;
    function close() {
        if (busy) return;
        if (order) { setOrder(null); setAddress(emptyAddress); }
        onClose();
    }
    return <div className="overlay" onClick={close}>
        <section className="modal checkout-modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title" onClick={event => event.stopPropagation()}>
            <button className="close" aria-label="Close checkout" disabled={busy} onClick={close}>×</button>
            {order ? <div className="order-success">
                <span className="success-seal" aria-hidden="true">✓</span><div className="eyebrow">YOUR NEXT CHAPTER</div>
                <h2 id="dialog-title">Order confirmed<span>.</span></h2><p role="status">Order #{order.id} has been placed successfully.</p>
                <div className="checkout-summary">{order.items.map((item, index) => <div className="summary-row" key={index}><span>{item.productName} × {item.quantity}</span><strong>{money(item.unitPrice * item.quantity)}</strong></div>)}<div className="basket-total"><span>Order total</span><strong>{money(order.totalPrice)}</strong></div></div>
                <p>Deliver to {order.deliveryAddress.recipientName}<br/>{order.deliveryAddress.addressLine1}{order.deliveryAddress.addressLine2 && ', ' + order.deliveryAddress.addressLine2}<br/>{order.deliveryAddress.city}, {order.deliveryAddress.postalCode}, {order.deliveryAddress.country}<br/>{order.deliveryAddress.phone}</p>
                <p className="muted">Your order has been recorded. No online payment was collected.</p>
                <button className="primary" onClick={() => { setOrder(null); setAddress(emptyAddress); onClose(); }}>Continue shopping ↗</button>
                <button className="text-button receipt-history" onClick={() => { setOrder(null); setAddress(emptyAddress); onHistory(); }}>View my orders ↗</button>
            </div> : <>
                <div className="eyebrow">THE FINAL CHAPTER</div><h2 id="dialog-title">Make it yours<span>.</span></h2>
                <p>Review your basket and enter your delivery details.</p>
                <div className="checkout-summary">{displayedLines.map(line => <div className="summary-row" key={line.product.id}><span>{line.product.name} × {line.quantity}</span><strong>{money(line.product.price * line.quantity)}</strong></div>)}<div className="basket-total"><span>Estimated total</span><strong>{money(total)}</strong></div><small className="muted">Final prices and availability are checked by the store when placing your order. No online payment is collected.</small></div>
                {attempt && <p className="retry-note" role="status">An order request is pending confirmation. Retry below to retrieve its result safely. Delivery details and the basket stay locked until it is resolved.</p>}
                <form className="login-form delivery-form" onSubmit={submit} noValidate>
                    <fieldset disabled={busy || !!attempt}>
                        <div className="delivery-grid">{([
                            ['recipientName', 'Recipient name', 'Your full name', 'name', 255],
                            ['phone', 'Phone number', '+998 90 123 45 67', 'tel', 30],
                            ['addressLine1', 'Street address', 'Street and house number', 'address-line1', 255],
                            ['addressLine2', 'Apartment / unit (optional)', 'Apartment, floor, building', 'address-line2', 255],
                            ['city', 'City', 'Tashkent', 'address-level2', 100],
                            ['postalCode', 'Postal code', '100000', 'postal-code', 20],
                            ['country', 'Country', 'Uzbekistan', 'country-name', 100],
                        ] as const).map(([field, label, placeholder, autocomplete, max]) => <label key={field} className={field.startsWith('address') ? 'full-width' : ''}>
                            {label}<input id={'delivery-' + field} type={field === 'phone' ? 'tel' : 'text'} name={field} autoComplete={autocomplete} maxLength={max} required={field !== 'addressLine2'} value={address[field]} placeholder={placeholder} aria-invalid={!!errors[field]} aria-describedby={errors[field] ? 'error-' + field : undefined} onChange={event => { setAddress(current => ({ ...current, [field]: event.target.value })); setErrors(current => ({ ...current, [field]: '' })); }}/>
                            {errors[field] && <small id={'error-' + field} className="field-error">{errors[field]}</small>}
                        </label>)}</div>
                        <label className="checkout-consent"><input type="checkbox" checked={consent} onChange={event => { setConsent(event.target.checked); setErrors(current => ({ ...current, consent: '' })); }}/><span>I confirm these items and delivery details. Use this basket for checkout, replacing any items previously saved in my account cart.</span></label>
                        {errors.consent && <p className="field-error">{errors.consent}</p>}
                    </fieldset>
                    {error && <div className="form-error" role="alert">{error}</div>}
                    <button className="primary" disabled={busy || !displayedLines.length}>{busy ? <><span className="spinner"/>{stage}</> : attempt ? 'Retry same order ↗' : 'Place order ↗'}</button>
                    <small className="muted">Only click Place order when you are ready to purchase. Duplicate clicks and retries reuse the same purchase.</small>
                </form>
            </>}
        </section>
    </div>;
}
