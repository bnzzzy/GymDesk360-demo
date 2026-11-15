-- Distingue los refresh tokens revocados por rotación (ya canjeados) de los revocados por
-- logout / cambio de contraseña / acción del admin. Solo reutilizar uno ROTADO indica robo.
ALTER TABLE refresh_tokens ADD COLUMN rotated BOOLEAN NOT NULL DEFAULT FALSE;
