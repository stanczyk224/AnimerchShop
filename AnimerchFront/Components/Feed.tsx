import ProductCard from './ProductCard';
import Banner from './Banner';
import Welcome from './Welcome';
import './Feed.css'
function Feed() {
  return (
    <div className="feed">
      <Welcome />

      <div className="banners">
        <Banner />
      </div>
        <div className="product-cards">
            <ProductCard />
            <ProductCard />
            <ProductCard />
            <ProductCard />
            <ProductCard />
        </div>
    </div>
  );
}

export default Feed;