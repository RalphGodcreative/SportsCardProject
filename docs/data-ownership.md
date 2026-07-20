# Data Ownership

## Strategy
Each user owns their own data. Ownership is enforced by a `user_id` foreign key on root and directly-queried entities. Child entities derive ownership through their parent relationship.

## Entity Ownership Map

| Entity | Has `user_id` | Reason |
|---|---|---|
| `Card` | Yes | Root entity, has its own listing page (`/card`) |
| `Transaction` | Yes | Has its own listing page (`/transactions`) |
| `SearchKeyword` | Yes | Root entity, has its own listing page (`/crawler`) |
| `Storage` | Yes | Root entity, has its own listing page (`/storage`) — planned, see `impl-storages.md` |
| `TransactionInfo` | No | Only accessed as a detail of `Transaction` |
| `SaleWithCard` | No | Only accessed as a detail of `Card` |
| `SearchProduct` | No | Only accessed as a result of `SearchKeyword` |
| `YoutubeVideo` | No | Planned for removal |

## Access Rule
Every query on a `user_id` entity must filter by the currently logged-in user. No user should ever see or modify another user's data.

Example:
```java
// Always scope queries to the current user
cardRepository.findByUserId(currentUserId);
transactionRepository.findByUserId(currentUserId);
```

## Migration Strategy
Since there is existing data that belongs to the admin (first user), the Flyway migration must:
1. Create the `users` table
2. Insert the admin user (seeded with a known ID, e.g. `id = 1`)
3. Add `user_id` column to `card`, `transaction`, `search_keyword` — set all existing rows to the admin user's ID as default

```sql
-- Example migration step
ALTER TABLE card ADD COLUMN user_id BIGINT;
UPDATE card SET user_id = 1;
ALTER TABLE card ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE card ADD CONSTRAINT fk_card_user FOREIGN KEY (user_id) REFERENCES users(id);
```

## Registration Strategy
- No self-registration — admin creates user accounts manually or via a seeded SQL script.
- First user registered becomes `ADMIN` and owns all existing data.
- See [`authentication.md`](authentication.md) for full auth plan.
- See [`user-roles.md`](user-roles.md) for role definitions.
