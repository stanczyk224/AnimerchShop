# Project walkthrough

Requested format: teach the completed work one lesson per conversation turn, then
assign practical tasks for this project after the walkthrough. Do not deliver the
entire course in one message or implement the learner's exercises automatically.

Lesson order:

1. Customer cancellation: controller → service → repository, identity and ownership.
2. Transactions and row locks: safe stock restoration and competing requests.
3. Order totals: saved unit prices, BigDecimal, derived totals, MapStruct DTOs.
4. Reading orders: ownership, admin access, pagination and stable sorting.
5. Checkout retries: customer-scoped idempotency keys, unique constraints, rollback.
6. Delivery snapshots: request validation, embedded values, immutable history,
   conflict detection when a retry changes delivery details.
7. Flyway: V19/V20, preserving old migrations, handling legacy orders.
8. Testing: service/controller tests, real PostgreSQL tests, concurrency tests.
9. CI: workflow triggers, database service, test environment, Maven verify,
   report guard, summaries and downloadable artifacts.
10. Review and learner exercises, assigned progressively with feedback.

Start with lesson 1 after CI implementation is complete. Progress has not yet been
confirmed by the learner. Keep explanations tied to the actual project files.
