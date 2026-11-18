import './CartPage.css'

function CartPage() {
    return(
        <>
            <div className="cart-overlay">
                <div className='cart-modal' onClick={(e) => e.stopPropagation()}>
                    <div className='cart-header'>
                        <h2>Cart</h2>
                         <button className="close-btn" aria-label="Close cart">
                                     &times;
                         </button>
                    </div>
                </div>
            </div>
        </>
    );
}

export default CartPage;