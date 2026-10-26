CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    avatar     VARCHAR(500)
);
