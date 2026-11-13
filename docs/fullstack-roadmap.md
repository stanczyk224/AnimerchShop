# Animerch: finish it yourself roadmap

Last reviewed: 2026-11-12.

## You can already call this a fullstack app

Your React interface talks to a Spring Boot API, which stores real data in
PostgreSQL. Customers can browse, register, sign in, place orders and view their
history. Docker Compose starts all three layers. That is already fullstack.

What remains is completing the customer/admin experience, strengthening your own
understanding, and preparing the app for deployment. A complete portfolio demo and
a shop that takes real payments are different targets. Payments and every feature
below are not requirements for using the word "fullstack."

This is your learning backlog, not a request for an assistant to implement it.
Work on one milestone at a time. Ask for explanations, hints and reviews before
asking for complete code.

## Existing foundation — do not rebuild it

- [x] React + TypeScript frontend with a responsive Berserk-inspired design.
- [x] Public products, product images, seller selection and category/price filters.
- [x] Server-side searching, sorting and pagination.
- [x] Guest basket with visible quantities and totals.
- [x] Customer registration and sign-in.
- [x] Client/server validation, including phone formats and password limits.
- [x] Delivery checkout and a confirmation using the real backend response.
- [x] Idempotency keys and recovery after a lost response or reload.
- [x] Private order history with statuses, delivery snapshots and totals.
- [x] PostgreSQL migrations, backend regression tests and backend CI.
- [x] One-command Docker Compose startup, including the frontend API proxy.

Existing limits to remember:

- Login tokens and the ordinary browsing basket are in memory and reset on reload.
- Pending checkout recovery is stored separately in tab session storage.
- Checkout currently asks permission to replace the saved account cart with the
  basket being reviewed.
- Checkout creates an order; it does not collect money.
- An admin account without a customer profile cannot place customer orders.
- A visible rating summary is not yet a complete reviews interface.
- Some features have APIs but no frontend, while others still need new API contracts.

## How to work through a milestone

1. Read the relevant controller, request/response types and frontend component.
2. Write down the user action, request, response and visible result in your own words.
3. Build the smallest useful version, then add loading, empty and failure states.
4. Check with two customer accounts and a guest when access control is involved.
5. Test the important behavior, update documentation and make one focused commit.

Keep prices, permissions and stock decisions authoritative on the backend. Do not
add a second implementation of those business rules in React.

## 1. Understand and simplify your current frontend

**Purpose:** make the code yours before growing it further.

- [ ] Explain how product loading, filters, basket state and login state work.
- [ ] Trace a checkout from the button through cart preparation to the order response.
- [ ] Explain why a timed-out purchase must retain its original key and address.
- [ ] Extract the header, product card and product-details view from `App.tsx`.
- [ ] Move shared product/order types and repeated price formatting into shared modules.
- [ ] Keep state in the component that owns it; derive totals from basket items.

**Start here:** [App.tsx](../Frontend/src/App.tsx),
[Checkout.tsx](../Frontend/src/Checkout.tsx),
[checkoutService.ts](../Frontend/src/checkoutService.ts),
[api.ts](../Frontend/src/api.ts).

**Done when:** existing behavior still works, and you can explain each extracted
component without reading generated code line by line. Avoid a complete rewrite.

## 2. Add the wishlist interface — a good first independent feature

**Backend exists:** `WishlistController` and `WishlistService`.

- [ ] Put a save/remove action on product cards and product details.
- [ ] Add a paginated My wishlist view.
- [ ] Prompt guests to sign in before saving; preserve what they intended to save.
- [ ] Show loading, empty, duplicate-save and failure states.
- [ ] Make sure saving an out-of-stock product works and does not reserve stock.

Use these existing endpoints:

| Action | Endpoint |
| --- | --- |
| Save | `POST /api/wishlist/items/{productId}` |
| List yours | `GET /api/wishlist?page=0&size=20` |
| Remove | `DELETE /api/wishlist/items/{productId}` |

