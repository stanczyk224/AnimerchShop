export type CatalogFilterValues = { sellerId: string; categoryId: string; minPrice: string; maxPrice: string };
export const emptyFilters: CatalogFilterValues = { sellerId: '', categoryId: '', minPrice: '', maxPrice: '' };
export function validateFilters(filters: CatalogFilterValues): string {
    for (const value of [filters.minPrice, filters.maxPrice]) {
        if (value && (!/^\d+(?:\.\d{1,2})?$/.test(value) || !Number.isFinite(Number(value)))) return 'Enter non-negative prices with at most two decimal places.';
    }
    if (filters.minPrice && filters.maxPrice && Number(filters.minPrice) > Number(filters.maxPrice)) return 'Minimum price cannot be higher than maximum price.';
    return '';
}
export function productParams(filters: CatalogFilterValues, search: string, sort: string, page: number, size: number): URLSearchParams {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort });
    if (search.trim()) params.set('search', search.trim());
    for (const [key, value] of Object.entries(filters)) if (value !== '') params.set(key, value);
    return params;
}
