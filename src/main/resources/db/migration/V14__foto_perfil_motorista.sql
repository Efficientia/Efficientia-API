ALTER TABLE sc_corporativo.tb_usuario
    ADD COLUMN IF NOT EXISTS foto_perfil BYTEA;

ALTER TABLE sc_corporativo.tb_usuario
    ADD COLUMN IF NOT EXISTS foto_perfil_mime VARCHAR(30);

ALTER TABLE sc_corporativo.tb_usuario
    ADD COLUMN IF NOT EXISTS foto_perfil_atualizada_em TIMESTAMPTZ;
