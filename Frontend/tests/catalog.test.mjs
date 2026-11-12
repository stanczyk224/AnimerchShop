import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyFilters, productParams, validateFilters } from '../src/productFilters.ts';

test('combined filters are sent to the server before pagination', () => {
    const params = productParams({ sellerId: '4', categoryId: '7', minPrice: '0', maxPrice: '250000.50' }, '  Naruto  ', 'price,asc', 2, 20);
    for (const [key, value] of Object.entries({ sellerId: '4', categoryId: '7', minPrice: '0', maxPrice: '250000.50', search: 'Naruto', sort: 'price,asc', page: '2', size: '20' })) assert.equal(params.get(key), value);
    const reset = productParams(emptyFilters, '', 'id,desc', 0, 12);
    assert.equal(reset.has('sellerId'), false);
    assert.equal(reset.has('minPrice'), false);
});
test('price filters reject negative, malformed and inverted ranges', () => {
    for (const values of [{ minPrice: '-1' }, { maxPrice: 'abc' }, { maxPrice: '1.123' }, { minPrice: '10', maxPrice: '9' }]) assert.ok(validateFilters({ ...emptyFilters, ...values }));
    assert.equal(validateFilters({ ...emptyFilters, minPrice: '0', maxPrice: '100.50' }), '');
});
