CREATE TABLE app_settings (
    key   VARCHAR(64)  PRIMARY KEY,
    value VARCHAR(255) NOT NULL
);

INSERT INTO app_settings (key, value) VALUES ('registration.enabled', 'false');
