
import "./ProductCard.css";
import { useCart } from "../hooks/useCart";

interface ProductCardProps {
    id: number;
    name: string;
    description: string;
    price: number;
    quantity: number;
    image: string;
}

function ProductCard({
    id,
    name,
    description,
    price,
    quantity,
    image
}: ProductCardProps) {

    const {
        cartQuery,
        addMutation,
        updateMutation,
        removeMutation
    } = useCart();

    const cartItem = cartQuery.data?.find(
        item => item.productId === id
    );

    const quantityNumber = cartItem?.quantity ?? 0;

    const isPending =
        addMutation.isPending ||
        updateMutation.isPending ||
        removeMutation.isPending;

    function handleQuantityPlus() {
        if (quantityNumber >= quantity || isPending) return;

        if (!cartItem) {
            addMutation.mutate(id);
        } else {
            updateMutation.mutate({
                productId: id,
                quantity: quantityNumber + 1
            });
        }
    }

    function handleQuantityMinus() {
        if (quantityNumber <= 0 || isPending) return;

        if (quantityNumber === 1) {
            removeMutation.mutate(id);
        } else {
            updateMutation.mutate({
                productId: id,
                quantity: quantityNumber - 1
            });
        }
    }

    return (
        <div className="product-card">
            <div className="product-card-header">
                <h2 className="product-name">{name}</h2>
            </div>

            <img
                src={image}
                alt={name}
                className="product-image"
            />

            <div className="product-card-main">
                <p className="product-description">{description}</p>
                <p className="product-price">Price: {price}</p>
            </div>

            <div className="product-card-controls">
                <p className="product-quantity">
                    Remains: {quantity}
                </p>

                <button
                    className="quantity-button"
                    onClick={handleQuantityMinus}
                    disabled={quantityNumber === 0 || isPending}
                >
                    -
                </button>

                <span className="quantity">
                    Quantity: {quantityNumber}
                </span>

                <button
                    className="quantity-button"
                    onClick={handleQuantityPlus}
                    disabled={quantityNumber >= quantity || isPending}
                >
                    +
                </button>
            </div>
        </div>
    );
}

export default ProductCard;
