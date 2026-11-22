-- Sets now span multiple sports (Topps + Panini, 2000-present).
-- Every set that exists before this migration is a Bowman baseball product.
ALTER TABLE sets ADD COLUMN sport VARCHAR(32);

UPDATE sets SET sport = 'Baseball' WHERE sport IS NULL;

ALTER TABLE sets ALTER COLUMN sport SET NOT NULL;
