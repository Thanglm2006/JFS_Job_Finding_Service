CREATE TYPE employer_type AS ENUM(
    'Company', 'Shop', 'Restaurant', 'Supermarket', 'Hotel', 
    'School', 'Hospital', 'Recruiter', 'Government', 'NGO', 
    'Startup', 'Event Organizer', 'Construction', 'Transportation', 
    'Salon', 'Gym', 'Farm', 'Entertainment', 'E-commerce','individual'
);


CREATE TABLE users(
    id SERIAL PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL CHECK (      email ~  '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'
        ),
    password TEXT NOT NULL,
    phone TEXT UNIQUE CHECK (phone ~ '^[0-9]{10,15}$'),
    address text,
    role TEXT CHECK (role IN ('Employer', 'Applicant')) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

create table employer(
    id text primary key,
    user_id int references users(id) on delete cascade,
    type employer_type not null
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
create table image_folders(
    id serial primary key,
    folder_name text not null,
    file_name text not null
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
    timestamp TIMESTAMP DEFAULT NOW() -- Time message was sent
);


