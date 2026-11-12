type ProductCardProps = {
    name: string;
    price: number;
    quantity: number;
    onAdd: () => void;
    onRemove: () => void;
};

function ProductCard({
    name,
    price,
    quantity,
    onAdd,
    onRemove,
}: ProductCardProps) {
    return (
        <article className="product-card">
            <h2>{name}</h2>
            <p>Price: {price.toLocaleString()} UZS</p>

            <button
                disabled={quantity <= 0}
                onClick={() => {
                    if (quantity <= 0) return;
                    onRemove();
                }}
            >
                −
            </button>

            <span>Added: {quantity}</span>

            <button onClick={onAdd}>+</button>
        </article>
    );
}

export default ProductCard;
