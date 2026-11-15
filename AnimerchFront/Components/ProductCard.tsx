import "./ProductCard.css"

interface ProductCardProps {
  name: string;
  description: string;
  price: number;
  quantity: number;
  image: string;
}

function ProductCard({name, description, price, quantity, image}: ProductCardProps) {
  return (
    <div className="product-card">
      <div className="product-card-header">
        <h2 className="product-name">{name}</h2>
      </div>
      
      <img src={image} alt="Product Image" className="product-image" />
      <div className="product-card-main">
        <p className="product-description">{description}</p>
        
        <p className="product-price">Price: {price}</p>
      </div>
      <div className="product-card-controls">
        <p className='product-quantity'>Remains: {quantity}</p>
        <button className="quantity-button">-</button>
        <span className="quantity">Quantity: 0</span>
        <button className="quantity-button">+</button>
      </div>
    </div>
  );
}

export default ProductCard;