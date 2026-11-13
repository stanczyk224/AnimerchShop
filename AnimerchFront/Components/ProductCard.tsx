import './ProductCard.css';
import Image from '../public/berser tshirt.jpg'
function ProductCard() {
  return (
    <div className="product-card">
      <div className="product-card-header">
        <h2 className="product-name">Name of product</h2>
      </div>
      
      <img src={Image} alt="Product Image" className="product-image" />
      <div className="product-card-main">
        <p className="product-description">Description of the product. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
        <p className="product-price">Price: $XX.XX</p>
      </div>
      <div className="product-card-controls">
        <button className="quantity-button">-</button>
        <span className="quantity">Quantity: 0</span>
        <button className="quantity-button">+</button>
      </div>
    </div>
  );
}

export default ProductCard;