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
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    author       VARCHAR(200)  NOT NULL,
    title        VARCHAR(300)  NOT NULL,
    pages        INT           NOT NULL DEFAULT 0,
    isbn         VARCHAR(20),
    status       VARCHAR(30)   NOT NULL DEFAULT 'AVAILABLE'
                 COMMENT 'AVAILABLE | BORROWED | RESERVED | UNAVAILABLE',
    publish_year INT           DEFAULT 0,
    publisher    VARCHAR(200),
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

CREATE TABLE IF NOT EXISTS extension_requests (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    rental_id    BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                     COMMENT 'PENDING | APPROVED | REJECTED',
    request_date DATE        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ext_rental FOREIGN KEY (rental_id) REFERENCES rentals(id) ON DELETE CASCADE,
    CONSTRAINT fk_ext_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reservations (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    book_id      BIGINT      NOT NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                     COMMENT 'PENDING | APPROVED | REJECTED | CANCELLED',
    request_date DATE        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_email      ON users  (email);
CREATE INDEX idx_books_title      ON books  (title);
CREATE INDEX idx_rentals_user_id  ON rentals(user_id);
CREATE INDEX idx_rentals_book_id  ON rentals(book_id);
CREATE INDEX idx_rentals_status   ON rentals(status);
CREATE INDEX idx_ext_req_status   ON extension_requests(status);
CREATE INDEX idx_reserv_status    ON reservations(status);

-- ============================================================
-- Development-only seed accounts. DO NOT use in production.
-- Remove or replace these credentials before deploying.
-- ============================================================

-- Test admin account: admin@alexandria.local / Admin123!
INSERT IGNORE INTO users (f_name, l_name, email, password, phone_number, user_rank)
VALUES ('Admin', 'Alexandria', 'admin@alexandria.local',
        '$2a$12$3tbigAhPqbh4khhj5.9qlOtCC8q8y0zJCnDTl05mnLVeksJ3J07cy',
        '+48 000 000 000', 1);

-- Test user account: user@alexandria.local / User1234!
INSERT IGNORE INTO users (f_name, l_name, email, password, phone_number, user_rank)
VALUES ('Jan', 'Kowalski', 'user@alexandria.local',
        '$2a$12$7LlWD4VdZnNDLiCDc0QOFu2rOsbN941o92zbA./HUctT9xhnZIxxG',
        '+48 111 222 333', 0);

INSERT IGNORE INTO books (author, title, pages, isbn, status, publish_year, publisher) VALUES
    ('Adam Mickiewicz',      'Pan Tadeusz',                       302,  '978-83-7339-765-4', 'AVAILABLE', 1834, 'Ossolineum'),
    ('Henryk Sienkiewicz',   'Ogniem i Mieczem',                  720,  '978-83-7000-000-1', 'AVAILABLE', 1884, 'Gebethner i Wolff'),
    ('Stanisław Lem',        'Solaris',                           304,  '978-83-7736-267-8', 'AVAILABLE', 1961, 'Wydawnictwo MON'),
    ('Bolesław Prus',        'Lalka',                             892,  '978-83-7337-150-9', 'AVAILABLE', 1890, 'Gebethner i Wolff'),
    ('Stanisław Lem',        'Cyberiada',                         264,  '978-83-7736-100-8', 'AVAILABLE', 1965, 'Wydawnictwo Literackie'),
    ('Andrzej Sapkowski',    'Wiedźmin: Ostatnie życzenie',       288,  '978-83-7578-507-4', 'AVAILABLE', 1993, 'superNOWA'),
    ('Olga Tokarczuk',       'Bieguni',                           432,  '978-83-08-04321-0', 'AVAILABLE', 2007, 'Wydawnictwo Literackie'),
    ('Ryszard Kapuściński',  'Cesarz',                            170,  '978-83-06-02841-9', 'AVAILABLE', 1978, 'Czytelnik'),
    ('Henryk Sienkiewicz',   'Quo Vadis',                         590,  '978-83-7000-001-8', 'AVAILABLE', 1896, 'Gebethner i Wolff'),
    ('Henryk Sienkiewicz',   'Potop',                             936,  '978-83-7000-002-5', 'AVAILABLE', 1886, 'Gebethner i Wolff'),
    ('Henryk Sienkiewicz',   'Pan Wołodyjowski',                  480,  '978-83-7000-003-2', 'AVAILABLE', 1888, 'Gebethner i Wolff'),
    ('Adam Mickiewicz',      'Dziady',                            320,  '978-83-7339-766-1', 'AVAILABLE', 1823, 'Ossolineum'),
    ('Juliusz Słowacki',     'Kordian',                           128,  '978-83-7339-767-8', 'AVAILABLE', 1834, 'Ossolineum'),
    ('Juliusz Słowacki',     'Balladyna',                         150,  '978-83-7339-768-5', 'AVAILABLE', 1839, 'Ossolineum'),
    ('Witold Gombrowicz',    'Ferdydurke',                        304,  '978-83-7339-769-2', 'AVAILABLE', 1937, 'Rój'),
    ('Witold Gombrowicz',    'Trans-Atlantyk',                    140,  '978-83-7339-770-8', 'AVAILABLE', 1953, 'Instytut Literacki'),
    ('Stanisław Lem',        'Bajki robotów',                     200,  '978-83-7736-101-5', 'AVAILABLE', 1964, 'Wydawnictwo Literackie'),
    ('Stanisław Lem',        'Niezwyciężony',                     208,  '978-83-7736-102-2', 'AVAILABLE', 1964, 'Wydawnictwo MON'),
    ('Stanisław Lem',        'Głos Pana',                         240,  '978-83-7736-103-9', 'AVAILABLE', 1968, 'Czytelnik'),
    ('Stanisław Lem',        'Kongres futurologiczny',            144,  '978-83-7736-104-6', 'AVAILABLE', 1971, 'Wydawnictwo Literackie'),
    ('Andrzej Sapkowski',    'Wiedźmin: Miecz przeznaczenia',     320,  '978-83-7578-508-1', 'AVAILABLE', 1992, 'superNOWA'),
    ('Andrzej Sapkowski',    'Wiedźmin: Krew elfów',              336,  '978-83-7578-509-8', 'AVAILABLE', 1994, 'superNOWA'),
    ('Andrzej Sapkowski',    'Wiedźmin: Czas pogardy',            352,  '978-83-7578-510-4', 'AVAILABLE', 1995, 'superNOWA'),
    ('Andrzej Sapkowski',    'Wiedźmin: Chrzest ognia',           340,  '978-83-7578-511-1', 'AVAILABLE', 1996, 'superNOWA'),
    ('Andrzej Sapkowski',    'Wiedźmin: Wieża Jaskółki',          432,  '978-83-7578-512-8', 'AVAILABLE', 1997, 'superNOWA'),
    ('Andrzej Sapkowski',    'Wiedźmin: Pani Jeziora',            544,  '978-83-7578-513-5', 'AVAILABLE', 1999, 'superNOWA'),
    ('Olga Tokarczuk',       'Prowadź swój pług przez kości umarłych', 280, '978-83-08-04322-7', 'AVAILABLE', 2009, 'Wydawnictwo Literackie'),
    ('Olga Tokarczuk',       'Księgi Jakubowe',                   912,  '978-83-08-04323-4', 'AVAILABLE', 2014, 'Wydawnictwo Literackie'),
    ('Olga Tokarczuk',       'Dom dzienny, dom nocny',            312,  '978-83-08-04324-1', 'AVAILABLE', 1998, 'Wydawnictwo Literackie'),
    ('Wisława Szymborska',   'Wiersze wybrane',                   320,  '978-83-240-0001-1', 'AVAILABLE', 2000, 'Znak'),
    ('Czesław Miłosz',       'Dolina Issy',                       240,  '978-83-240-0002-8', 'AVAILABLE', 1955, 'Instytut Literacki'),
    ('Czesław Miłosz',       'Zniewolony umysł',                  256,  '978-83-240-0003-5', 'AVAILABLE', 1953, 'Instytut Literacki'),
    ('Tadeusz Borowski',     'Pożegnanie z Marią',                180,  '978-83-240-0004-2', 'AVAILABLE', 1947, 'Wiedza'),
    ('Zofia Nałkowska',      'Granica',                           320,  '978-83-240-0005-9', 'AVAILABLE', 1935, 'Rój'),
    ('Władysław Reymont',    'Chłopi',                            840,  '978-83-240-0006-6', 'AVAILABLE', 1904, 'Gebethner i Wolff'),
    ('Stefan Żeromski',      'Przedwiośnie',                      296,  '978-83-240-0007-3', 'AVAILABLE', 1924, 'Mortkowicz'),
    ('Stefan Żeromski',      'Ludzie bezdomni',                   380,  '978-83-240-0008-0', 'AVAILABLE', 1900, 'Gebethner i Wolff'),
    ('Gabriela Zapolska',    'Moralność pani Dulskiej',           144,  '978-83-240-0009-7', 'AVAILABLE', 1907, 'Gebethner i Wolff'),
    ('Bruno Schulz',         'Sklepy cynamonowe',                 160,  '978-83-240-0010-3', 'AVAILABLE', 1934, 'Rój'),
    ('Bruno Schulz',         'Sanatorium pod Klepsydrą',          176,  '978-83-240-0011-0', 'AVAILABLE', 1937, 'Rój'),
    ('Sławomir Mrożek',      'Tango',                             128,  '978-83-240-0012-7', 'AVAILABLE', 1964, 'Wydawnictwo Literackie'),
    ('Maria Dąbrowska',      'Noce i dnie',                       1200, '978-83-240-0013-4', 'AVAILABLE', 1932, 'Rój'),
    ('Zbigniew Herbert',     'Pan Cogito',                        120,  '978-83-240-0014-1', 'AVAILABLE', 1974, 'Czytelnik'),
    ('Jacek Dukaj',          'Lód',                               1024, '978-83-240-0015-8', 'AVAILABLE', 2007, 'Wydawnictwo Literackie'),
    ('Jacek Dukaj',          'Inne pieśni',                       640,  '978-83-240-0016-5', 'AVAILABLE', 2003, 'Wydawnictwo Literackie'),
    ('Dorota Masłowska',     'Wojna polsko-ruska pod flagą biało-czerwoną', 192, '978-83-240-0017-2', 'AVAILABLE', 2002, 'Lampa i Iskra Boża'),
    ('Szczepan Twardoch',    'Morfina',                           352,  '978-83-240-0018-9', 'AVAILABLE', 2012, 'Wydawnictwo Literackie'),
    ('Szczepan Twardoch',    'Drach',                             272,  '978-83-240-0019-6', 'AVAILABLE', 2014, 'Wydawnictwo Literackie'),
    ('Zygmunt Miłoszewski', 'Ziarno prawdy',                     352,  '978-83-240-0020-2', 'AVAILABLE', 2011, 'W.A.B.'),
    ('Zygmunt Miłoszewski', 'Gniew',                             384,  '978-83-240-0021-9', 'AVAILABLE', 2014, 'W.A.B.'),
    ('Remigiusz Mróz',       'Kasacja',                           480,  '978-83-240-0022-6', 'AVAILABLE', 2015, 'Czwarta Strona'),
    ('Remigiusz Mróz',       'Testament',                         512,  '978-83-240-0023-3', 'AVAILABLE', 2016, 'Czwarta Strona'),
    ('Joanna Chmielewska',   'Całe zdanie nieboszczyka',          224,  '978-83-240-0024-0', 'AVAILABLE', 1972, 'Czytelnik'),
    ('Joanna Chmielewska',   'Krokodyl z Kraju Karoliny',         240,  '978-83-240-0025-7', 'AVAILABLE', 1969, 'Czytelnik'),
    ('Marek Hłasko',         'Piękni dwudziestoletni',            160,  '978-83-240-0026-4', 'AVAILABLE', 1966, 'Instytut Literacki'),
    ('Tadeusz Konwicki',     'Mała apokalipsa',                   184,  '978-83-240-0027-1', 'AVAILABLE', 1979, 'Index on Censorship'),
    ('Józef Hen',            'Crimen',                            288,  '978-83-240-0028-8', 'AVAILABLE', 2006, 'W.A.B.'),
    ('Wiesław Myśliwski',    'Traktat o łuskaniu fasoli',         400,  '978-83-240-0029-5', 'AVAILABLE', 2006, 'Znak'),
    ('Leopold Tyrmand',      'Zły',                               640,  '978-83-240-0030-1', 'AVAILABLE', 1955, 'Czytelnik');
