import { useEffect, useState } from 'react';
import { api } from './api';
import type { CatalogFilterValues } from './productFilters';

type Option = { id: number; name: string };
type Props = { value: CatalogFilterValues; onChange: (value: CatalogFilterValues) => void; onClear: () => void; error: string; pageSize: number; onPageSize: (size: number) => void };
export default function CatalogFilters({ value, onChange, onClear, error, pageSize, onPageSize }: Props) {
    const [sellers, setSellers] = useState<Option[]>([]);
    const [categories, setCategories] = useState<Option[]>([]);
    const [loading, setLoading] = useState(true);
    const [optionsError, setOptionsError] = useState('');
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        async function loadSellers() {
            const all: Option[] = [];
            for (let page = 0; ; page++) {
                const result = await api<{ content: Option[]; totalPages: number }>(`/sellers?page=${page}&size=20&sort=name,asc&sort=id,asc`, { signal: controller.signal });
                all.push(...result.content);
                if (page + 1 >= result.totalPages) return all;
            }
        }
        Promise.allSettled([loadSellers(), api<Option[]>('/categories', { signal: controller.signal })]).then(results => {
            if (controller.signal.aborted) return;
            if (results[0].status === 'fulfilled') setSellers(results[0].value);
            if (results[1].status === 'fulfilled') setCategories(results[1].value);
            setOptionsError(results.some(result => result.status === 'rejected') ? 'Some filter options could not load. You can retry.' : '');
            setLoading(false);
        });
        return () => controller.abort();
    }, [retry]);
    function update(field: keyof CatalogFilterValues, next: string) { onChange({ ...value, [field]: next }); }
    return <div className="advanced-filters">
        <label>Seller<select id="seller-filter" value={value.sellerId} disabled={loading} onChange={event => update('sellerId', event.target.value)}><option value="">All sellers</option>{value.sellerId && !sellers.some(seller => String(seller.id) === value.sellerId) && <option value={value.sellerId}>Seller #{value.sellerId}</option>}{sellers.map(seller => <option value={seller.id} key={seller.id}>{seller.name}</option>)}</select></label>
        <label>Category<select value={value.categoryId} disabled={loading} onChange={event => update('categoryId', event.target.value)}><option value="">All categories</option>{categories.map(category => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
        <label>Min price · UZS<input inputMode="decimal" value={value.minPrice} onChange={event => update('minPrice', event.target.value)} placeholder="0" aria-invalid={!!error}/></label>
        <label>Max price · UZS<input inputMode="decimal" value={value.maxPrice} onChange={event => update('maxPrice', event.target.value)} placeholder="No limit" aria-invalid={!!error}/></label>
        <label>Per page<select value={pageSize} onChange={event => onPageSize(Number(event.target.value))}><option value={6}>6 pieces</option><option value={12}>12 pieces</option><option value={20}>20 pieces</option></select></label>
        <div className="filter-actions"><button className="primary" type="submit">Apply filters ↗</button><button className="text-button" type="button" onClick={onClear}>Clear all</button></div>
        {error && <p className="field-error full-width" role="alert">{error}</p>}
        {loading && <small className="muted full-width" role="status">Loading seller and category options…</small>}
        {optionsError && <p className="field-error full-width" role="alert">{optionsError} <button type="button" className="text-button" onClick={() => { setLoading(true); setRetry(value => value + 1); }}>Retry</button></p>}
    </div>;
}
