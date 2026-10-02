-- Somente leitura. Nao imprime os cadastros nem as credenciais.
-- Compare a impressao digital antes/depois, sem escritas concorrentes.
BEGIN READ ONLY;
SELECT 'contagens' AS verificacao,
       (SELECT count(*) FROM municipio) AS municipios,
       (SELECT count(*) FROM unidade_saude) AS unidades,
       (SELECT count(*) FROM profissional) AS profissionais,
       (SELECT count(*) FROM paciente) AS pacientes,
       (SELECT count(*) FROM atendimento) AS atendimentos;
SELECT min(data_atendimento)::date AS data_inicio,
       max(data_atendimento)::date AS data_fim FROM atendimento;
SELECT status, count(*) AS total FROM atendimento GROUP BY status ORDER BY status;
SELECT a.unidade_id, a.status, count(*) AS total
FROM atendimento a
GROUP BY a.unidade_id, a.status ORDER BY a.unidade_id, a.status;
SELECT md5(string_agg(tabela || ':' || resumo, '|' ORDER BY tabela)) AS impressao_digital
FROM (
    SELECT 'municipio' AS tabela, md5(string_agg(row_to_json(t)::text, '|' ORDER BY id)) AS resumo FROM municipio t
    UNION ALL SELECT 'unidade_saude', md5(string_agg(row_to_json(t)::text, '|' ORDER BY id)) FROM unidade_saude t
    UNION ALL SELECT 'profissional', md5(string_agg(row_to_json(t)::text, '|' ORDER BY id)) FROM profissional t
    UNION ALL SELECT 'paciente', md5(string_agg(row_to_json(t)::text, '|' ORDER BY id)) FROM paciente t
    UNION ALL SELECT 'atendimento', md5(string_agg(row_to_json(t)::text, '|' ORDER BY id)) FROM atendimento t
) s;
COMMIT;
