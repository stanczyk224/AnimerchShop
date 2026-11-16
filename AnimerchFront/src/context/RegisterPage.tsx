
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { register } from "../api/authApi";

export default function RegisterPage() {

    const navigate = useNavigate();

    const [form, setForm] = useState({
        name: "",
        phone: "",
        email: "",
        password: "",
    });

    const [error, setError] = useState("");

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value,
        });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");

        try {
            const response = await register(form);

            console.log("Registered user:", response);

            navigate("/login");
        } catch (error: unknown) {
        if (axios.isAxiosError(error)) {
            setError(
                error.response?.data?.message ??
                "Registration failed"
            );
        } else {
            setError("An unexpected error occurred");
        }
    }
    };

    return (
        <form onSubmit={handleSubmit}>
            <h1>Create account</h1>

            <input name="name" placeholder="Name"
                value={form.name} onChange={handleChange} required />

            <input name="phone" placeholder="Phone"
                value={form.phone} onChange={handleChange} required />

            <input name="email" type="email" placeholder="Email"
                value={form.email} onChange={handleChange} required />

            <input name="password" type="password" placeholder="Password"
                value={form.password} onChange={handleChange} required />

            {error && <p>{error}</p>}

            <button type="submit">Register</button>
        </form>
    );
}
