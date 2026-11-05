# Database upgrades

Flyway migrations V1–V14 are unchanged so existing installations retain valid checksums.
V15 adds database constraints without deleting or merging customer data. If existing
carts or cart items contain duplicates, resolve them explicitly before applying V15.
Its product checks apply to new and updated rows; review legacy invalid prices and
quantities before validating those constraints.

For an installation still on V7 or earlier with existing consumers, V8 needs a planned
data backfill before its NOT NULL statement can succeed. Each consumer needs a real,
unique app_user account and a linked user_id. Do not generate shared passwords or
delete consumers to bypass the migration. A later migration cannot repair an earlier
failed migration. Back up and rehearse that upgrade on a copy of the database first.

The database context test runs when DB_URL is set; the Docker build runs the standalone
regression tests without requiring a database. Use the dev Spring profile to enable
SQL and application debug logging locally.
