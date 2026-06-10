-- Wordle schema v1 (CLAUDE.md §7). Column names checked against the
-- MySQL 9.x reserved-word list (mode/answer/guesses/fails are all safe).

CREATE TABLE solver_config (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(64)  NOT NULL,
    opener     CHAR(5)      NOT NULL,
    hard_mode  BOOLEAN      NOT NULL DEFAULT FALSE,
    guess_pool VARCHAR(16)  NOT NULL,
    tiebreak   VARCHAR(32)  NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_solver_config_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE game (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    mode        VARCHAR(16) NOT NULL, -- SIMULATOR / DAILY / BENCHMARK
    answer      CHAR(5)     NOT NULL,
    solved      BOOLEAN     NOT NULL,
    num_guesses INT         NOT NULL,
    guesses     JSON        NOT NULL, -- [{"word":"salet","pattern":93}, ...]
    config_id   BIGINT      NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_game_mode_created (mode, created_at),
    CONSTRAINT fk_game_config FOREIGN KEY (config_id) REFERENCES solver_config (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE benchmark_run (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    config_id    BIGINT      NOT NULL,
    answer_count INT         NOT NULL,
    win_rate     DOUBLE      NOT NULL,
    avg_guesses  DOUBLE      NOT NULL,
    dist_1       INT         NOT NULL DEFAULT 0,
    dist_2       INT         NOT NULL DEFAULT 0,
    dist_3       INT         NOT NULL DEFAULT 0,
    dist_4       INT         NOT NULL DEFAULT 0,
    dist_5       INT         NOT NULL DEFAULT 0,
    dist_6       INT         NOT NULL DEFAULT 0,
    fails        INT         NOT NULL DEFAULT 0,
    duration_ms  BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_benchmark_run_config (config_id),
    CONSTRAINT fk_benchmark_run_config FOREIGN KEY (config_id) REFERENCES solver_config (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE daily_solve (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    puzzle_date DATE        NOT NULL,
    puzzle_id   INT         NULL,
    answer      CHAR(5)     NULL, -- unknown until an ASSIST game concludes
    solved      BOOLEAN     NOT NULL,
    num_guesses INT         NOT NULL,
    mode        VARCHAR(8)  NOT NULL, -- AUTO / ASSIST
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_solve_date_mode (puzzle_date, mode)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
