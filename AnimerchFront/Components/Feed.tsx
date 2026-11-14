import ProductCard from './ProductCard';
import Banner from './Banner';
import Welcome from './Welcome';
import { useQuery } from '@tanstack/react-query';
import './Feed.css';

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
  const { data, isLoading, error } = useQuery<PageResponse<Product>>({
    queryKey: ['products', 0, 20],
    queryFn: async () => {
      const res = await fetch(`${API_BASE_URL}?page=0&size=20`);
      if (!res.ok) {
        throw new Error(`Failed to fetch products: ${res.status}`);
      }
      return res.json();
    },
  });

  if (isLoading) {
    return <h2>Loading...</h2>;
  }

  if (error) {
    return <div className="error">Error: {error.message}</div>;
  }

  return (
    <div className="feed">
      <Welcome />
      <div className="banners">
        <Banner />
      </div>
      <div className="product-cards">
        {data?.content.map((product) => {
          // 1. Grab the first photo object if it exists
          const firstPhoto = product.photos?.[0];

          // 2. Build the exact Spring endpoint URL using product.id and photo.id
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
    </div>
  );
}

export default Feed;
