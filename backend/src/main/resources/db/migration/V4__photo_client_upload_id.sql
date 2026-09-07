ALTER TABLE photos ADD COLUMN client_upload_id VARCHAR(36);

CREATE UNIQUE INDEX idx_photos_client_upload_id ON photos (client_upload_id);
