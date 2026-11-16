import { useState, type ChangeEvent, type SubmitEvent } from "react";
import "./Search.css";
interface SearchProps {
    placeholder?: string;
    onSearch?: (query: string) => void;
}

function Search({ placeholder = "Search product...", onSearch }: SearchProps) {
    const [input, setInput] = useState<string>("");

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setInput(value);
        if (onSearch) {
            onSearch(value);
        }
    };

    const handleSubmit = (e: SubmitEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (onSearch) {
            onSearch(input.trim());
        }
    };

    return (
        <div className="search">
            <form onSubmit={handleSubmit} className="search-form">
                <input
                    type="search"
                    placeholder={placeholder}
                    name="search"
                    id="search-bar"
                    value={input}
                    onChange={handleChange}
                />
                <button type="submit" className="search-submit">
                    Search
                </button>
            </form>
        </div>
    );
}

export default Search;
