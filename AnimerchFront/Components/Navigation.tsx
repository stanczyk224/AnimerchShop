import './Navigation.css'
function Navigation() {

  return (
    <nav className="navbar">
      <ul className="nav-links">
        <li className="navbar-logo"><a href="/" className="nav-logo">Feed</a></li>

        <li><a href="/whishlist">Whishlist</a></li>
        <li><a href="/cart">My Cart</a></li>
        <li><a href="/history">My History</a></li>
        <li><a href="/signin">SignIn</a></li>
        <li><a href="/signup">SignUp</a></li>
      </ul>
    </nav>
  )
}

export default Navigation