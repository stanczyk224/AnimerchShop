import { useState, useRef, useLayoutEffect } from 'react';
import ProductCard from './ProductCard';
import { useDebounce } from '../Hooks/useDebounce';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import './Feed.css';
import SearchBar from './Search';
import { Pagination, type PageResponse } from './Pagination';

interface Photo {
  id: number;
  url: string;
}

interface Product {
  id: number;
  name: string;
  description: string;
  model: string;
  weight: string;
  quantity: number;
  price: number;
  photos: Photo[];
}

const API_BASE_URL = 'http://localhost:8080/api/products';
const PLACEHOLDER_IMAGE = 'https://placehold.co/724/110000/FF0000?text=Product+Photo';
const PAGE_SIZE = 15;

function Feed() {
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const debouncedSearchTerm = useDebounce(searchTerm.trim(), 500);

  // Refs to lock the camera on the pagination bar
  const paginationWrapperRef = useRef<HTMLDivElement>(null);
  const savedOffsetRef = useRef<number | null>(null);

  const { data, isLoading, isFetching, error } = useQuery<PageResponse<Product>>({
    queryKey: ['products', page, PAGE_SIZE, debouncedSearchTerm],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: page.toString(),
        size: PAGE_SIZE.toString(),
      });

      if (debouncedSearchTerm) {
        params.append('search', debouncedSearchTerm);
      }

      const res = await fetch(`${API_BASE_URL}?${params.toString()}`);
      if (!res.ok) {
        throw new Error(`Failed to fetch products: ${res.status}`);
      }
      return res.json();
    },
    placeholderData: keepPreviousData,
  });

  const handleSearch = (query: string) => {
    setSearchTerm(query);
    setPage(0);
  };

  const handlePageChange = (newPage: number) => {
    // 1. Capture exact vertical screen position of the pagination bar before fetch
    if (paginationWrapperRef.current) {
      savedOffsetRef.current = paginationWrapperRef.current.getBoundingClientRect().top;
    }
    setPage(newPage);
  };

  // 2. When new page `data` arrives, instantly adjust scroll so the bar stays under your cursor
  useLayoutEffect(() => {
    if (paginationWrapperRef.current && savedOffsetRef.current !== null) {
      const currentTop = paginationWrapperRef.current.getBoundingClientRect().top;
      const diff = currentTop - savedOffsetRef.current;

      if (diff !== 0) {
        window.scrollBy({ top: diff, behavior: 'instant' });
      }
      savedOffsetRef.current = null;
    }
  }, [data]);

  return (
    <div className="feed">
      <SearchBar placeholder="Search products" onSearch={handleSearch} />

      {error && <div className="error">Error: {error.message}</div>}

      {isLoading ? (
        <h2 className="loading">Loading...</h2>
      ) : (
        <>
          {/* Dim existing cards slightly while fetching instead of inserting a <p> tag */}
          <div className={`product-cards ${isFetching ? 'fetching' : ''}`}>
            {data?.content.map((product) => {
              const firstPhoto = product.photos?.[0];
              const imageUrl = firstPhoto
                ? `${API_BASE_URL}/${product.id}/photos/${firstPhoto.id}/content`
                : PLACEHOLDER_IMAGE;

              return (
                <ProductCard
                  key={product.id}
                  name={product.name}
                  description={product.description}
                  price={product.price}
                  quantity={product.quantity}
                  image={imageUrl}
                />
              );
            })}
          </div>

          {data && (
            <div ref={paginationWrapperRef}>
              <Pagination
                totalPages={data.totalPages}
                number={data.number}
                first={data.first}
                last={data.last}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default Feed;
