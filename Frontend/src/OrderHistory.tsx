import { useEffect, useState } from 'react';
import { api, ApiError } from './api';
type Item = { id: number; productName: string; quantity: number; unitPrice: number; status: string };
type Address = { recipientName: string; phone: string; addressLine1: string; addressLine2: string | null; city: string; postalCode: string; country: string };
type Order = { id: number; items: Item[]; deliveryAddress: Address | null; totalPrice: number; originalTotal: number; cancelledTotal: number; activeTotal: number };
type Page = { content: Order[]; totalPages: number; totalElements: number };
const money = (value: number) => new Intl.NumberFormat('en-US').format(value) + ' UZS';
type Props = { token: string; revision: number; onAuth: () => void; onClose: () => void };

export default function OrderHistory({ token, revision, onAuth, onClose }: Props) {
    const [data, setData] = useState<Page | null>(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [retry, setRetry] = useState(0);
    const [authExpired, setAuthExpired] = useState(false);
    const [expanded, setExpanded] = useState<number | null>(null);
    useEffect(() => {
        const controller = new AbortController();
        api<Page>(`/order/my?page=${page}&size=5`, { token, signal: controller.signal })
            .then(setData)
            .catch(error => {
                if (controller.signal.aborted) return;
                setAuthExpired(error instanceof ApiError && error.status === 401);
                setError(error instanceof ApiError && error.status === 404
                    ? 'Order history is available for customer accounts. An administrator account may not have a customer profile.'
                    : error instanceof Error ? error.message : 'Unable to load orders.');
            })
            .finally(() => { if (!controller.signal.aborted) setLoading(false); });
        return () => controller.abort();
    }, [token, page, retry, revision]);
    function changePage(next: number) { setLoading(true); setError(''); setExpanded(null); setPage(next); }
    return <div className="overlay" onClick={onClose}><section className="modal history-modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title" onClick={event => event.stopPropagation()}>
        <button className="close" aria-label="Close order history" onClick={onClose}>×</button>
        <div className="eyebrow">YOUR JOURNEY</div><h2 id="dialog-title">My orders<span>.</span></h2><p>Your purchases, delivery details and item statuses.</p>
        {loading ? <div className="state-panel" role="status">Loading your orders…</div> : error ? <div className="state-panel" role="alert"><p>{error}</p><button className="primary" onClick={() => { if (authExpired) onAuth(); else { setLoading(true); setError(''); setRetry(value => value + 1); } }}>{authExpired ? 'Sign in again' : 'Retry'} ↗</button></div> : data?.content.length ? <>
            <div className="result-count">{data.totalElements} orders · Newest first</div>
            <div className="order-list">{data.content.map(order => <article className="order-card" key={order.id}>
                <div className="order-card-heading"><div><span className="eyebrow">ORDER #{order.id}</span><h3>{order.items.reduce((sum, item) => sum + item.quantity, 0)} items</h3></div><strong>{money(order.originalTotal ?? order.totalPrice)}</strong></div>
                <div className="order-item-list">{order.items.map(item => <div className="order-line" key={item.id}><div><strong>{item.productName}</strong><p>{item.quantity} × {money(item.unitPrice)}</p></div><span className={`status-badge status-${item.status.toLowerCase()}`}>{item.status.toLowerCase()}</span></div>)}</div>
                <button className="text-button order-details-toggle" aria-expanded={expanded === order.id} aria-controls={`order-details-${order.id}`} onClick={() => setExpanded(current => current === order.id ? null : order.id)}>{expanded === order.id ? 'Hide details −' : 'Delivery & totals +'}</button>
                {expanded === order.id && <div className="order-details" id={`order-details-${order.id}`}>
                    {order.deliveryAddress ? <address><strong>{order.deliveryAddress.recipientName}</strong><br/>{order.deliveryAddress.addressLine1}{order.deliveryAddress.addressLine2 && ', ' + order.deliveryAddress.addressLine2}<br/>{order.deliveryAddress.city}, {order.deliveryAddress.postalCode}, {order.deliveryAddress.country}<br/>{order.deliveryAddress.phone}</address> : <p className="muted">Delivery details were not recorded for this older order.</p>}
                    <dl><div><dt>Original total</dt><dd>{money(order.originalTotal ?? order.totalPrice)}</dd></div><div><dt>Cancelled items</dt><dd>{money(order.cancelledTotal ?? 0)}</dd></div><div><dt>Active total</dt><dd>{money(order.activeTotal ?? order.totalPrice)}</dd></div></dl>
                </div>}
            </article>)}</div>
            <div className="pagination"><button disabled={page === 0} onClick={() => changePage(page - 1)}>← Previous</button><span>Page {page + 1} of {data.totalPages}</span><button disabled={page + 1 >= data.totalPages} onClick={() => changePage(page + 1)}>Next →</button></div>
        </> : <div className="state-panel"><span className="state-symbol">✦</span><h3>Your story starts here.</h3><p>Your first order will appear here after checkout.</p><button className="primary" onClick={onClose}>Continue shopping ↗</button></div>}
    </section></div>;
}
