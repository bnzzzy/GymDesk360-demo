-- Planes que vende cada gimnasio
CREATE TABLE plans (
    id              BIGSERIAL PRIMARY KEY,
    gym_id          BIGINT       NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    price           BIGINT       NOT NULL CHECK (price >= 0),          -- pesos chilenos, sin decimales
    duration_value  INT          NOT NULL CHECK (duration_value > 0),
    duration_unit   VARCHAR(10)  NOT NULL CHECK (duration_unit IN ('DAYS', 'MONTHS')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_plans_gym_name UNIQUE (gym_id, name)
);

-- Socios (independientes de los usuarios con login)
CREATE TABLE members (
    id                       BIGSERIAL PRIMARY KEY,
    gym_id                   BIGINT       NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    rut                      VARCHAR(12),                               -- formato 12345678-K
    first_name               VARCHAR(80)  NOT NULL,
    last_name                VARCHAR(80)  NOT NULL,
    email                    VARCHAR(255),
    phone                    VARCHAR(30),
    birth_date               DATE,
    emergency_contact_name   VARCHAR(150),
    emergency_contact_phone  VARCHAR(30),
    notes                    VARCHAR(1000),
    active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_members_gym_rut UNIQUE (gym_id, rut)
);
CREATE INDEX idx_members_gym ON members (gym_id);

-- Membresías vendidas: guardan nombre y precio del plan al momento de la venta
CREATE TABLE memberships (
    id            BIGSERIAL PRIMARY KEY,
    gym_id        BIGINT       NOT NULL REFERENCES gyms (id) ON DELETE CASCADE,
    member_id     BIGINT       NOT NULL REFERENCES members (id),
    plan_id       BIGINT       NOT NULL REFERENCES plans (id),
    plan_name     VARCHAR(100) NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    price_paid    BIGINT       NOT NULL CHECK (price_paid >= 0),
    notes         VARCHAR(500),
    cancelled_at  TIMESTAMPTZ,
    created_by    BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_memberships_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_memberships_member ON memberships (member_id);
CREATE INDEX idx_memberships_gym_end ON memberships (gym_id, end_date);
CREATE INDEX idx_memberships_plan ON memberships (plan_id);
