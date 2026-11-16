
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import "./Auth.css";
export default function SignIn() {
    const { login } = useAuth();
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            await login({ email, password });
            navigate("/");
        } catch (error: unknown) {
            console.error("Login error:", error);

            setError(
                error instanceof Error
                    ? error.message
                    : "Unexpected login error"
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <form className="auth-form" onSubmit={handleSubmit}>
                <h2>Sign In</h2>

                {error && <p className="error">{error}</p>}

                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />

                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />

                <button type="submit" disabled={loading}>
                    {loading ? "Signing in..." : "Sign In"}
                </button>

                <p>
                    Don't have an account? <Link to="/signup">Sign Up</Link>
                </p>
                <button
                    type="button"
                    className="auth-close"
                    onClick={() => navigate("/")}
                >
                    ×
                </button>
            </form>
        </div>
    );
}

