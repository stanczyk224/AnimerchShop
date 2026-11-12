import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import Checkout from "./Checkout";
import CatalogFilters from "./CatalogFilters";
import OrderHistory from "./OrderHistory";
import { emptyFilters, productParams, validateFilters } from "./productFilters";
import type { CatalogFilterValues } from "./productFilters";
import { api } from "./api";
import { normalizedPhone, validateAccount } from "./validation";
import type { FieldErrors } from "./validation";
import "./store.css";
import "./checkout.css";
import "./catalog.css";

type Product = {
    id: number;
    name: string;
    description: string;
    price: number;
    quantity: number;
    averageRating: number | null;
    reviewCount: number;
    seller: { id: number; name: string } | null;
    categories: { id: number; name: string }[];
    photos: { id: number; url: string }[];
};
type Page = { content: Product[]; totalElements: number; totalPages: number };
const money = (value: number) =>
    new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(value) +
    " UZS";

async function request<T>(
    path: string,
    token: string,
    signal?: AbortSignal,
    body?: unknown,
): Promise<T> {
    return api<T>(path, { token, signal, body });
}

function Artwork() {
    return (
        <svg className="hero-art" viewBox="0 0 700 520" aria-hidden="true">
            <defs>
                <radialGradient id="eclipse">
                    <stop stopColor="#ae262b" />
                    <stop offset="1" stopColor="#1a1013" />
                </radialGradient>
                <pattern
                    id="rain"
                    width="17"
                    height="23"
                    patternUnits="userSpaceOnUse"
                    patternTransform="rotate(24)"
                >
                    <path d="M0 0v12" stroke="#e7d5bd" strokeOpacity=".08" />
                </pattern>
            </defs>
            <circle cx="430" cy="210" r="175" fill="url(#eclipse)" />
            <circle
                cx="430"
                cy="192"
                r="104"
                fill="#0c0c0e"
                stroke="#d66353"
                strokeWidth="2"
            />
            <path
                d="M0 460 110 359 183 409 268 317 360 415 460 340 570 408 700 337V520H0"
                fill="#141416"
            />
            <path d="m330 520 27-156 35-59 41 9 37 61 37 145Z" fill="#08090a" />
            <path
                d="m386 322-17-43 17-39 43 5 21 38-20 39-22-11Z"
                fill="#0a0a0c"
            />
            <path
                d="m377 252 6-29 17 12 14-18 7 22 22 1-5 24Z"
                fill="#08090a"
            />
            <path
                d="m366 342-82 165 134-59 108 72-45-166-38-33Z"
                fill="#0b0b0c"
                stroke="#574145"
            />
            <path d="m418 366 110-309 34 8-103 314Z" fill="#70676a" />
            <path d="m434 354 99-287 17 3-98 286Z" fill="#aaa19b" />
            <path d="m400 367 83 25 4-14-82-25Z" fill="#3e3939" />
            <path d="m397 281 15-3" stroke="#e76c53" strokeWidth="2" />
            <path
                d="m367 350-20 91m93-104 35 108"
                fill="none"
                stroke="#5a4549"
            />
            <path d="M0 485q130-60 266 8t434-12v39H0Z" fill="#08090a" />
            <rect width="700" height="520" fill="url(#rain)" />
        </svg>
    );
}

