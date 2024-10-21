CREATE TABLE IF NOT EXISTS Auto_Farm_Ip (
    char_id INT NOT NULL,
    ip VARCHAR(15) NOT NULL,
    PRIMARY KEY (char_id, ip)
);