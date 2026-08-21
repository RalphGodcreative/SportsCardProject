-- Replaces the one-to-many "storages" feature with many-to-many "tags".
-- See docs/impl-tags.md

-- 1. storages becomes tags (keeps the rows, the ids, and the identity sequence)
ALTER TABLE storages RENAME TO tags;
ALTER TABLE tags RENAME CONSTRAINT fk_storages_user TO fk_tags_user;

-- 2. the join table
CREATE TABLE card_tags (
    card_id INTEGER NOT NULL,
    tag_id  BIGINT  NOT NULL,
    PRIMARY KEY (card_id, tag_id),
    CONSTRAINT fk_card_tags_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_tags_tag  FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
);

CREATE INDEX idx_card_tags_tag ON card_tags (tag_id);

-- 3. carry every existing assignment over
INSERT INTO card_tags (card_id, tag_id)
SELECT id, storage_id FROM cards WHERE storage_id IS NOT NULL;

-- 4. drop the old single-valued column
ALTER TABLE cards DROP CONSTRAINT fk_cards_storage;
ALTER TABLE cards DROP COLUMN storage_id;