function ProductImage({ product, token }: { product: Product; token: string }) {
    const [src, setSrc] = useState("");
    const photoId = product.photos?.[0]?.id;
    useEffect(() => {
        if (!photoId) return;
        const controller = new AbortController();
        let objectUrl = "";
        fetch(`/api/products/${product.id}/photos/${photoId}/content`, {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
            signal: controller.signal,
        })
            .then((response) => {
                if (!response.ok) throw new Error();
                return response.blob();
            })
            .then((blob) => {
                if (!controller.signal.aborted) {
                    objectUrl = URL.createObjectURL(blob);
                    setSrc(objectUrl);
                }
            })
            .catch(() => {});
        return () => {
            controller.abort();
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [product.id, photoId, token]);
    return src ? (
        <img src={src} alt={product.name} />
    ) : (
        <div className="product-placeholder">
            <span className="rune">✦</span>
            <span>ANIMERCH ARCHIVE</span>
        </div>
    );
}

function App() {
    const [token, setToken] = useState("");
    const [loginOpen, setLoginOpen] = useState(false);
    const [authMode, setAuthMode] = useState<"login" | "register">("login");
    const [loginError, setLoginError] = useState("");
    const [signingIn, setSigningIn] = useState(false);
    const [authErrors, setAuthErrors] = useState<FieldErrors>({});
    const [data, setData] = useState<Page | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");
    const [query, setQuery] = useState("");
    const [sort, setSort] = useState("id,desc");
    const [filters, setFilters] = useState<CatalogFilterValues>(emptyFilters);
    const [appliedFilters, setAppliedFilters] = useState<CatalogFilterValues>(emptyFilters);
    const [filterError, setFilterError] = useState("");
    const [pageSize, setPageSize] = useState(12);
    const [page, setPage] = useState(0);
    const [retry, setRetry] = useState(0);
    const [selected, setSelected] = useState<Product | null>(null);
    const [basketOpen, setBasketOpen] = useState(false);
    const [checkoutRequested, setCheckoutRequested] = useState(false);
    const [checkoutOpen, setCheckoutOpen] = useState(false);
    const [pendingCheckout, setPendingCheckout] = useState(false);
    const [historyOpen, setHistoryOpen] = useState(false);
    const [historyRequested, setHistoryRequested] = useState(false);
    const [basket, setBasket] = useState<
        { product: Product; quantity: number }[]
    >([]);
    const [notice, setNotice] = useState("");
    const count = basket.reduce((sum, item) => sum + item.quantity, 0);
    const total = basket.reduce(
        (sum, item) => sum + item.product.price * item.quantity,
        0,
    );

    useEffect(() => {
        const controller = new AbortController();
        const params = productParams(appliedFilters, query, sort, page, pageSize);
        request<Page>("/products?" + params, "", controller.signal)
            .then(setData)
            .catch((err) => {
                if (!controller.signal.aborted)
                    setError(
                        err instanceof Error
                            ? err.message
                            : "Unable to reach the store.",
                    );
            })
            .finally(() => {
                if (!controller.signal.aborted) setLoading(false);
            });
        return () => controller.abort();
    }, [token, page, query, sort, retry, appliedFilters, pageSize]);

    useEffect(() => {
        if (!notice) return;
        const timer = window.setTimeout(() => setNotice(""), 3000);
        return () => window.clearTimeout(timer);
    }, [notice]);

    useEffect(() => {
        if (!loginOpen && !selected && !basketOpen && !checkoutOpen && !historyOpen) return;
        const previous = document.activeElement as HTMLElement | null;
        const dialog = document.querySelector<HTMLElement>('[role="dialog"]');
        const focusable = () =>
            Array.from(
                dialog?.querySelectorAll<HTMLElement>(
                    'button:not(:disabled), input, select, a[href], [tabindex="0"]',
                ) || [],
            );
        focusable()[0]?.focus();
        const key = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                if (checkoutOpen) return;
                setCheckoutOpen(false);
                setCheckoutRequested(false);
                setLoginOpen(false);
                setSelected(null);
                setBasketOpen(false);
                setHistoryOpen(false);
                setHistoryRequested(false);
            }
            if (event.key === "Tab") {
                const items = focusable();
                const first = items[0];
                const last = items[items.length - 1];
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last?.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first?.focus();
                }
            }
        };
        document.body.style.overflow = "hidden";
        document.addEventListener("keydown", key);
        return () => {
            document.body.style.overflow = "";
            document.removeEventListener("keydown", key);
            previous?.focus({ preventScroll: true });
        };
    }, [loginOpen, selected, basketOpen, checkoutOpen, pendingCheckout, historyOpen]);

    function beginLoad() {
        setLoading(true);
        setError("");
    }
    async function login(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (signingIn) return;
        setSigningIn(true);
        setLoginError("");
        const form = new FormData(event.currentTarget);
        const invalid = validateAccount(form, authMode === "register");
        setAuthErrors(invalid);
        if (Object.keys(invalid).length) {
            setSigningIn(false);
            event.currentTarget.querySelector<HTMLElement>(`[name="${Object.keys(invalid)[0]}"]`)?.focus();
            return;
        }
        const email = String(form.get("email") || "").trim().toLowerCase();
        const password = String(form.get("password") || "");
        if (authMode === "register" && password !== form.get("confirmPassword")) {
            setLoginError("Passwords do not match.");
            setSigningIn(false);
            return;
        }
        try {
            if (authMode === "register") {
                await request("/auth/register", "", undefined, {
                    name: String(form.get("name") || "").trim(),
                    phone: normalizedPhone(String(form.get("phone") || "").trim()),
                    email,
                    password,
                });
                setAuthMode("login");
                setNotice("Account created. Signing you in…");
            }
            const result = await request<{ accessToken: string }>(
                "/auth/login",
                "",
                undefined,
                { email, password },
            );
            beginLoad();
            setToken(result.accessToken);
            setRetry((value) => value + 1);
            setLoginOpen(false);
            setPage(0);
            setQuery("");
            setSearch("");
            if (checkoutRequested) {
                setCheckoutOpen(true);
                setCheckoutRequested(false);
                setNotice("Signed in. Complete your delivery details to place your order.");
            } else if (historyRequested) {
                setHistoryRequested(false);
                setHistoryOpen(true);
            } else {
                browse();
            }
        } catch (err) {
            setLoginError(
                err instanceof Error
                    ? err.message
                    : "Cannot connect to the backend.",
            );
        } finally {
            setSigningIn(false);
        }
    }
    function add(product: Product) {
        if (pendingCheckout) { setNotice("Resolve your pending checkout before changing the basket."); return; }
        const current =
            basket.find((item) => item.product.id === product.id)?.quantity ||
            0;
        if (current >= product.quantity) {
            setNotice("All available stock is already in your basket.");
            return;
        }
        setBasket((items) =>
            items.some((item) => item.product.id === product.id)
                ? items.map((item) =>
                      item.product.id === product.id
                          ? { ...item, quantity: item.quantity + 1 }
                          : item,
                  )
                : [...items, { product, quantity: 1 }],
        );
        setNotice(`${product.name} added to basket`);
    }
    function browse() {
        document
            .getElementById("collection")
            ?.scrollIntoView({ behavior: "smooth" });
    }
    function remove(productId: number) {
        if (pendingCheckout) return;
        setBasket((items) => items
            .map((item) => item.product.id === productId
                ? { ...item, quantity: item.quantity - 1 } : item)
            .filter((item) => item.quantity > 0));
    }
    function quantityControls(product: Product) {
        const quantity = basket.find((item) => item.product.id === product.id)?.quantity || 0;
        return <div className="quantity-controls card-quantity" aria-label={`Basket quantity for ${product.name}`}>
            <button aria-label={`Remove one ${product.name}`} disabled={pendingCheckout || quantity === 0} onClick={() => remove(product.id)}>−</button>
            <output key={quantity} aria-live="polite" aria-label={`Quantity of ${product.name}`}>{quantity}</output>
            <button aria-label={`Add ${product.name} to basket`} disabled={pendingCheckout || quantity >= product.quantity} onClick={() => add(product)}>+</button>
        </div>;
    }
    function close() {
        setCheckoutRequested(false);
        setLoginOpen(false);
        setSelected(null);
        setBasketOpen(false);
        setHistoryRequested(false);
    }
    function openHistory() {
        if (token) setHistoryOpen(true);
        else {
            setHistoryRequested(true); setAuthMode("login"); setLoginError("");
            setAuthErrors({}); setLoginOpen(true);
        }
    }
    function clearFilters() {
        setFilters(emptyFilters); setAppliedFilters(emptyFilters); setSearch(""); setQuery("");
        setPage(0); setFilterError(""); setSort("id,desc"); setPageSize(12);
        beginLoad(); setRetry(value => value + 1);
    }
    function selectSeller(sellerId: number) {
        const next = { ...appliedFilters, sellerId: String(sellerId) };
        setFilters(next); setAppliedFilters(next); setSelected(null); setPage(0);
        beginLoad(); setRetry(value => value + 1); browse();
    }

    return (
        <>
            <div className="announcement">
                FOR THOSE WHO KEEP STRUGGLING <span>✦</span> ANIME. ARMOR.
                IDENTITY.
            </div>
            <header className="header">
                <a className="logo" href="#">
                    ANI<span>MERCH</span>
                    <small>THE STRUGGLER'S ARCHIVE</small>
                </a>
                <nav aria-label="Main navigation">
                    <a href="#collection">Shop products</a>
                    <a href="#collection" onClick={() => document.getElementById("seller-filter")?.focus({ preventScroll: true })}>Sellers</a>
                    <a href="#manifesto">Our creed</a>
                </nav>
                <div className="header-actions">
                    <button className="text-button history-link" onClick={openHistory}>My orders ↗</button>
                    {!token && <button className="text-button" onClick={() => {
                        setAuthMode("register"); setLoginError(""); setLoginOpen(true);
                    }}>Sign up ↗</button>}
                    <button
                        className="text-button"
                        disabled={!!token && pendingCheckout}
                        onClick={() =>
                            token
                                ? (setToken(""), setNotice("Signed out. You can keep browsing with your basket."))
                                : (setAuthMode("login"), setLoginError(""), setLoginOpen(true))
                        }
                    >
                        {token ? "Sign out ↗" : "Sign in ↗"}
                    </button>
                    <button
                        className="basket-button"
                        onClick={() => pendingCheckout ? setCheckoutOpen(true) : setBasketOpen(true)}
                    >
                        {pendingCheckout ? "Resume order" : "Basket"} <span>{count.toString().padStart(2, "0")}</span>
                    </button>
                </div>
            </header>
            <main>
                <section className="hero">
                    <Artwork />
                    <div className="hero-copy">
                        <div className="eyebrow">
                            <span /> THE DARK FANTASY EDIT
                        </div>
                        <h1>
                            Wear your
                            <br />
                            own <em>destiny.</em>
                        </h1>
                        <p>
                            For the outcasts. The dreamers. The ones who never
                            yield.
                            <br />
                            Discover anime merchandise with a darker soul.
                        </p>
                        <button className="primary" onClick={browse}>
                            Shop products <span>↗</span>
                        </button>
                        <div className="hero-footnote">
                            INSPIRED BY THE WORLD OF BERSERK{" "}
                            <span>VOL. 01 — THE BLACK SWORDSMAN</span>
                        </div>
                    </div>
                    <span className="hero-side">抗え — KEEP STRUGGLING</span>
                </section>
                <div className="ticker">
                    <span>FORGED IN OBSESSION</span>
                    <b>✦</b>
                    <span>WORN WITH PURPOSE</span>
                    <b>✦</b>
                    <span>BEYOND THE ORDINARY</span>
                    <b>✦</b>
                    <span>FORGED IN OBSESSION</span>
                </div>
                <section id="collection" className="collection">
                    <div className="section-heading">
                        <div>
                            <div className="eyebrow">01 / THE PRODUCT FEED</div>
                            <h2>
                                Choose your armor<span>.</span>
                            </h2>
                        </div>
                        <p>Find something worth fighting for.</p>
                    </div>
                    <form
                        className="filters"
                        onSubmit={(event) => {
                            event.preventDefault();
                            const invalid = validateFilters(filters);
                            setFilterError(invalid);
                            if (invalid) return;
                            beginLoad();
                            setAppliedFilters({ ...filters });
                            setQuery(search.trim());
                            setPage(0);
                            setRetry((value) => value + 1);
                        }}
                    >
                        <label className="search">
                            <span aria-hidden="true">⌕</span>
                            <input
                                aria-label="Search products"
                                placeholder="Search the archive…"
                                value={search}
                                onChange={(event) =>
                                    setSearch(event.target.value)
                                }
                            />
                            <button type="submit">Search ↗</button>
                        </label>
                        <label className="sort">
                            Sort by{" "}
                            <select
                                value={sort}
                                onChange={(event) => {
                                    beginLoad();
                                    setSort(event.target.value);
                                    setPage(0);
                                }}
                            >
                                <option value="id,desc">Latest arrivals</option>
                                <option value="price,asc">
                                    Price: low to high
                                </option>
                                <option value="price,desc">
                                    Price: high to low
                                </option>
                                <option value="name,asc">Name: A–Z</option>
                                <option value="name,desc">Name: Z–A</option>
                                <option value="quantity,desc">Stock: high to low</option>
                            </select>
                        </label>
                        <CatalogFilters value={filters} onChange={value => { setFilters(value); setFilterError(""); }} onClear={clearFilters} error={filterError} pageSize={pageSize} onPageSize={size => { if (size !== pageSize) { beginLoad(); setPageSize(size); setPage(0); } }} />
                    </form>
                    {error ? (
                        <div className="state-panel" role="alert">
                            <h3>Our connection was interrupted.</h3>
                            <p>{error}</p>
                            <button
                                className="primary"
                                onClick={() =>
                                    error.includes("session")
                                        ? setLoginOpen(true)
                                        : (beginLoad(),
                                          setRetry((value) => value + 1))
                                }
                            >
                                {error.includes("session")
                                    ? "Sign in again"
                                    : "Try again"}{" "}
                                ↗
                            </button>
                        </div>
                    ) : loading ? (
                        <div
                            className="product-grid"
                            aria-label="Loading products"
                            aria-busy="true"
                        >
                            {[1, 2, 3, 4].map((value) => (
                                <div className="skeleton" key={value}>
                                    <div />
                                    <p />
                                    <p />
                                </div>
                            ))}
                        </div>
                    ) : data?.content.length ? (
                        <>
                            <div className="result-count">
                                {data.totalElements} pieces in the archive
                                {query && ` · Results for “${query}”`}
                            </div>
                            <div className="product-grid">
                                {data.content.map((product, index) => (
                                    <article
                                        className="product-card"
                                        key={product.id}
                                    >
                                        <button
                                            className="product-visual"
                                            onClick={() => setSelected(product)}
                                            aria-label={`View ${product.name}`}
                                        >
                                            <ProductImage
                                                product={product}
                                                token={token}
                                            />
                                            <span className="product-index">
                                                ARCHIVE /{" "}
                                                {String(
                                                    index + 1 + page * pageSize,
                                                ).padStart(3, "0")}
                                            </span>
                                            <span
                                                className={`stock ${product.quantity < 1 ? "sold-out" : ""}`}
                                            >
                                                {product.quantity > 0
                                                    ? "IN STOCK"
                                                    : "SOLD OUT"}
                                            </span>
                                            <span className="view-product">
                                                View piece ↗
                                            </span>
                                        </button>
                                        <div className="product-meta">
                                            {product.categories
                                                ?.map(
                                                    (category) => category.name,
                                                )
                                                .join(" / ") ||
                                                "THE COLLECTION"}
                                        </div>
                                        {product.seller && <button className="seller-link" onClick={() => product.seller && selectSeller(product.seller.id)}>By {product.seller.name} ↗</button>}
                                        <button
                                            className="product-name"
                                            onClick={() => setSelected(product)}
                                        >
                                            {product.name}
                                        </button>
                                        <div className="rating">
                                            {product.averageRating != null
                                                ? `★ ${product.averageRating.toFixed(1)} · ${product.reviewCount} reviews`
                                                : "No reviews yet"}
                                        </div>
                                        <div className="product-bottom">
                                            <strong>
                                                {money(product.price)}
                                            </strong>
                                            {quantityControls(product)}
                                        </div>
                                    </article>
                                ))}
                            </div>
                            <div className="pagination">
                                <button
                                    disabled={page === 0}
                                    onClick={() => {
                                        beginLoad();
                                        setPage((value) => value - 1);
                                    }}
                                >
                                    ← Previous
                                </button>
                                <span>
                                    Page {page + 1} of {data.totalPages}
                                </span>
                                <button
                                    disabled={page + 1 >= data.totalPages}
                                    onClick={() => {
                                        beginLoad();
                                        setPage((value) => value + 1);
                                    }}
                                >
                                    Next →
                                </button>
                            </div>
                        </>
                    ) : (
                        <div className="state-panel">
                            <h3>No pieces found.</h3>
                            <p>
                                {query || Object.values(appliedFilters).some(Boolean)
                                    ? "Try a different search to explore the archive."
                                    : "New arrivals will appear here when products are added to the store."}
                            </p>
                            {(query || Object.values(appliedFilters).some(Boolean)) && (
                                <button
                                    className="text-button"
                                    onClick={clearFilters}
                                >
                                    Clear all filters ↗
                                </button>
                            )}
                        </div>
                    )}
                </section>
                <section id="manifesto" className="manifesto">
                    <span className="manifesto-mark">✦</span>
                    <div>
                        <div className="eyebrow">MORE THAN MERCHANDISE</div>
                        <h2>
                            Some stories stay with you.
                            <br />
                            <em>Wear the ones that do.</em>
                        </h2>
                        <p>
                            Animerch is a home for the worlds we carry with us.
                            From dark fantasy to the stories that made us dream,
                            find a piece of your own.
                        </p>
                    </div>
                    <span className="vertical-text">
                        THE STRUGGLE CONTINUES
                    </span>
                </section>
            </main>
            <footer>
                <a className="logo" href="#">
                    ANI<span>MERCH</span>
                </a>
                <span>Anime lives beyond the screen.</span>
                <small>
                    © {new Date().getFullYear()} Animerch · Independent fan
                    storefront
                </small>
            </footer>
            {notice && !checkoutOpen && (
                <div className="toast" role="status">
                    ✦ {notice}
                </div>
            )}
            {(loginOpen || selected || basketOpen) && (
                <div className="overlay" onClick={close}>
                    <section
                        className={`modal ${selected ? "detail-modal" : ""}`}
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="dialog-title"
                        onClick={(event) => event.stopPropagation()}
                    >
                        <button
                            className="close"
                            aria-label="Close dialog"
                            onClick={close}
                        >
                            ×
                        </button>
                        {loginOpen ? (
                            <>
                                <div className="eyebrow">
                                    WELCOME BACK, STRUGGLER
                                </div>
                                <h2 id="dialog-title">
                                    {authMode === "register" ? "Join the archive" : "Enter the archive"}<span>.</span>
                                </h2>
                                <p>{authMode === "register" ? "Create your customer account to explore the collection." : "Sign in with your Animerch account."}</p>
                                <div className="auth-tabs" aria-label="Account options">
                                    <button className={authMode === "login" ? "active" : ""} disabled={signingIn} onClick={() => { setAuthMode("login"); setLoginError(""); setAuthErrors({}); }}>Sign in</button>
                                    <button className={authMode === "register" ? "active" : ""} disabled={signingIn} onClick={() => { setAuthMode("register"); setLoginError(""); setAuthErrors({}); }}>Create account</button>
                                </div>
                                <form className="login-form" onSubmit={login} key={authMode} noValidate onChange={() => setAuthErrors({})}>
                                    {Object.keys(authErrors).length > 0 && <div className="validation-summary" role="alert"><strong>Please check these fields:</strong><ul>{Object.entries(authErrors).map(([field, message]) => <li key={field}><button type="button" onClick={() => document.querySelector<HTMLElement>(`.login-form [name="${field}"]`)?.focus()}>{field === "confirmPassword" ? "Confirm password" : field}: {message}</button></li>)}</ul></div>}
                                    {authMode === "register" && <>
                                        <label>Name<input name="name" autoComplete="name" required maxLength={100} placeholder="Your name" aria-invalid={!!authErrors.name}/></label>
                                        <label>Phone<input name="phone" type="tel" autoComplete="tel" required maxLength={30} placeholder="+998 90 123 45 67" aria-invalid={!!authErrors.phone}/><small className="muted">7–15 digits. Spaces, parentheses and hyphens are accepted.</small></label>
                                    </>}
                                    <label>
                                        Email
                                        <input
                                            name="email"
                                            aria-invalid={!!authErrors.email}
                                            type="email"
                                            autoComplete="username"
                                            maxLength={254}
                                            required
                                            placeholder="you@example.com"
                                        />
                                    </label>
                                    <label>
                                        Password
                                        <input
                                            name="password"
                                            aria-invalid={!!authErrors.password}
                                            type="password"
                                            autoComplete={authMode === "register" ? "new-password" : "current-password"}
                                            minLength={authMode === "register" ? 8 : undefined}
                                            maxLength={authMode === "register" ? 72 : undefined}
                                            required
                                            placeholder="Your password"
                                        />
                                    </label>
                                    {loginError && (
                                        <p className="form-error" role="alert">
                                            {loginError}
                                        </p>
                                    )}
                                    {authMode === "register" && <label>Confirm password<input name="confirmPassword" type="password" autoComplete="new-password" required minLength={8} maxLength={72} placeholder="Repeat your password" /></label>}
                                    <button
                                        className="primary"
                                        disabled={signingIn}
                                    >
                                        {signingIn
                                            ? "Please wait…"
                                            : authMode === "register" ? "Create account ↗" : "Sign in ↗"}
                                    </button>
                                </form>
                                <small className="muted">
                                    Your session stays in this tab and ends when
                                    you reload.
                                </small>
                            </>
                        ) : selected ? (
                            <>
                                <div className="detail-image">
                                    <ProductImage
                                        product={selected}
                                        token={token}
                                    />
                                </div>
                                <div className="eyebrow">
                                    THE ANIMERCH ARCHIVE
                                </div>
                                <h2 id="dialog-title">{selected.name}</h2>
                                {selected.seller && <button className="seller-link" onClick={() => selected.seller && selectSeller(selected.seller.id)}>Shop {selected.seller.name} ↗</button>}
                                <p>
                                    {selected.description ||
                                        "A piece for your collection. Let your favorite stories become part of your everyday."}
                                </p>
                                <p className="rating">
                                    {selected.averageRating != null
                                        ? `★ ${selected.averageRating.toFixed(1)} · ${selected.reviewCount} reviews`
                                        : "No reviews yet"}
                                </p>
                                <strong className="detail-price">
                                    {money(selected.price)}
                                </strong>
                                <p className="muted">
                                    {selected.quantity > 0
                                        ? `${selected.quantity} available`
                                        : "Currently out of stock"}
                                </p>
                                <div className="detail-quantity">In your basket {quantityControls(selected)}</div>
                                <button
                                    className="primary"
                                    disabled={selected.quantity < 1}
                                    onClick={() => add(selected)}
                                >
                                    Add to basket +
                                </button>
                            </>
                        ) : (
                            <>
                                <div className="eyebrow">YOUR FINDS</div>
                                <h2 id="dialog-title">
                                    The basket<span>.</span>
                                </h2>
                                <p className="muted">
                                    Your basket is saved in this tab. Checkout requires
                                    a customer account.
                                </p>
                                {basket.length ? (
                                    <>
                                        <div className="basket-items">
                                            {basket.map((item) => (
                                                <div
                                                    className="basket-item"
                                                    key={item.product.id}
                                                >
                                                    <div>
                                                        <strong>
                                                            {item.product.name}
                                                        </strong>
                                                        <p>
                                                            {money(
                                                                item.product
                                                                    .price,
                                                            )}{" "}
                                                            × {item.quantity}
                                                        </p>
                                                    </div>
                                                    <div className="quantity-controls">
                                                        <button
                                                            aria-label={`Remove one ${item.product.name}`}
                                                            onClick={() =>
                                                                setBasket(
                                                                    (items) =>
                                                                        items
                                                                            .map(
                                                                                (
                                                                                    entry,
                                                                                ) =>
                                                                                    entry
                                                                                        .product
                                                                                        .id ===
                                                                                    item
                                                                                        .product
                                                                                        .id
                                                                                        ? {
                                                                                              ...entry,
                                                                                              quantity:
                                                                                                  entry.quantity -
                                                                                                  1,
                                                                                          }
                                                                                        : entry,
                                                                            )
                                                                            .filter(
                                                                                (
                                                                                    entry,
                                                                                ) =>
                                                                                    entry.quantity >
                                                                                    0,
                                                                            ),
                                                                )
                                                            }
                                                        >
                                                            −
                                                        </button>
                                                        <span>
                                                            {item.quantity}
                                                        </span>
                                                        <button
                                                            aria-label={`Add one ${item.product.name}`}
                                                            disabled={
                                                                item.quantity >=
                                                                item.product
                                                                    .quantity
                                                            }
                                                            onClick={() =>
                                                                add(
                                                                    item.product,
                                                                )
                                                            }
                                                        >
                                                            +
                                                        </button>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                        <div className="basket-total">
                                            <span>Total</span>
                                            <strong>{money(total)}</strong>
                                        </div>
                                        {!token ? <button className="primary checkout-button" onClick={() => {
                                            setCheckoutRequested(true);
                                            setBasketOpen(false);
                                            setAuthMode("login");
                                            setLoginError("");
                                            setLoginOpen(true);
                                        }}>Sign in or sign up to checkout ↗</button> : <button className="primary checkout-button" onClick={() => { setBasketOpen(false); setCheckoutOpen(true); }}>Checkout ↗</button>}
                                        <button
                                            className="text-button"
                                            disabled={pendingCheckout}
                                            onClick={() => setBasket([])}
                                        >
                                            Clear basket
                                        </button>
                                    </>
                                ) : (
                                    <div className="empty-basket">
                                        <span>✦</span>
                                        <p>
                                            Your next favorite piece is waiting.
                                        </p>
                                        <button
                                            className="primary"
                                            onClick={() => {
                                                setBasketOpen(false);
                                                browse();
                                            }}
                                        >
                                            Explore the collection ↗
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </section>
                </div>
            )}
            <Checkout open={checkoutOpen} token={token} lines={basket} onLock={setPendingCheckout}
                onClose={() => setCheckoutOpen(false)}
                onAuth={() => { setCheckoutOpen(false); setBasketOpen(false); setCheckoutRequested(true); setAuthMode("login"); setLoginError(""); setAuthErrors({}); setLoginOpen(true); }}
                onComplete={() => { setBasket([]); beginLoad(); setRetry(value => value + 1); }}
                onHistory={() => { setCheckoutOpen(false); openHistory(); }} />
            {historyOpen && <OrderHistory token={token} revision={retry} onClose={() => setHistoryOpen(false)} onAuth={() => { setHistoryOpen(false); setHistoryRequested(true); setAuthMode("login"); setLoginError(""); setAuthErrors({}); setLoginOpen(true); }} />}
        </>
    );
}

export default App;
