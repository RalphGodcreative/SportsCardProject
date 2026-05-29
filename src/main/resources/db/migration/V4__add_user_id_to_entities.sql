-- Run AFTER the first admin user has been registered (they will get id = 1).
-- Backfills all existing rows to that admin user.

ALTER TABLE cards ADD COLUMN user_id BIGINT;
UPDATE cards SET user_id = 1;
ALTER TABLE cards ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE cards ADD CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE transactions ADD COLUMN user_id BIGINT;
UPDATE transactions SET user_id = 1;
ALTER TABLE transactions ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE search_keywords ADD COLUMN user_id BIGINT;
UPDATE search_keywords SET user_id = 1;
ALTER TABLE search_keywords ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE search_keywords ADD CONSTRAINT fk_search_keywords_user FOREIGN KEY (user_id) REFERENCES users(id);
