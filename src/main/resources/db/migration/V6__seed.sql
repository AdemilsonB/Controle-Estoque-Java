INSERT INTO funcionario (nome, sobrenome, cpf, email, senha, matricula, data_admissao, setor, role,
                          logradouro, numero, bairro, cidade, estado, cep, ativo, criado_em, atualizado_em)
VALUES ('Admin', 'Sistema', '00000000000', 'admin@estoque.com',
        '$2b$10$RCnF8W47c/o7N.ePdJU2Deq/2NNmz9Bje5XhPTj8epL7T0vParxcW',
        'F000', CURRENT_DATE, 'Administração', 'ADMIN',
        'Rua Principal', '1', 'Centro', 'Curitiba', 'PR', '80000-000',
        TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categoria (nome, descricao) VALUES
    ('Calçados', 'Tênis, sapatos e sandálias'),
    ('Vestuário', 'Camisetas, calças e jaquetas'),
    ('Acessórios', 'Bolsas, cintos e bonés');

INSERT INTO colecao (nome, descricao) VALUES
    ('Verão 2026', 'Coleção lançada em janeiro de 2026'),
    ('Inverno 2026', 'Coleção lançada em junho de 2026');
