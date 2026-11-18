
import "./App.css";

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthProvider";

import Navigation from "./Components/Navigation";
import Main from "./Components/Main";
import Welcome from "./Components/Welcome";
import Banner from "./Components/Banner";
import Footer from "./Components/Footer";

import SignIn from "./pages/SignIn";
import SignUp from "./pages/SignUp";
import CartPage from "./Components/CartPage";

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <div className="App">
                    <Navigation />

                    <Routes>
                        <Route path="/signin" element={<SignIn />} />
                        <Route path="/signup" element={<SignUp />} />
                        <Route path="/cart" element={<CartPage />} />
                    </Routes>

                    <Welcome />
                    <div className="banners">
                        <Banner />
                    </div>
                    <Main />
                    <Footer />
                </div>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;
