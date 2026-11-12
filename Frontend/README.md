# Animerch storefront

React 19 + TypeScript + Vite frontend with a Berserk-inspired dark fantasy design.
Built with AI assistance while learning React and TypeScript.

Start the backend and PostgreSQL from the repository root:

```bash
docker compose up --build -d
```

Then run the frontend in another terminal:

```bash
cd Frontend
npm ci
npm run dev
```

Use Node.js 24 (the version used for tests). Open the local URL printed by Vite,
normally http://localhost:5173. API requests are proxied to http://localhost:8080.

The storefront supports public browsing, seller/category/price filters, a guest
basket, registration/login, validated delivery checkout, order confirmation and
customer order history. Checkout records an order and does not collect payment.

```bash
npm test
npm run build
npm run preview
```

Build output goes to `dist/`. Production hosting must supply an `/api` proxy;
the development proxy is not included in the generated static files.

See [STORE-SETUP.md](STORE-SETUP.md) for session behavior, checkout retries,
validation rules, browser QA and current limitations. See the [project README](../README.md)
for backend setup, API documentation, database requirements and CI.