**Done when:** saving survives reload after signing in again, removing updates the
view, and another customer sees only their own wishlist.

## 3. Let customers cancel placed order items

**Backend exists:** `OrderController` and `OrderService`.

- [ ] Add Cancel beside a `PLACED` item in My orders.
- [ ] Ask for confirmation and disable repeated clicks while the request runs.
- [ ] Call `PATCH /api/order/{orderId}/items/{itemId}/cancel`.
- [ ] Reload the order and catalog stock after success.
- [ ] Show the server's error if shipping or another cancellation won the race.
- [ ] Display original, cancelled and active totals correctly.

**Done when:** placed items can be cancelled, shipped/delivered items cannot, and
stock returns once. The original order total must remain historical purchase data.

## 4. Define the signed-in customer's identity

**API work is needed:** there is no dedicated current-user/profile endpoint yet.
The JWT currently contains an email subject, not a frontend role/profile contract.

- [ ] Design an authenticated current-user response with a stable user ID, display
  name, role and whether the account has a customer profile.
- [ ] Add and test that endpoint before using it to build role-aware navigation.
- [ ] Store current-user state in one clear place in the frontend.
- [ ] Handle expired sessions consistently and preserve the intended action on login.
- [ ] Decide whether editable customer profile fields are in scope; add owner-only
  APIs if so. The existing consumer-management endpoints are admin-only.

**Done when:** customer/admin navigation comes from server-confirmed identity,
not a hardcoded admin email. Backend authorization must still enforce every operation.

## 5. Add the complete reviews experience

**Backend largely exists:** `ReviewController` and `ReviewService`.

- [ ] Show paginated reviews on product details.
- [ ] Add a rating/comment form and field validation.
- [ ] Handle the delivered-purchase requirement and duplicate-review response.
- [ ] Let authors edit/delete their own reviews, with confirmation for deletion.
- [ ] Show edited status and refresh rating summaries after changes.

API base: `/api/products/{productId}/reviews`.
Use `GET`/`POST` on the base, and `PUT`/`DELETE` on `/{reviewId}`.

**Contract gap:** `ReviewDTO` currently has a display name, but no stable author ID
or `isMine` flag. Extend the contract deliberately so the UI can show ownership.
Never compare display names to decide who owns a review. Review reads currently
require authentication; either design that UI or separately change/test the policy.

**Done when:** a delivered customer can review, an ineligible customer gets a useful
message, and editing/deleting somebody else's review is rejected by the server.

## 6. Build an admin order-management screen

**Backend exists:** admin order listing/details and item status transitions.

- [ ] Add an admin-only navigation area using milestone 4's identity response.
- [ ] List orders using `GET /api/order?page=0&size=20`.
- [ ] Show items, delivery details and totals.
- [ ] Use `PATCH /api/order/{orderId}/items/{itemId}/status` for supported transitions.
- [ ] Refresh the server state after every update and handle rejected transitions.
- [ ] Verify customer and guest requests remain forbidden even if they bypass the UI.

**Done when:** an admin can move an item through the supported lifecycle and the
customer sees the updated status. You can then test delivered-purchase reviews
without manually changing database records.

## 7. Build catalog administration

**Many APIs exist:** products, sellers, categories, photos and CSV imports.

- [ ] List/create/edit products, with validated prices, stock, categories and seller.
- [ ] Upload/delete product images and show validation/upload failures.
- [ ] List/create/delete sellers and categories using the existing endpoints.
- [ ] Add CSV import screens only after the ordinary forms work.
- [ ] Display import results and row errors instead of reporting unconditional success.
- [ ] Require confirmation before deletion and explain related-data conflicts.

Check the actual controller before promising an edit feature: seller/category edit
endpoints are not currently exposed. Add those contracts and tests if you want them.
Seller-photo endpoints are currently admin-only, including reads; public seller
portraits would need a separate authorization decision and tests.

**Done when:** catalog maintenance can be performed through the UI without Swagger
or SQL, and server permissions remain enforced.

## 8. Improve navigation, basket persistence and account sessions

