import { useState } from 'react';
import ProductCard from './ProductCard';
import Banner from './Banner';
import { useDebounce } from './useDebounce';
import Welcome from './Welcome';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import './Feed.css';
import SearchBar from './Search';

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

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

const API_BASE_URL = 'http://localhost:8080/api/products';
const PLACEHOLDER_IMAGE = 'https://placehold.co/724/110000/FF0000?text=Product+Photo';

function Feed() {
  const [searchTerm, setSearchTerm] = useState<string>('');
  const debouncedSearchTerm = useDebounce(searchTerm.trim(), 500);

  const { data, isLoading, isFetching, error } = useQuery<PageResponse<Product>>({
    queryKey: ['products', 0, 20, debouncedSearchTerm],
    queryFn: async () => {
      // Fixed: Use `debouncedSearchTerm` (not `searchTerm`) and removed duplicate page/size params
      const url = debouncedSearchTerm
        ? `${API_BASE_URL}?page=0&size=20&search=${encodeURIComponent(debouncedSearchTerm)}`
        : `${API_BASE_URL}?page=0&size=20`;

      const res = await fetch(url);
      if (!res.ok) {
        throw new Error(`Failed to fetch products: ${res.status}`);
      }
      return res.json();
    },
    placeholderData: keepPreviousData, // Keeps existing products on screen while fetching
  });

  const handleSearch = (query: string) => {
    setSearchTerm(query);
  };

  return (
    <div className="feed">
      <Welcome />
      <div className="banners">
        <Banner />
      </div>
      <SearchBar placeholder="Search products" onSearch={handleSearch} />

      {/* 2. Handle loading and errors ONLY inside the results section below SearchBar */}
      {error && <div className="error">Error: {error.message}</div>}

      {isLoading ? (
        <h2 className='loading'>Loading...</h2>
      ) : (
        <>
          {isFetching && <p>Searching...</p>}
          <div className="product-cards">
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
        </>
      )}
    </div>
  );
}

export default Feed;
