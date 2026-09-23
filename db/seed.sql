-- ============================================================
-- Dados de teste para a API.
-- Rode UMA VEZ no DBeaver, no banco saude_basica, DEPOIS de subir
-- a aplicacao pela primeira vez (e a aplicacao que cria as tabelas,
-- por causa do ddl-auto: update).
-- ============================================================

INSERT INTO municipio (codigo_ibge, nome) VALUES
    ('2800308', 'Aracaju'),
    ('2803302', 'Nossa Senhora do Socorro');

INSERT INTO unidade_saude (nome, cnes, municipio_id) VALUES
    ('USF Centro',        '1234567', 1),
    ('USF Rosa Elze',     '2345678', 1),
    ('USF Marcos Freire', '3456789', 2);

INSERT INTO profissional (nome, cns, especialidade, unidade_id) VALUES
    ('Dra. Ana Lima',      '700000000000001', 'Clinica Geral', 1),
    ('Dr. Bruno Santos',   '700000000000002', 'Pediatria',     1),
    ('Dra. Carla Moura',   '700000000000003', 'Cardiologia',   2),
    ('Dr. Diego Alves',    '700000000000004', 'Clinica Geral', 3);

INSERT INTO paciente (nome, cpf, data_nascimento, municipio_id) VALUES
    ('Joao da Silva',     '11111111111', '1990-03-12', 1),
    ('Maria Oliveira',    '22222222222', '1985-07-25', 1),
    ('Pedro Costa',       '33333333333', '2001-11-02', 1),
    ('Lucia Ferreira',    '44444444444', '1978-01-30', 2),
    ('Rafael Gomes',      '55555555555', '1995-09-14', 2),
    ('Beatriz Nunes',     '66666666666', '2010-05-08', 1);

-- Confere se entrou tudo
SELECT 'municipios', COUNT(*) FROM municipio
UNION ALL SELECT 'unidades', COUNT(*) FROM unidade_saude
UNION ALL SELECT 'profissionais', COUNT(*) FROM profissional
UNION ALL SELECT 'pacientes', COUNT(*) FROM paciente;