These improve usability; they are not prerequisites for being fullstack.

- [ ] Give products and order views shareable URLs and working back/forward behavior.
- [ ] Put applied search filters in the URL and restore them on reload.
- [ ] Preserve the guest basket across reloads using product IDs/quantities; reload
  current product data instead of trusting stored prices or stock.
- [ ] Design an explicit merge-or-replace experience for guest and saved account carts.
- [ ] Keep pending-checkout recovery separate from ordinary cart editing.
- [ ] Decide whether persistent login is needed. If it is, design session/refresh-token
  expiry, logout and revocation together on both sides rather than just persisting
  the current bearer token indefinitely.

**Done when:** reload/navigation behavior is intentional, cart changes are predictable,
and the lost-response checkout recovery test still produces exactly one order.

## 9. Bring frontend and fullstack checks into CI

Backend verification already runs in GitHub Actions. The frontend Docker build
runs its tests and build, but the GitHub workflow does not yet check the frontend.

- [ ] Add a Node.js job that runs `npm ci`, `npm test` and `npm run build` in `Frontend`.
- [ ] Adapt browser checks to run repeatably in CI against disposable PostgreSQL.
- [ ] Cover guest browsing, combined filters, registration, checkout, order history,
  ownership boundaries and a lost checkout response.
- [ ] Add tests for the features you build in milestones 2–7.
- [ ] Verify `docker compose up --build` starts a healthy frontend, API and database.
- [ ] Retain useful failed-test logs/screenshots without exposing account secrets.

**Done when:** a broken frontend build or failed customer workflow fails the PR checks,
and the required database tests cannot silently skip.

## 10. Deploy a portfolio version

- [ ] Deploy the three services and verify the public storefront uses the real API.
- [ ] Configure a domain and HTTPS, plus deliberate public access to the API/Swagger.
- [ ] Supply deployment secrets outside source control and frontend bundles.
- [ ] Persist database/uploads and test backup restoration, not just backup creation.
- [ ] Verify health checks, logs, restart behavior and the database migration process.
- [ ] Test mobile layouts, keyboard use, form errors and slow/interrupted requests.
- [ ] Prepare sample catalog data and screenshots; label the site as a demo if applicable.
- [ ] Update the README with setup, architecture, limitations and the deployed URL.

The ignored local certificate override used on this machine is not a deployment
dependency. Configure certificate trust for the deployment environment only if needed.

**Done when:** somebody else can use the demo without your laptop or manual setup,
and you can restore its data and explain how it is operated.

## Optional: before accepting real money

These are separate product requirements, not a checklist for the term "fullstack."

- [ ] Integrate a payment provider in its test environment, with server-side payment
  state and verified, idempotent webhook handling. A browser redirect is not proof of payment.
- [ ] Define how payment failures, stock, shipping, cancellations and refunds interact.
- [ ] Add account recovery/email verification and abuse controls appropriate to the service.
- [ ] Define shipping costs, delivery coverage, currency and any tax requirements.
- [ ] Add customer support/contact information and the applicable store policies.
- [ ] Test payment failure, retry, duplicate webhook and refund scenarios before launch.

Do not accept card details directly in this application as a learning shortcut.

## A useful definition of finished

For a solid portfolio demo, aim to demonstrate these journeys yourself:

1. Guest finds products through search, seller/category filters and pricing.
2. Customer registers, buys, sees order history and can cancel an eligible item.
3. Admin manages a product and advances an order to delivery.
4. The delivered customer leaves a review and another customer cannot edit it.
5. Wishlist persists, private data stays private, and checkout retries do not duplicate orders.
6. A fresh checkout of the repository can be started with documented commands and checked in CI.

The most important learning outcome: you can explain the request, state changes,
database effects, validation and failure handling for each journey.

## Your next exercise

Start with **milestone 2: wishlist** after reading the current components.

First implement only a save button that calls the existing API for a signed-in
customer, shows progress, and handles failure. Then add the wishlist list and
removal. Do not try to finish the whole roadmap in one session.
