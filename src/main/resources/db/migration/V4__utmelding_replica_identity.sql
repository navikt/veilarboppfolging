-- needed for datastream setup, or you won't be able to delete from utmelding
ALTER TABLE utmelding REPLICA IDENTITY USING INDEX unique_aktor_id;