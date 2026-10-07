-- Criacao de schemas se nao existirem
CREATE SCHEMA IF NOT EXISTS sc_corporativo;
CREATE SCHEMA IF NOT EXISTS sc_frota;
CREATE SCHEMA IF NOT EXISTS sc_operacao;

-- Usuario
ALTER TABLE public.usuario SET SCHEMA sc_corporativo;
ALTER TABLE sc_corporativo.usuario RENAME TO tb_usuario;

-- Endereco
ALTER TABLE public.endereco SET SCHEMA sc_corporativo;
ALTER TABLE sc_corporativo.endereco RENAME TO tb_endereco;

-- Fazenda
ALTER TABLE public.fazenda SET SCHEMA sc_corporativo;
ALTER TABLE sc_corporativo.fazenda RENAME TO tb_fazenda;

-- VeiculoCarreta
ALTER TABLE public.veiculo_carreta SET SCHEMA sc_frota;
ALTER TABLE sc_frota.veiculo_carreta RENAME TO tb_veiculo_carreta;

-- VeiculoCavalo
ALTER TABLE public.veiculo_cavalo SET SCHEMA sc_frota;
ALTER TABLE sc_frota.veiculo_cavalo RENAME TO tb_veiculo_cavalo;

-- RelatorioViagem
ALTER TABLE public.relatorio_viagem SET SCHEMA sc_operacao;
ALTER TABLE sc_operacao.relatorio_viagem RENAME TO tb_relatorio_viagem;

-- ParadaImprevista
ALTER TABLE public.parada_imprevista SET SCHEMA sc_operacao;
ALTER TABLE sc_operacao.parada_imprevista RENAME TO tb_parada_imprevista;

-- AnomaliaEmbarque
ALTER TABLE public.anomalia_embarque SET SCHEMA sc_operacao;
ALTER TABLE sc_operacao.anomalia_embarque RENAME TO tb_anomalia_embarque;

-- AnomaliaDesembarque
ALTER TABLE public.anomalia_desembarque SET SCHEMA sc_operacao;
ALTER TABLE sc_operacao.anomalia_desembarque RENAME TO tb_anomalia_desembarque;
