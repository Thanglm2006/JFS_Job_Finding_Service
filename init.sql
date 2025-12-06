/* Install PGroonga extension */
CREATE EXTENSION IF NOT EXISTS pgroonga;

/* Create ENUM type */
CREATE TYPE employer_type AS ENUM (
    'Company', 'Shop', 'Restaurant', 'Supermarket', 'Hotel',
    'School', 'Hospital', 'Recruiter', 'Government', 'NGO',
    'Startup', 'Event Organizer', 'Construction', 'Transportation',
    'Salon', 'Gym', 'Farm', 'Entertainment', 'E-commerce', 'Individual', 'Other'
);

/* Create tables with constraints */
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name TEXT NOT NULL CHECK (full_name ~ '^[\p{L} ]+$'),
    email TEXT UNIQUE NOT NULL CHECK (
        email ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'
    ),
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

CREATE OR REPLACE FUNCTION check_minimum_age()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if date_of_birth is later than 15 years ago
    IF NEW.date_of_birth > (CURRENT_DATE - INTERVAL '15 years') THEN
        RAISE EXCEPTION 'Violation of constraint: User must be at least 15 years old.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_user_age
BEFORE INSERT OR UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION check_minimum_age();

CREATE TABLE employer (
    id TEXT PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    type employer_type NOT NULL,
    custom_type TEXT,
    org TEXT not null
);

CREATE TABLE admin (
    id SERIAL PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL CHECK (
        email ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'
    ),
    password TEXT NOT NULL
);

CREATE TABLE applicant (
    id TEXT PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    resume JSONB
);

CREATE TABLE job_post (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    employer_id TEXT REFERENCES employer(id) ON DELETE CASCADE,
    description JSONB NOT NULL CHECK (description ? 'location'),
    workspace_picture TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE pending_job_post (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    employer_id TEXT REFERENCES employer(id) ON DELETE CASCADE,
    description JSONB NOT NULL CHECK (description ? 'location'),
    workspace_picture TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
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

CREATE TABLE report_on_user (
    id SERIAL PRIMARY KEY,
    reported_by INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
    status TEXT CHECK (status IN ('Pending', 'Reviewed', 'Accepted', 'Rejected')) DEFAULT 'Pending',
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

/* Create sequences for ID generation */
CREATE SEQUENCE employer_id_seq START 1;
CREATE SEQUENCE applicant_id_seq START 1;
CREATE SEQUENCE job_post_id_seq START 1;

/* Create trigger functions */
CREATE FUNCTION generate_employer_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'EMP-' || LPAD(nextval('employer_id_seq')::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION generate_applicant_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'APP-' || LPAD(nextval('applicant_id_seq')::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION generate_job_post_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'JOB-' || LPAD(nextval('job_post_id_seq')::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/* Create triggers */
CREATE TRIGGER employer_id_trigger
    BEFORE INSERT ON employer
    FOR EACH ROW
    EXECUTE FUNCTION generate_employer_id();

CREATE TRIGGER applicant_id_trigger
    BEFORE INSERT ON applicant
    FOR EACH ROW
    EXECUTE FUNCTION generate_applicant_id();

CREATE TRIGGER job_post_id_trigger
    BEFORE INSERT ON job_post
    FOR EACH ROW
    EXECUTE FUNCTION generate_job_post_id();

/* Create PGroonga index */
CREATE INDEX IF NOT EXISTS pgroonga_index ON job_post USING pgroonga ((title || ' ' || description));

/* Disable sequential scans for PGroonga optimization */
SET enable_seqscan = OFF;
