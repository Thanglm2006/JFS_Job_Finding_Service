/* Extensions and Enums */
CREATE EXTENSION IF NOT EXISTS pgroonga;

CREATE TYPE employer_type AS ENUM (
    'Company', 'Shop', 'Restaurant', 'Supermarket', 'Hotel',
    'School', 'Hospital', 'Recruiter', 'Government', 'NGO',
    'Startup', 'Event Organizer', 'Construction', 'Transportation',
    'Salon', 'Gym', 'Farm', 'Entertainment', 'E-commerce', 'Individual', 'Other'
);
CREATE TYPE verification_status AS ENUM ('PENDING', 'VERIFIED', 'REJECTED', 'BANNED');
CREATE TYPE application_status AS ENUM ('PENDING', 'REVIEWED', 'ACCEPTED', 'REJECTED');
CREATE TYPE job_type AS ENUM ('PART_TIME', 'FULL_TIME', 'INTERNSHIP');
CREATE TYPE employer_request_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');
CREATE TYPE position_status AS ENUM ('OPEN', 'CLOSED');
/* Sequences */
CREATE SEQUENCE employer_id_seq START 1;
CREATE SEQUENCE applicant_id_seq START 1;
CREATE SEQUENCE job_post_id_seq START 1;

/* Tables */
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name TEXT NOT NULL CHECK (full_name ~ '^[[:alpha:] ]+$'),
    email TEXT UNIQUE NOT NULL CHECK (email ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'),
    password TEXT NOT NULL,
    phone TEXT UNIQUE CHECK (phone ~ '^[0-9]{10,15}$'),
    address TEXT,
    gender TEXT CHECK (gender IN ('male', 'female', 'other')) NOT NULL,
    date_of_birth DATE NOT NULL,
    role TEXT CHECK (role IN ('Employer', 'Applicant')) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE employer (
    id TEXT PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    org_name TEXT NOT NULL,
    type employer_type NOT NULL,
    custom_type TEXT,
    tax_code TEXT UNIQUE,
    business_license_url TEXT,
    business_code text,
    company_website TEXT,
    company_email TEXT,
    headquarters_address TEXT,
    id_card_number text,
    id_card_front text,
    status verification_status DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE admin (
    id SERIAL PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL CHECK (email ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'),
    password TEXT NOT NULL
);

CREATE TABLE employer_request(
    id SERIAL PRIMARY KEY,
    employer TEXT REFERENCES employer(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status employer_request_status DEFAULT 'PENDING',
    rejection_reason TEXT
);

CREATE TABLE applicant (
    id TEXT PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    resume JSONB
);

CREATE TABLE job_post (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    positions JSONB NOT NULL DEFAULT '[]'::jsonb,
    employer_id TEXT REFERENCES employer(id) ON DELETE CASCADE,
    job_description JSONB not null,
    requirements JSONB not null,
    responsibilities JSONB not null,
    advantages JSONB not null,
    extension JSONB,
    type job_type not null,
    workspace_picture TEXT,
    addresses text array not null,
    salary_min NUMERIC,
    salary_max NUMERIC,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    search_text TEXT,
    CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);

CREATE TABLE pending_job_post (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    positions JSONB NOT NULL DEFAULT '[]'::jsonb,
    employer_id TEXT REFERENCES employer(id) ON DELETE CASCADE,
    job_description JSONB not null,
    requirements JSONB not null,
    responsibilities JSONB not null,
    advantages JSONB not null,
    extension JSONB,
    type job_type not null,
    addresses text array not null,
    salary_min NUMERIC,
    salary_max NUMERIC,
    workspace_picture TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);

CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE image_folders (
    id SERIAL PRIMARY KEY,
    folder_name TEXT NOT NULL,
    file_name TEXT NOT NULL
);

CREATE TABLE job_taken (
    id SERIAL PRIMARY KEY,
    job_id TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    applicant_id TEXT NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    taken_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE schedule (
    id SERIAL PRIMARY KEY,
    applicant_id TEXT NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    job_id TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    start_time INT NOT NULL CHECK (start_time >= 0 AND start_time <= 1439),
    end_time INT NOT NULL CHECK (end_time > start_time AND end_time <= 1439),
    day TEXT NOT NULL CHECK (day IN ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')),
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
create table Interview (
    id SERIAL PRIMARY KEY,
    applicant_id TEXT NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    employer_id TEXT REFERENCES employer(id) ON DELETE CASCADE,
    interview_date TIMESTAMP DEFAULT NOW(),
    room text,
    created_at TIMESTAMP DEFAULT NOW()

);
CREATE TABLE report_on_user (
    id SERIAL PRIMARY KEY,
    reported_by INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,V
    reason TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE report_on_post (
    id SERIAL PRIMARY KEY,
    reported_by INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_post TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE applications (
    id SERIAL PRIMARY KEY,
    job_id TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    applicant_id TEXT NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    status application_status not null DEFAULT 'PENDING',
    cv text not null,
    interview_date timestamp,
    reason text,
    position TEXT NOT NULL,
    applied_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE saved_jobs (
    id SERIAL PRIMARY KEY,
    applicant_id TEXT NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    job_id TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    saved_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id SERIAL PRIMARY KEY,
    sender_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    file_url TEXT,
    timestamp TIMESTAMP DEFAULT NOW()
);

/* Triggers for ID Generation and Validation */

CREATE OR REPLACE FUNCTION check_minimum_age() RETURNS TRIGGER AS $$
BEGIN
    IF age(NEW.date_of_birth) < interval '15 years' THEN
        RAISE EXCEPTION 'User must be at least 15 years old.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_user_age BEFORE INSERT OR UPDATE ON users FOR EACH ROW EXECUTE FUNCTION check_minimum_age();

CREATE OR REPLACE FUNCTION validate_job_positions() RETURNS TRIGGER AS $$
DECLARE
    elem jsonb;
    status_text text;
BEGIN
    -- 1. Kiểm tra phải là mảng
    IF jsonb_typeof(NEW.positions) <> 'array' THEN
        RAISE EXCEPTION 'Positions must be a JSON array';
    END IF;

    -- 2. Duyệt từng phần tử
    FOR elem IN SELECT * FROM jsonb_array_elements(NEW.positions)
    LOOP
        IF NOT (elem ? 'name' AND elem ? 'quantity' AND elem ? 'status') THEN
            RAISE EXCEPTION 'Each position object must contain keys: name, quantity, status. Invalid object: %', elem;
        END IF;

        status_text := elem->>'status';

        IF status_text IS NULL OR status_text NOT IN ('OPEN', 'CLOSED') THEN
            RAISE EXCEPTION 'Invalid status value in position: "%". Allowed values are: OPEN, CLOSED', status_text;
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Gắn trigger validate cho job_post
CREATE TRIGGER trg_validate_job_positions
BEFORE INSERT OR UPDATE ON job_post
FOR EACH ROW EXECUTE FUNCTION validate_job_positions();

-- Gắn trigger validate cho pending_job_post
CREATE TRIGGER trg_validate_pending_job_positions
BEFORE INSERT OR UPDATE ON pending_job_post
FOR EACH ROW EXECUTE FUNCTION validate_job_positions();


CREATE OR REPLACE FUNCTION generate_employer_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'EMP-' || LPAD(nextval('employer_id_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER employer_id_trigger BEFORE INSERT ON employer FOR EACH ROW EXECUTE FUNCTION generate_employer_id();

CREATE OR REPLACE FUNCTION generate_applicant_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'APP-' || LPAD(nextval('applicant_id_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER applicant_id_trigger BEFORE INSERT ON applicant FOR EACH ROW EXECUTE FUNCTION generate_applicant_id();

CREATE OR REPLACE FUNCTION generate_job_post_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'JOB-' || LPAD(nextval('job_post_id_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER job_post_id_trigger BEFORE INSERT ON job_post FOR EACH ROW EXECUTE FUNCTION generate_job_post_id();

/* Search Text Trigger Logic - UPDATED FOR JSONB POSITIONS & EMPLOYER INFO */
CREATE OR REPLACE FUNCTION update_job_search_text() RETURNS TRIGGER AS $$
DECLARE
    v_org_name TEXT;
    v_employer_full_name TEXT;
    v_positions_str TEXT;
BEGIN
    SELECT e.org_name, u.full_name
    INTO v_org_name, v_employer_full_name
    FROM employer e
    JOIN users u ON e.user_id = u.id
    WHERE e.id = NEW.employer_id;

    SELECT string_agg(obj->>'name', ' ')
    INTO v_positions_str
    FROM jsonb_array_elements(NEW.positions) obj;

    NEW.search_text :=
        COALESCE(NEW.title, '') || ' ' ||
        COALESCE(v_org_name, '') || ' ' ||
        COALESCE(v_employer_full_name, '') || ' ' ||
        COALESCE(v_positions_str, '') || ' ' ||
        COALESCE(NEW.job_description::text, '') || ' ' ||
        COALESCE(NEW.requirements::text, '') || ' ' ||
        COALESCE(NEW.responsibilities::text, '') || ' ' ||
        COALESCE(NEW.advantages::text, '') || ' ' ||
        COALESCE(array_to_string(NEW.addresses, ' '), '');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_search_text BEFORE INSERT OR UPDATE ON job_post FOR EACH ROW EXECUTE FUNCTION update_job_search_text();

/* Indexes */
CREATE INDEX IF NOT EXISTS idx_job_post_search ON job_post USING pgroonga (search_text);
CREATE INDEX IF NOT EXISTS idx_job_post_salary_range ON job_post (salary_min, salary_max);
CREATE INDEX IF NOT EXISTS idx_job_post_employer_id ON job_post (employer_id);

/* Search Function */
CREATE OR REPLACE FUNCTION filter_job_posts(
    p_keyword TEXT DEFAULT NULL,
    p_job_type job_type DEFAULT NULL,
    p_address TEXT DEFAULT NULL,
    p_min_salary NUMERIC DEFAULT NULL,
    p_max_salary NUMERIC DEFAULT NULL,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS SETOF job_post AS $$
BEGIN
    RETURN QUERY
    SELECT * FROM job_post
    WHERE
        (p_keyword IS NULL OR search_text &@~ p_keyword)
        AND (p_job_type IS NULL OR type = p_job_type)
        AND (p_address IS NULL OR array_to_string(addresses, ' ') &@~ p_address)
        AND (p_min_salary IS NULL OR salary_min >= p_min_salary)
        AND (p_max_salary IS NULL OR salary_max <= p_max_salary)
    ORDER BY created_at DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;


CREATE INDEX IF NOT EXISTS idx_employer_org_name_pgroonga
ON employer USING pgroonga (org_name);

CREATE INDEX IF NOT EXISTS idx_users_full_name_pgroonga
ON users USING pgroonga (full_name);

CREATE OR REPLACE FUNCTION find_jobs_by_org_name(
    p_org_name TEXT,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS SETOF job_post AS $$
BEGIN
    RETURN QUERY
    SELECT jp.*
    FROM job_post jp
    JOIN employer e ON jp.employer_id = e.id
    WHERE e.org_name &@~ p_org_name
    ORDER BY jp.created_at DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION find_jobs_by_employer_name(
    p_employer_name TEXT,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS SETOF job_post AS $$
BEGIN
    RETURN QUERY
    SELECT jp.*
    FROM job_post jp
    JOIN employer e ON jp.employer_id = e.id
    JOIN users u ON e.user_id = u.id
    WHERE u.full_name &@~ p_employer_name
    ORDER BY jp.created_at DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION get_employer_jobs_by_status(
    p_employer_id TEXT,
    p_status TEXT,
    p_limit INTEGER,
    p_offset INTEGER
)
RETURNS SETOF job_post AS $$
BEGIN
    RETURN QUERY
    SELECT * FROM job_post
    WHERE employer_id = p_employer_id
    AND (
        p_status IS NULL
        OR
        (
            p_status = 'OPEN'
            AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(positions) elem
                WHERE elem->>'status' = 'OPEN'
            )
        )
        OR
        (
            p_status = 'CLOSED'
            AND NOT EXISTS (
                SELECT 1 FROM jsonb_array_elements(positions) elem
                WHERE elem->>'status' = 'OPEN'
            )
        )
    )
    ORDER BY created_at DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;