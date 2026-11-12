# Animerch storefront

React 19 + TypeScript + Vite frontend with a Berserk-inspired dark fantasy design.
Built with AI assistance while learning React and TypeScript.

Start the entire application from the repository root:

```bash
docker compose up
```

Open http://localhost:3000. Docker builds/tests the frontend and serves it with
Nginx; no local Node installation is required. After source changes, use
`docker compose up --build`. The storefront port is configurable with `FRONTEND_PORT`.

For optional frontend development with hot reload, keep the backend running and use:

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

Build output goes to `dist/`. The Docker image supplies an Nginx `/api` proxy to
the backend. Other static hosting must configure its own proxy; Vite's development
proxy is not included in the generated static files.

See [STORE-SETUP.md](STORE-SETUP.md) for session behavior, checkout retries,
validation rules, browser QA and current limitations. See the [project README](../README.md)
for backend setup, API documentation, database requirements and CI.
