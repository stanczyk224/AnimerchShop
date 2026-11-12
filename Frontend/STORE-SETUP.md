# Animerch storefront

Run the existing backend with Docker Compose from the repository root. Then, in
this folder, run `npm install` (first time only) and `npm run dev`. Open the local
URL printed by Vite. Products load immediately, with no account needed.

Catalog listing, product details, product images and category reads are public.
Account data, cart APIs, checkout and catalog writes still require authentication.
The frontend uses the returned access token in memory; reloading requires signing in again.
No admin credentials or JWT signing secrets belong in frontend environment files.

Vite forwards `/api` requests to `http://localhost:8080` in development and preview.
Search, sorting, pagination, pricing, stock, rating summaries and product images
come from the real API. Products without images show a decorative placeholder.
If the backend is unavailable, the collection shows an error with a retry action.

The browsing basket is local, cleared on reload and preserved on sign-in or sign-out.
It does not reserve stock. At checkout, the customer explicitly confirms replacing
their server cart with this basket. The frontend reads and sets absolute quantities,
checks the prepared cart, and submits the delivery snapshot to `/api/order/checkout`.
The store returns an order confirmation with its ID and server-calculated total.
No payment gateway is connected and no online payment is collected.
Administration remains a future feature. Use Sign up (or Create
account in the sign-in dialog) to register a customer with name, phone, email and
password. Successful registration signs in automatically and opens the product
feed. Existing customers can use Sign in; Shop products opens the feed directly.
Each product card and detail view shows its current basket quantity with minus
and plus controls. Guest checkout opens sign-in/sign-up and preserves the basket.
After authentication the delivery form opens. Checkout requires a customer profile;
an administrator account without one cannot place an order.

Each purchase uses a UUID idempotency key. Pending requests keep the key, address,
account identity and basket snapshot in this tab's session storage. Retries submit
that same request without preparing or incrementing the server cart again. A pending
order locks basket editing until its result is known. After a reload, use Resume
order and sign in with the original account. Confirmed success clears the pending
snapshot. Delivery data remains in session storage only while an order is pending.

Registration, sign-in and delivery forms validate input before calling the API.
Backend constraints also reject malformed names, phones, addresses and postal codes.
Phone numbers accept 7–15 digits with an optional + and standard formatting, but
this checks format only, not phone ownership. Passwords require at least 8 characters
for registration and at most 72 UTF-8 bytes to match BCrypt's supported input limit.
Motion respects the browser's reduced-motion preference.

Use My orders in the header (or View my orders after checkout) for paginated
customer order history. Item statuses, original/cancelled/active totals and saved
delivery details come from the backend. History requires signing in and displays
only the current customer's orders. Older orders may have no saved delivery address.

Catalog controls combine text search, seller, category, minimum/maximum price,
sorting and page sizes of 6, 12 or 20. Filters are applied on the server before
pagination. Use Apply filters/Search to apply a draft selection and Clear all to
reset. Seller names on product cards and details link to that seller's products.
Seller and category options load publicly; all seller pages are loaded using the
backend's 20-item limit. Seller writes remain admin-only.

`npm run build` checks TypeScript and builds to `dist/`. `npm run preview` serves
that build locally with the same backend proxy. For deployment, serve `dist/`
and configure the hosting server to forward `/api` to the backend; Vite's proxy
is not part of the built static files.

`npm test` (Node 24) runs validation and cart-preparation regression tests.

Browser QA is available in `scripts/check-checkout-browser.mjs`. It creates a
customer and places an order, so run it only against a disposable database with
an isolated backend on 8081, frontend on 5174 (`ANIMERCH_API_TARGET` points Vite at
that backend), and a separate headless Chrome profile on debugging port 9229.
It tests guest checkout, signup errors, delivery errors and a deliberately lost
checkout success response, then verifies that retrying creates only one order.

The design uses CSS and an inline SVG eclipse illustration. Google Fonts are
optional: system fallback fonts apply if the font service is unavailable.
