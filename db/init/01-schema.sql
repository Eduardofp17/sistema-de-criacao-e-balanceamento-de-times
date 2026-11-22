CREATE TABLE usuario (
    id       SERIAL PRIMARY KEY,
    nome     VARCHAR(100) NOT NULL,
    username VARCHAR(50)  NOT NULL UNIQUE,
    senha    VARCHAR(255) NOT NULL,
    ativo    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE esporte (
    id             SERIAL PRIMARY KEY,
    nome           VARCHAR(50) NOT NULL,
    tamanho_equipe INT         NOT NULL CHECK (tamanho_equipe > 0)
);

CREATE TABLE grupo (
    id               SERIAL PRIMARY KEY,
    nome             VARCHAR(100) NOT NULL UNIQUE,
    descricao        TEXT,
    data_criacao     DATE         NOT NULL DEFAULT CURRENT_DATE,
    hora_criacao     TIME         NOT NULL DEFAULT LOCALTIME,
    esporte_id       INT          NOT NULL UNIQUE REFERENCES esporte (id),
    administrador_id INT          NOT NULL REFERENCES usuario (id),
    criador_id       INT          NOT NULL REFERENCES usuario (id)
);

CREATE TABLE participacao (
    usuario_id   INT  NOT NULL REFERENCES usuario (id),
    grupo_id     INT  NOT NULL REFERENCES grupo (id) ON DELETE CASCADE,
    data_entrada DATE NOT NULL DEFAULT CURRENT_DATE,
    hora_entrada TIME NOT NULL DEFAULT LOCALTIME,
    PRIMARY KEY (usuario_id, grupo_id)
);

CREATE TABLE habilidade (
    usuario_id INT              NOT NULL,
    grupo_id   INT              NOT NULL,
    valor      DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (valor BETWEEN 0 AND 5),
    PRIMARY KEY (usuario_id, grupo_id),
    FOREIGN KEY (usuario_id, grupo_id) REFERENCES participacao (usuario_id, grupo_id) ON DELETE CASCADE
);

CREATE TABLE time_esportivo (
    id               SERIAL PRIMARY KEY,
    grupo_id         INT              NOT NULL REFERENCES grupo (id) ON DELETE CASCADE,
    nome             VARCHAR(100)     NOT NULL,
    media_habilidade DOUBLE PRECISION NOT NULL DEFAULT 0,
    UNIQUE (grupo_id, nome)
);

CREATE TABLE time_jogador (
    time_id    INT NOT NULL REFERENCES time_esportivo (id) ON DELETE CASCADE,
    usuario_id INT NOT NULL REFERENCES usuario (id),
    PRIMARY KEY (time_id, usuario_id)
);
