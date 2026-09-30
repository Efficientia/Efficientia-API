-- V6: Dados cadastrais complementares da empresa (Etapa 1 de 3 - Onboarding Corporativo)
-- Adiciona suporte a telefone, nome fantasia, URL de logotipo e controle de progresso cadastral

ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS nome_fantasia VARCHAR(150);
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS etapa_cadastro INTEGER DEFAULT 1;
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS cadastro_completo BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_empresa_telefone ON public.empresa(telefone);
