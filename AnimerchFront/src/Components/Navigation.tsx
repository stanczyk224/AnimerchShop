import './Navigation.css';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

function Navigation() {
    const { isAuthenticated, logout } = useAuth();

    return (
        <nav className="navbar">
            <ul className="nav-links">

                <li className="navbar-logo">
                    <Link to="/" className="nav-logo">Feed</Link>
                </li>

                <li><Link to="/whishlist">Wishlist</Link></li>

                {isAuthenticated && (
                    <>
                        <li><Link to="/cart">My Cart</Link></li>
                        <li><Link to="/history">My History</Link></li>
                    </>
                )}

                {!isAuthenticated ? (
                    <>
                        <li><Link to="/signin">Sign In</Link></li>
                        <li><Link to="/signup">Sign Up</Link></li>
                    </>
                ) : (
                    <li>
                        <button
                            className="logout-btn"
                            onClick={logout}
                        >
                            Logout
                        </button>
                    </li>
                )}

            </ul>
        </nav>
    );
}

export default Navigation;