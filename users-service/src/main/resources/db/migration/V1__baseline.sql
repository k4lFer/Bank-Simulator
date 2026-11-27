-- Fase 1.1 / 1.7: baseline idempotente de bank_users.
-- Generado desde las entidades JPA con Hibernate 7 + MySQLDialect.

CREATE TABLE IF NOT EXISTS users (
    active bit not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    id binary(16) not null,
    email varchar(255) not null,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    password varchar(255) not null,
    phone varchar(255),
    role enum ('ADMIN','CUSTOMER') not null,
    primary key (id),
    constraint uk_user_email unique (email)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS user_profiles (
    date_of_birth date,
    created_at datetime(6) not null,
    user_id binary(16) not null,
    id_document varchar(50),
    occupation varchar(100),
    address varchar(255),
    primary key (user_id),
    constraint fk_user_profiles_user foreign key (user_id) references users (id)
) engine=InnoDB;
