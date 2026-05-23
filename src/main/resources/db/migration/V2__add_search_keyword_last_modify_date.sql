ALTER TABLE search_keywords ADD COLUMN last_modify_date TIMESTAMP;
UPDATE search_keywords SET last_modify_date = last_search_time WHERE last_search_time IS NOT NULL;
