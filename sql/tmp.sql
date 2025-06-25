/* install pgroonga first*/

CREATE TYPE employer_type AS ENUM(
    'Company', 'Shop', 'Restaurant', 'Supermarket', 'Hotel', 
    'School', 'Hospital', 'Recruiter', 'Government', 'NGO', 
    'Startup', 'Event Organizer', 'Construction', 'Transportation', 
    'Salon', 'Gym', 'Farm', 'Entertainment', 'E-commerce','individual','Other'
);


CREATE TABLE users(
    id SERIAL PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL CHECK (      email ~  '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'
        ),
    password TEXT NOT NULL,
    phone TEXT UNIQUE CHECK (phone ~ '^[0-9]{10,15}$'),
    address text,
    gender text check (gender in ('male', 'female', 'other')) not null,
    date_of_birth DATE,
    role TEXT CHECK (role IN ('Employer', 'Applicant')) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    is_active BOOLEAN DEFAULT TRUE
);

create table employer(
    id text primary key,
    user_id int references users(id) on delete cascade,
    type employer_type not null,
    custom_type text
);
create table admin(
    id serial primary key,
    full_name Text NOT NULL,
    email TEXT UNIQUE NOT NULL CHECK (      email ~  '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'
        ),
    password TEXT NOT NULL
);

create sequence employer_id_seq start 1;
create function generate_employer_id() returns trigger as $$
begin
    new.id := 'EMP-' || lpad(nextval('employer_id_seq')::text, 4, '0');
    return new;
end;    
$$ language plpgsql;
create trigger employer_id_trigger
before insert on employer
for each row
execute function generate_employer_id();

create table applicant(
    id text primary key,
    user_id int references users(id) on delete cascade,
    resume jsonB
);

create sequence applicant_id_seq start 1;
create function generate_applicant_id() returns trigger as $$
begin
    new.id := 'APP-' || lpad(nextval('applicant_id_seq')::text, 4, '0');
    return new;
end;


$$ language plpgsql;
create trigger applicant_id_trigger
before insert on applicant
for each row
execute function generate_applicant_id();


create table job_post(
    id text primary key,
    title text not null,
    employer_id text references employer(id) on delete cascade,
    description jsonB not null check (description ? 'location'),
    workspace_picture text,
    created_at timestamp not null default now()
);
-- CREATE EXTENSION IF NOT EXISTS pgroonga;
-- CREATE INDEX pgroonga_index ON job_post USING pgroonga ((title || ' ' || description));
--SELECT * FROM job_post WHERE (title || ' ' || description) &@~ 'software engineer';


create table pending_job_post(
    id serial primary key,
    title text not null,
    employer_id text references employer(id) on delete cascade,
    description jsonB not null check (description ? 'location'),
    workspace_picture text,
    created_at timestamp not null default now()
);

create table notification(
    id serial primary key,
    user_id int not null references users(id) on delete cascade,
    message text not null,
    is_read boolean default false,
    created_at timestamp default now()
);

create table image_folders(
    id serial primary key,
    folder_name text not null,
    file_name text not null
);

create table job_taken(
    id serial primary key,
    job_id text not null references job_post(id) on delete cascade,
    applicant_id text not null references applicant(id) on delete cascade,
    taken_at timestamp default now()
);

create table schedule(
    id serial primary key,
    applicant_id text not null references applicant(id) on delete cascade,
    job_id text not null references job_post(id) on delete cascade,
    start_time int not null check(start_time >= 0 and start_time <= 1439),
    end_time int not null check(end_time > start_time and end_time <= 1439),
    day text not null check(day in ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')),
    description text not null,
    created_at timestamp default now()
);

create table report_on_user(
    id serial primary key,
    reported_by int not null references users(id) on delete cascade,
    reported_user int not null references users(id) on delete cascade,
    reason text not null,
    details text,
    created_at timestamp default now()
);
create table report_on_post(
    id serial primary key,
    reported_by int not null references users(id) on delete cascade,
    reported_post text not null references job_post(id) on delete cascade,
    reason text not null,
    details text,
    created_at timestamp default now()
);

CREATE SEQUENCE job_post_id_seq START 1;
CREATE FUNCTION generate_job_post_id() RETURNS TRIGGER AS $$
BEGIN
    NEW.id := 'JOB-' || LPAD(nextval('job_post_id_seq')::TEXT, 4, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER job_post_id_trigger
BEFORE INSERT ON job_post
FOR EACH ROW
EXECUTE FUNCTION generate_job_post_id();

CREATE TABLE applications(
    id SERIAL PRIMARY KEY,
    job_id TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    applicant_id text NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    status TEXT CHECK (status IN ('Pending', 'Reviewed', 'Accepted', 'Rejected')) DEFAULT 'Pending',
    position TEXT NOT NULL,
    applied_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE saved_jobs (
    id SERIAL PRIMARY KEY,
    applicant_id text NOT NULL REFERENCES applicant(id) ON DELETE CASCADE,
    job_id TEXT NOT NULL REFERENCES job_post(id) ON DELETE CASCADE,
    saved_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE chat_messages(
    id SERIAL PRIMARY KEY,
    sender_id INT NOT NULL references users(id) on delete cascade,      -- User sending the message
    receiver_id INT NOT NULL references users(id) on delete cascade,    -- User receiving the message
    message TEXT NOT NULL,       -- Chat message
    file_url text,
    timestamp TIMESTAMP DEFAULT NOW() -- Time message was sent
);

SET enable_seqscan = off;
