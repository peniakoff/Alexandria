-- Alexandria database schema
-- Compatible with MySQL 8+ and the Docker Compose setup

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS users (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    f_name       VARCHAR(100)  NOT NULL,
    l_name       VARCHAR(100)  NOT NULL,
    email        VARCHAR(255)  NOT NULL UNIQUE,
    password     VARCHAR(255)  NOT NULL COMMENT 'BCrypt hash',
    phone_number VARCHAR(30),
    user_rank    TINYINT       NOT NULL DEFAULT 0 COMMENT '0=user 1=admin',
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS books (
    id      BIGINT        NOT NULL AUTO_INCREMENT,
    author  VARCHAR(200)  NOT NULL,
    title   VARCHAR(300)  NOT NULL,
    pages   INT           NOT NULL DEFAULT 0,
    isbn    VARCHAR(20),
    status  VARCHAR(30)   NOT NULL DEFAULT 'AVAILABLE'
                COMMENT 'AVAILABLE | BORROWED | RESERVED | UNAVAILABLE',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rentals (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    book_id     BIGINT   NOT NULL,
    borrow_date DATE     NOT NULL,
    due_date    DATE     NOT NULL,
    return_date DATE,
    status      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
                    COMMENT 'ACTIVE | RETURNED | OVERDUE | RETURNED_LATE',
    PRIMARY KEY (id),
    CONSTRAINT fk_rental_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_email      ON users  (email);
CREATE INDEX idx_books_title      ON books  (title);
CREATE INDEX idx_rentals_user_id  ON rentals(user_id);
CREATE INDEX idx_rentals_book_id  ON rentals(book_id);
CREATE INDEX idx_rentals_status   ON rentals(status);

INSERT IGNORE INTO users (f_name, l_name, email, password, phone_number, user_rank)
VALUES ('Admin', 'Alexandria', 'admin@alexandria.local',
        '$2a$12$K8LGNKQoFbSsq3qYqF1YBudJR0mJkHiAl3ZmI0ZEDM8e7KfL.Ci1W',
        '+48 000 000 000', 1);

INSERT IGNORE INTO books (author, title, pages, isbn, status) VALUES
    ('Adam Mickiewicz',    'Pan Tadeusz',                  302,  '978-83-7339-765-4', 'AVAILABLE'),
    ('Henryk Sienkiewicz', 'Ogniem i Mieczem',             720,  '978-83-7000-000-1', 'AVAILABLE'),
    ('Stanisław Lem',      'Solaris',                      304,  '978-83-7736-267-8', 'AVAILABLE'),
    ('Bolesław Prus',      'Lalka',                        892,  '978-83-7337-150-9', 'AVAILABLE'),
    ('Stanisław Lem',      'Cyberiada',                    264,  '978-83-7736-100-8', 'AVAILABLE'),
    ('Andrzej Sapkowski',  'Wiedźmin: Ostatnie życzenie',  288,  '978-83-7578-507-4', 'AVAILABLE'),
    ('Olga Tokarczuk',     'Bieguni',                      432,  '978-83-08-04321-0', 'AVAILABLE'),
    ('Ryszard Kapuściński','Cesarz',                       170,  '978-83-06-02841-9', 'AVAILABLE');
