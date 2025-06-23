--
-- PostgreSQL database dump
--

-- Dumped from database version 14.17 (Debian 14.17-1.pgdg120+1)
-- Dumped by pg_dump version 14.17 (Debian 14.17-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgroonga; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgroonga WITH SCHEMA public;


--
-- Name: EXTENSION pgroonga; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgroonga IS 'Super fast and all languages supported full text search index based on Groonga';


--
-- Name: employer_type; Type: TYPE; Schema: public; Owner: thanglm2006
--

CREATE TYPE public.employer_type AS ENUM (
    'Company',
    'Shop',
    'Restaurant',
    'Supermarket',
    'Hotel',
    'School',
    'Hospital',
    'Recruiter',
    'Government',
    'NGO',
    'Startup',
    'Event Organizer',
    'Construction',
    'Transportation',
    'Salon',
    'Gym',
    'Farm',
    'Entertainment',
    'E-commerce',
    'individual',
    'Other'
);


ALTER TYPE public.employer_type OWNER TO thanglm2006;

--
-- Name: generate_applicant_id(); Type: FUNCTION; Schema: public; Owner: thanglm2006
--

CREATE FUNCTION public.generate_applicant_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    new.id := 'APP-' || lpad(nextval('applicant_id_seq')::text, 4, '0');
    return new;
end;


$$;


ALTER FUNCTION public.generate_applicant_id() OWNER TO thanglm2006;

--
-- Name: generate_employer_id(); Type: FUNCTION; Schema: public; Owner: thanglm2006
--

CREATE FUNCTION public.generate_employer_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
    new.id := 'EMP-' || lpad(nextval('employer_id_seq')::text, 4, '0');
    return new;
end;    
$$;


ALTER FUNCTION public.generate_employer_id() OWNER TO thanglm2006;

--
-- Name: generate_job_post_id(); Type: FUNCTION; Schema: public; Owner: thanglm2006
--

CREATE FUNCTION public.generate_job_post_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.id := 'JOB-' || LPAD(nextval('job_post_id_seq')::TEXT, 4, '0');
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.generate_job_post_id() OWNER TO thanglm2006;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.admin (
    id bigint NOT NULL,
    full_name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    CONSTRAINT admin_email_check CHECK (((email)::text ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'::text))
);


ALTER TABLE public.admin OWNER TO thanglm2006;

--
-- Name: admin_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.admin_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.admin_id_seq OWNER TO thanglm2006;

--
-- Name: admin_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.admin_id_seq OWNED BY public.admin.id;


--
-- Name: applicant; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.applicant (
    id character varying(255) NOT NULL,
    user_id bigint,
    resume jsonb
);


ALTER TABLE public.applicant OWNER TO thanglm2006;

--
-- Name: applicant_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.applicant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.applicant_id_seq OWNER TO thanglm2006;

--
-- Name: applications; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.applications (
    id bigint NOT NULL,
    job_id character varying(255) NOT NULL,
    applicant_id character varying(255) NOT NULL,
    status character varying(255) DEFAULT 'Pending'::text,
    applied_at timestamp without time zone DEFAULT now(),
    "position" character varying(255) NOT NULL,
    CONSTRAINT applications_status_check CHECK (((status)::text = ANY (ARRAY['Pending'::text, 'Reviewed'::text, 'Accepted'::text, 'Rejected'::text])))
);


ALTER TABLE public.applications OWNER TO thanglm2006;

--
-- Name: applications_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.applications_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.applications_id_seq OWNER TO thanglm2006;

--
-- Name: applications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.applications_id_seq OWNED BY public.applications.id;


--
-- Name: chat_messages; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.chat_messages (
    id bigint NOT NULL,
    sender_id bigint NOT NULL,
    receiver_id bigint NOT NULL,
    message character varying(255) NOT NULL,
    "timestamp" timestamp without time zone DEFAULT now(),
    file_url character varying(255)
);


ALTER TABLE public.chat_messages OWNER TO thanglm2006;

--
-- Name: chat_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.chat_messages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.chat_messages_id_seq OWNER TO thanglm2006;

--
-- Name: chat_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.chat_messages_id_seq OWNED BY public.chat_messages.id;


--
-- Name: employer; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.employer (
    id character varying(255) NOT NULL,
    user_id bigint,
    type character varying(255) NOT NULL
);


ALTER TABLE public.employer OWNER TO thanglm2006;

--
-- Name: employer_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.employer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.employer_id_seq OWNER TO thanglm2006;

--
-- Name: image_folders; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.image_folders (
    id bigint NOT NULL,
    file_name character varying(255) NOT NULL,
    folder_name character varying(255) NOT NULL
);


ALTER TABLE public.image_folders OWNER TO thanglm2006;

--
-- Name: image_folders_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

ALTER TABLE public.image_folders ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.image_folders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: job_post; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.job_post (
    id character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    employer_id character varying(255),
    description jsonb NOT NULL,
    workspace_picture character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT job_post_description_check CHECK ((description ? 'location'::text))
);


ALTER TABLE public.job_post OWNER TO thanglm2006;

--
-- Name: job_post_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.job_post_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.job_post_id_seq OWNER TO thanglm2006;

--
-- Name: job_taken; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.job_taken (
    id bigint NOT NULL,
    job_id character varying(255) NOT NULL,
    applicant_id character varying(255) NOT NULL,
    taken_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.job_taken OWNER TO thanglm2006;

--
-- Name: job_taken_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.job_taken_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.job_taken_id_seq OWNER TO thanglm2006;

--
-- Name: job_taken_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.job_taken_id_seq OWNED BY public.job_taken.id;


--
-- Name: notification; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.notification (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    message character varying(255) NOT NULL,
    is_read boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.notification OWNER TO thanglm2006;

--
-- Name: notification_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.notification_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.notification_id_seq OWNER TO thanglm2006;

--
-- Name: notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.notification_id_seq OWNED BY public.notification.id;


--
-- Name: pending_job_post; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.pending_job_post (
    id bigint NOT NULL,
    title character varying(255) NOT NULL,
    employer_id character varying(255),
    description jsonb NOT NULL,
    workspace_picture character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT pending_job_post_description_check CHECK ((description ? 'location'::text))
);


ALTER TABLE public.pending_job_post OWNER TO thanglm2006;

--
-- Name: pending_job_post_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.pending_job_post_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.pending_job_post_id_seq OWNER TO thanglm2006;

--
-- Name: pending_job_post_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.pending_job_post_id_seq OWNED BY public.pending_job_post.id;


--
-- Name: report_on_post; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.report_on_post (
    id bigint NOT NULL,
    reported_by bigint NOT NULL,
    reported_post character varying(255) NOT NULL,
    reason character varying(255) NOT NULL,
    details character varying(255),
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.report_on_post OWNER TO thanglm2006;

--
-- Name: report_on_post_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.report_on_post_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.report_on_post_id_seq OWNER TO thanglm2006;

--
-- Name: report_on_post_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.report_on_post_id_seq OWNED BY public.report_on_post.id;


--
-- Name: report_on_user; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.report_on_user (
    id bigint NOT NULL,
    reported_by bigint NOT NULL,
    reported_user bigint NOT NULL,
    reason character varying(255) NOT NULL,
    details character varying(255),
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.report_on_user OWNER TO thanglm2006;

--
-- Name: report_on_user_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.report_on_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.report_on_user_id_seq OWNER TO thanglm2006;

--
-- Name: report_on_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.report_on_user_id_seq OWNED BY public.report_on_user.id;


--
-- Name: saved_jobs; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.saved_jobs (
    id bigint NOT NULL,
    applicant_id character varying(255) NOT NULL,
    job_id character varying(255) NOT NULL,
    saved_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.saved_jobs OWNER TO thanglm2006;

--
-- Name: saved_jobs_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.saved_jobs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.saved_jobs_id_seq OWNER TO thanglm2006;

--
-- Name: saved_jobs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.saved_jobs_id_seq OWNED BY public.saved_jobs.id;


--
-- Name: schedule; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.schedule (
    id bigint NOT NULL,
    applicant_id character varying(255) NOT NULL,
    job_id character varying(255) NOT NULL,
    start_time integer NOT NULL,
    end_time integer NOT NULL,
    day character varying(255) NOT NULL,
    description character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT schedule_check CHECK (((end_time > start_time) AND (end_time <= 1439))),
    CONSTRAINT schedule_day_check CHECK (((day)::text = ANY (ARRAY['Monday'::text, 'Tuesday'::text, 'Wednesday'::text, 'Thursday'::text, 'Friday'::text, 'Saturday'::text, 'Sunday'::text]))),
    CONSTRAINT schedule_start_time_check CHECK (((start_time >= 0) AND (start_time <= 1439)))
);


ALTER TABLE public.schedule OWNER TO thanglm2006;

--
-- Name: schedule_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.schedule_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.schedule_id_seq OWNER TO thanglm2006;

--
-- Name: schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.schedule_id_seq OWNED BY public.schedule.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: thanglm2006
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    full_name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    phone character varying(255),
    address character varying(255),
    role character varying(255) NOT NULL,
    avatar_url character varying(255),
    created_at timestamp without time zone DEFAULT now(),
    gender character varying(255),
    date_of_birth timestamp(6) without time zone,
    is_active boolean DEFAULT true,
    CONSTRAINT users_email_check CHECK (((email)::text ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'::text)),
    CONSTRAINT users_phone_check CHECK (((phone)::text ~ '^[0-9]{10,15}$'::text)),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY (ARRAY['Employer'::text, 'Applicant'::text])))
);


ALTER TABLE public.users OWNER TO thanglm2006;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: thanglm2006
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO thanglm2006;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: thanglm2006
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: admin id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.admin ALTER COLUMN id SET DEFAULT nextval('public.admin_id_seq'::regclass);


--
-- Name: applications id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.applications ALTER COLUMN id SET DEFAULT nextval('public.applications_id_seq'::regclass);


--
-- Name: chat_messages id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.chat_messages ALTER COLUMN id SET DEFAULT nextval('public.chat_messages_id_seq'::regclass);


--
-- Name: job_taken id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.job_taken ALTER COLUMN id SET DEFAULT nextval('public.job_taken_id_seq'::regclass);


--
-- Name: notification id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.notification ALTER COLUMN id SET DEFAULT nextval('public.notification_id_seq'::regclass);


--
-- Name: pending_job_post id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.pending_job_post ALTER COLUMN id SET DEFAULT nextval('public.pending_job_post_id_seq'::regclass);


--
-- Name: report_on_post id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_post ALTER COLUMN id SET DEFAULT nextval('public.report_on_post_id_seq'::regclass);


--
-- Name: report_on_user id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_user ALTER COLUMN id SET DEFAULT nextval('public.report_on_user_id_seq'::regclass);


--
-- Name: saved_jobs id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.saved_jobs ALTER COLUMN id SET DEFAULT nextval('public.saved_jobs_id_seq'::regclass);


--
-- Name: schedule id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.schedule ALTER COLUMN id SET DEFAULT nextval('public.schedule_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: admin; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.admin (id, full_name, email, password) FROM stdin;
1	Thang	sungang.20072009@gmail.com	$2a$10$dPx8xUjX/eWFLkNAX8lYpeBEUohMR2FehF5Hf3yKxbtxwEmlFatDS
2	string	xx1@gmail.com	$2a$10$OuuO.Gu9Luj3qsFSKdmVJ.FUO0b1dVwEHZeXoHvXXD5j86NVApwJy
\.


--
-- Data for Name: applicant; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.applicant (id, user_id, resume) FROM stdin;
APP-0011	37	\N
APP-0012	39	\N
APP-0013	41	\N
APP-0014	42	\N
APP-0015	43	\N
APP-0010	35	{"Que quan": "2 ba trung"}
\.


--
-- Data for Name: applications; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.applications (id, job_id, applicant_id, status, applied_at, "position") FROM stdin;
13	JOB-0010	APP-0010	Pending	2025-06-21 04:57:01.814483	nhan vien ban cafe
15	JOB-0014	APP-0014	Pending	2025-06-22 06:32:37.3267	Đại ca
16	JOB-0012	APP-0014	Pending	2025-06-22 13:21:48.262417	Chủ tịch
\.


--
-- Data for Name: chat_messages; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.chat_messages (id, sender_id, receiver_id, message, "timestamp", file_url) FROM stdin;
18	33	35	hi	2025-05-26 05:01:32.341766	\N
19	33	35	where is Danh?	2025-05-26 05:02:08.596854	\N
20	35	33	I don't know	2025-05-26 09:22:57.711903	\N
23	35	33	 	2025-05-26 13:27:06.555272	https://res.cloudinary.com/dqkxxaulc/image/upload/v1748266026/messageIMGs/okx8jdhalkvwye9d6eur.jpg
24	35	33	new virus appear!	2025-05-26 13:41:30.047738	\N
25	35	33	be careful	2025-05-26 17:54:43.645016	\N
26	39	33	hey	2025-06-21 07:44:43.149176	\N
27	42	33	hêlo	2025-06-21 07:46:04.191098	\N
28	42	44	Chào bạnm	2025-06-21 07:55:01.230312	\N
29	44	42	hi	2025-06-21 07:55:39.503045	\N
30	42	44	hihi	2025-06-21 07:55:46.986851	\N
31	44	42	oke	2025-06-21 07:55:49.649611	\N
32	42	44	 	2025-06-21 07:56:16.644534	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492576/messageIMGs/eek6rkejl9rgqzmqry4e.webp
33	42	44	Xin chào bạn	2025-06-21 08:07:22.815748	\N
34	44	42	hi	2025-06-21 08:08:05.413459	\N
35	42	44	hi	2025-06-21 08:08:12.523475	\N
36	35	44	hey boy	2025-06-22 09:00:40.491175	\N
37	42	33	 	2025-06-22 09:45:00.516661	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750585499/messageIMGs/wjzysvuyd5bng0u9kgm0.png
38	33	35	ok	2025-06-22 12:59:35.146319	\N
\.


--
-- Data for Name: employer; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.employer (id, user_id, type) FROM stdin;
EMP-0018	33	Company
EMP-0020	36	Company
EMP-0021	38	Company
EMP-0022	40	Company
EMP-0023	44	Company
\.


--
-- Data for Name: image_folders; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.image_folders (id, file_name, folder_name) FROM stdin;
1	https://res.cloudinary.com/dqkxxaulc/image/upload/v1746022240/sdfvwWed%20Apr%2030%2021:10:38%20ICT%202025/g11bcyes2hvltxm4cybj.jpg	sdfvwWed Apr 30 21:10:38 ICT 2025
2	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750469863/dfsaSADSFDWSDFSVDSat%20Jun%2021%2008:37:41%20ICT%202025/rbuv5gi92fstqqmtvobz.jpg	dfsaSADSFDWSDFSVDSat Jun 21 08:37:41 ICT 2025
3	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750469865/dfsaSADSFDWSDFSVDSat%20Jun%2021%2008:37:41%20ICT%202025/xqgyrab0atliillem9wh.jpg	dfsaSADSFDWSDFSVDSat Jun 21 08:37:41 ICT 2025
4	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750471722/asdasdSat%20Jun%2021%2009:08:40%20ICT%202025/zlb2yiab7ch3dlziejn7.jpg	asdasdSat Jun 21 09:08:40 ICT 2025
5	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492349/Ph%E1%BB%A5c%20v%E1%BB%A5%20y%20t%E1%BA%BFSat%20Jun%2021%2014:52:28%20ICT%202025/itujkpeauinv5rk9aije.jpg	Phục vụ y tếSat Jun 21 14:52:28 ICT 2025
6	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492351/Ph%E1%BB%A5c%20v%E1%BB%A5%20y%20t%E1%BA%BFSat%20Jun%2021%2014:52:28%20ICT%202025/m7u2ithluaz3tmkbyygz.webp	Phục vụ y tếSat Jun 21 14:52:28 ICT 2025
7	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492353/Ph%E1%BB%A5c%20v%E1%BB%A5%20y%20t%E1%BA%BFSat%20Jun%2021%2014:52:28%20ICT%202025/duut4i7ppik2rhuwf9f6.webp	Phục vụ y tếSat Jun 21 14:52:28 ICT 2025
8	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492355/Ph%E1%BB%A5c%20v%E1%BB%A5%20y%20t%E1%BA%BFSat%20Jun%2021%2014:52:34%20ICT%202025/cty63r0ku6epvdr7jjbe.jpg	Phục vụ y tếSat Jun 21 14:52:34 ICT 2025
9	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492357/Ph%E1%BB%A5c%20v%E1%BB%A5%20y%20t%E1%BA%BFSat%20Jun%2021%2014:52:34%20ICT%202025/grqpl4cez8cbpqpoxlbh.webp	Phục vụ y tếSat Jun 21 14:52:34 ICT 2025
10	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750492359/Ph%E1%BB%A5c%20v%E1%BB%A5%20y%20t%E1%BA%BFSat%20Jun%2021%2014:52:34%20ICT%202025/oacom1oomlwsvnis3uki.webp	Phục vụ y tếSat Jun 21 14:52:34 ICT 2025
\.


--
-- Data for Name: job_post; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.job_post (id, title, employer_id, description, workspace_picture, created_at) FROM stdin;
JOB-0008	sdfvw	EMP-0018	{"location": "wsvsv\\t"}	sdfvwWed Apr 30 21:10:38 ICT 2025	2025-04-30 14:10:41.77262
JOB-0009	nothing	EMP-0018	{"location": "nothing\\t"}	\N	2025-04-30 15:05:34.94867
JOB-0010	Tuyển nhân viên	EMP-0018	{"Quán": "Boy cafe", "location": "470, Nam Kì Khởi Nghĩa", "Số điện thoại": "0979162743", "Vị trí tuyển dụng": "nhân viên phục vụ bàn", "Yêu cầu kinh nghiệm": "đã có kinh nghiệm bán cà phê, phục vụ bàn"}	\N	2025-06-15 10:56:09.761654
JOB-0012	Phục vụ y tế	EMP-0023	{"location": "Quận Ngũ Hành Sơn", "description": "Cần người phụ vụ"}	Phục vụ y tếSat Jun 21 14:52:34 ICT 2025	2025-06-21 07:52:40.385159
JOB-0013	Phục vụ y tế	EMP-0023	{"location": "Quận Ngũ Hành Sơn", "description": "Cần người phụ vụ"}	Phục vụ y tếSat Jun 21 14:52:28 ICT 2025	2025-06-21 07:52:34.20939
JOB-0014	Hỗ trợ  quán cf	EMP-0023	{"location": "Đức Phổ", "description": "Cần người hỗ trợ"}	\N	2025-06-21 08:10:05.717687
\.


--
-- Data for Name: job_taken; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.job_taken (id, job_id, applicant_id, taken_at) FROM stdin;
\.


--
-- Data for Name: notification; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.notification (id, user_id, message, is_read, created_at) FROM stdin;
1	33	New application received for job ID JOB-0009 from applicant Lê Thắng	f	2025-05-27 04:48:50.798718
2	33	Application for job ID JOB-0009 has been withdrawn by applicant Lê Thắng	f	2025-05-27 04:50:51.523939
3	33	New application received for job ID JOB-0009 from applicant Lê Thắng	f	2025-05-27 04:52:15.516816
4	33	Application for job ID JOB-0009 has been withdrawn by applicant Lê Thắng	f	2025-05-27 04:52:18.596685
5	33	New application received for job ID JOB-0009 from applicant Lê Thắng	f	2025-05-27 05:04:41.273281
6	33	Application for job ID JOB-0009 has been withdrawn by applicant Lê Thắng	f	2025-05-27 05:04:43.10169
7	33	New application received for job ID JOB-0009 from applicant Lê Thắng	f	2025-05-27 07:50:30.936965
8	33	Application for job ID JOB-0009 has been withdrawn by applicant Lê Thắng	f	2025-05-27 08:41:59.990673
9	33	New application received for job ID JOB-0009 from applicant Lê Thắng	f	2025-05-27 08:42:02.055841
10	33	Application for job ID JOB-0009 has been withdrawn by applicant Lê Thắng	f	2025-05-27 08:42:06.520642
11	33	New application received for job ID JOB-0009 from applicant Lê Thắng	f	2025-05-27 08:42:10.38573
12	33	Application for job ID JOB-0009 has been withdrawn by applicant Lê Thắng	f	2025-05-27 09:25:14.370317
13	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: Tuyển nhân viên	f	2025-05-28 01:46:09.286838
14	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: Tuyển nhân viên	f	2025-06-15 10:56:09.878148
15	33	Bài đăng của bạn đã được duyệt: Tuyển nhân viên	f	2025-06-15 11:08:25.695936
16	33	Bài đăng của bạn đã được duyệt: Tuyển nhân viên	f	2025-06-18 17:51:14.011795
17	33	New application received for job ID JOB-0010 from applicant Lê Thắng	f	2025-06-20 08:50:03.330666
18	33	Application for job ID JOB-0010 has been withdrawn by applicant Lê Thắng	f	2025-06-20 08:50:22.249756
19	33	New application received for job ID JOB-0010 from applicant Thành Danh	f	2025-06-20 09:42:59.175797
20	33	Application for job ID JOB-0010 has been withdrawn by applicant Thành Danh	f	2025-06-20 13:39:22.007744
21	33	New application received for job ID JOB-0010 from applicant Thành Danh	f	2025-06-20 13:39:22.484929
22	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: dfsaSADSFDWSDFSVD	f	2025-06-21 01:37:45.820468
23	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: asdasd	f	2025-06-21 02:08:43.284646
24	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: wef	f	2025-06-21 02:11:05.499383
25	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: sdfsdf	f	2025-06-21 02:14:27.479658
26	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: dfsa	f	2025-06-21 02:17:23.083005
27	33	New application received for job ID JOB-0010 from applicant Lê Thắng	f	2025-06-21 04:37:40.683
28	33	Application for job ID JOB-0010 has been withdrawn by applicant Lê Thắng	f	2025-06-21 04:37:45.26839
29	33	New application received for job ID JOB-0010 from applicant Lê Thắng	f	2025-06-21 04:39:24.112604
30	33	Application for job ID JOB-0010 has been withdrawn by applicant Lê Thắng	f	2025-06-21 04:39:25.338463
31	33	New application received for job ID JOB-0010 from applicant Lê Thắng	f	2025-06-21 04:44:36.896699
32	33	Application for job ID JOB-0010 has been withdrawn by applicant Lê Thắng	f	2025-06-21 04:44:44.862135
33	33	New application received for job ID JOB-0010 from applicant Lê Thắng	f	2025-06-21 04:57:01.854479
34	33	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: asas	f	2025-06-21 07:17:21.429932
35	33	Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: asas	f	2025-06-21 07:18:14.641178
36	33	Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: dfsa	f	2025-06-21 07:18:16.659907
37	33	Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: sdfsdf	f	2025-06-21 07:18:17.479639
38	33	Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: wef	f	2025-06-21 07:18:18.029669
39	33	Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: asdasd	f	2025-06-21 07:18:19.461422
40	33	Bài đăng của bạn đã bị xóa do không tuân theo các quy định về nội dung: dfsaSADSFDWSDFSVD	f	2025-06-21 07:18:20.974707
41	33	New application received for job ID JOB-0010 from applicant Thành Danh	f	2025-06-21 07:36:11.690486
42	33	Application for job ID JOB-0010 has been withdrawn by applicant Thành Danh	f	2025-06-21 07:36:25.70252
43	44	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: Phục vụ y tế	f	2025-06-21 07:52:34.215338
44	44	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: Phục vụ y tế	f	2025-06-21 07:52:40.390829
45	44	Bài đăng của bạn đã được duyệt: Phục vụ y tế	f	2025-06-21 07:53:40.573373
46	44	Bài đăng của bạn đã được duyệt: Phục vụ y tế	f	2025-06-21 07:53:47.643824
47	44	Bài đăng của bạn đã được gửi đi và đang chờ duyệt: Hỗ trợ  quán cf	f	2025-06-21 08:10:05.723727
48	44	Bài đăng của bạn đã được duyệt: Hỗ trợ  quán cf	f	2025-06-21 08:10:57.719671
49	44	New application received for job ID JOB-0014 from applicant Thành Danh	f	2025-06-22 06:32:37.331448
50	44	New application received for job ID JOB-0012 from applicant Thành Danh	f	2025-06-22 13:21:48.267419
\.


--
-- Data for Name: pending_job_post; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.pending_job_post (id, title, employer_id, description, workspace_picture, created_at) FROM stdin;
\.


--
-- Data for Name: report_on_post; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.report_on_post (id, reported_by, reported_post, reason, details, created_at) FROM stdin;
\.


--
-- Data for Name: report_on_user; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.report_on_user (id, reported_by, reported_user, reason, details, created_at) FROM stdin;
\.


--
-- Data for Name: saved_jobs; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.saved_jobs (id, applicant_id, job_id, saved_at) FROM stdin;
3	APP-0010	JOB-0009	2025-05-27 05:12:58.996758
340	APP-0012	JOB-0008	2025-06-20 11:15:27.544171
343	APP-0010	JOB-0010	2025-06-20 11:16:25.447218
354	APP-0012	JOB-0010	2025-06-20 15:22:37.793884
356	APP-0014	JOB-0010	2025-06-20 23:14:18.587189
359	APP-0014	JOB-0009	2025-06-21 07:51:16.974505
360	APP-0014	JOB-0012	2025-06-21 08:06:43.026766
361	APP-0014	JOB-0014	2025-06-22 06:32:31.849612
362	APP-0010	JOB-0014	2025-06-22 13:29:54.604173
\.


--
-- Data for Name: schedule; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.schedule (id, applicant_id, job_id, start_time, end_time, day, description, created_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: thanglm2006
--

COPY public.users (id, full_name, email, password, phone, address, role, avatar_url, created_at, gender, date_of_birth, is_active) FROM stdin;
37	Tgha	sdcf@gmail.com	$2a$10$bP.6W8f4Iv7mGN1W1eq4fenKyZL/yCU.N1Y2B39G7vbr7IRJjRt5u	\N	\N	Applicant	\N	2025-06-19 02:57:58.568766	Nam	\N	f
38	Tgha	sdcfq@gmail.com	$2a$10$BTOIjJDwJrUiarqCh8MOTOgjGj5L3NPy46GCIOBj6YrXlG1egmhYe	\N	\N	Employer	\N	2025-06-19 02:58:26.852333	Nam	\N	f
40	Thành Danh Hanma	xx2@gmail.com	$2a$10$GbDVhf5cLG10kI2LR1dVUuCJSqsLzej8NLjuo4NhlltCoyGgK87bW	\N	\N	Employer	\N	2025-06-20 10:42:41.528987	Nam	\N	f
41	JAV	a@a.a	$2a$10$UQoAnHrLUKtYdoYW.azdfuU3RVf56IvauhXoJPjb/p9xsqAAGca8y	\N	\N	Applicant	\N	2025-06-20 12:46:31.746556	Nam	\N	f
36	Danh	danhnth.24ai@vku.udn.vn	$2a$10$aMwhEYvx3urM6JcL8bcn2eC9D73gM66ch5rPYBGw2R3VIS5AO5YaO	\N	\N	Employer	\N	2025-05-26 19:10:14.623135	male	2026-10-03 07:00:00	f
42	Thành Danh	x1@gmail.com	$2a$10$oHbLuyHhwzZ4yaPOtyhP.uHGYVDG960nEgFeI19qUYxJi.YESmHl2	\N	\N	Applicant	\N	2025-06-20 21:50:36.263478	Nam	\N	t
43	Thành Danh	x2@gmail.com	$2a$10$BVLFfjoN3s098LUnGiYLzeaNxX2gD67Zi4KcT00skZZEZcleMJ.Gu	\N	\N	Applicant	\N	2025-06-20 23:17:40.215547	Nam	\N	t
44	Thành Danh	x3@gmail.com	$2a$10$.UWYzlqGXER83tObLT9rku34HpXUXGXeHNc124qY0zo0vpxMR9Ux2	\N	\N	Employer	\N	2025-06-20 23:18:11.73409	Nam	\N	t
39	Thành Danh	xx1@gmail.com	$2a$10$wUYzs7JvNNpgOPar5lukrOm67k1lKsB.qZsfDqtt.ItIqUc9WjvCm	\N	\N	Applicant	\N	2025-06-20 09:38:06.189769	Nam	\N	t
33	Lê Mạnh Thắng	sungang.20072009@gmail.com	$2a$10$YC03k5f.6JurXDFIKJdqfOfdQcaU3iJ8HCtCUcQxKqPzNTGJLuFry	\N	\N	Employer	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750580739/messageIMGs/ot8y2qbnhp06zgvj7cpj.png	2025-04-30 08:08:31.338024	\N	\N	t
35	Lê Thắng	thanglm.24ai@vku.udn.vn	$2a$10$jueVCYuU4Vp0qMkFiDqnjOUBlfi66iTn5h5f2mRLmiN/vsRlviVOq	\N	\N	Applicant	https://res.cloudinary.com/dqkxxaulc/image/upload/v1750581643/messageIMGs/jooacrj2oaw4lxduslyt.jpg	2025-05-25 11:13:51.131559	male	2006-03-22 07:00:00	t
\.


--
-- Name: admin_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.admin_id_seq', 2, true);


--
-- Name: applicant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.applicant_id_seq', 15, true);


--
-- Name: applications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.applications_id_seq', 16, true);


--
-- Name: chat_messages_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.chat_messages_id_seq', 38, true);


--
-- Name: employer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.employer_id_seq', 23, true);


--
-- Name: image_folders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.image_folders_id_seq', 10, true);


--
-- Name: job_post_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.job_post_id_seq', 14, true);


--
-- Name: job_taken_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.job_taken_id_seq', 1, false);


--
-- Name: notification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.notification_id_seq', 50, true);


--
-- Name: pending_job_post_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.pending_job_post_id_seq', 11, true);


--
-- Name: report_on_post_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.report_on_post_id_seq', 1, false);


--
-- Name: report_on_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.report_on_user_id_seq', 1, false);


--
-- Name: saved_jobs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.saved_jobs_id_seq', 363, true);


--
-- Name: schedule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.schedule_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: thanglm2006
--

SELECT pg_catalog.setval('public.users_id_seq', 44, true);


--
-- Name: admin admin_email_key; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT admin_email_key UNIQUE (email);


--
-- Name: admin admin_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT admin_pkey PRIMARY KEY (id);


--
-- Name: applicant applicant_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.applicant
    ADD CONSTRAINT applicant_pkey PRIMARY KEY (id);


--
-- Name: applications applications_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_pkey PRIMARY KEY (id);


--
-- Name: chat_messages chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);


--
-- Name: employer employer_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.employer
    ADD CONSTRAINT employer_pkey PRIMARY KEY (id);


--
-- Name: image_folders image_folders_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.image_folders
    ADD CONSTRAINT image_folders_pkey PRIMARY KEY (id);


--
-- Name: job_post job_post_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.job_post
    ADD CONSTRAINT job_post_pkey PRIMARY KEY (id);


--
-- Name: job_taken job_taken_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.job_taken
    ADD CONSTRAINT job_taken_pkey PRIMARY KEY (id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: pending_job_post pending_job_post_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.pending_job_post
    ADD CONSTRAINT pending_job_post_pkey PRIMARY KEY (id);


--
-- Name: report_on_post report_on_post_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_post
    ADD CONSTRAINT report_on_post_pkey PRIMARY KEY (id);


--
-- Name: report_on_user report_on_user_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_user
    ADD CONSTRAINT report_on_user_pkey PRIMARY KEY (id);


--
-- Name: saved_jobs saved_jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT saved_jobs_pkey PRIMARY KEY (id);


--
-- Name: schedule schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_phone_key; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_key UNIQUE (phone);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: pgroonga_index; Type: INDEX; Schema: public; Owner: thanglm2006
--

CREATE INDEX pgroonga_index ON public.job_post USING pgroonga (((((title)::text || ' '::text) || description)));


--
-- Name: applicant applicant_id_trigger; Type: TRIGGER; Schema: public; Owner: thanglm2006
--

CREATE TRIGGER applicant_id_trigger BEFORE INSERT ON public.applicant FOR EACH ROW EXECUTE FUNCTION public.generate_applicant_id();


--
-- Name: employer employer_id_trigger; Type: TRIGGER; Schema: public; Owner: thanglm2006
--

CREATE TRIGGER employer_id_trigger BEFORE INSERT ON public.employer FOR EACH ROW EXECUTE FUNCTION public.generate_employer_id();


--
-- Name: job_post job_post_id_trigger; Type: TRIGGER; Schema: public; Owner: thanglm2006
--

CREATE TRIGGER job_post_id_trigger BEFORE INSERT ON public.job_post FOR EACH ROW EXECUTE FUNCTION public.generate_job_post_id();


--
-- Name: applicant applicant_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.applicant
    ADD CONSTRAINT applicant_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: applications applications_applicant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_applicant_id_fkey FOREIGN KEY (applicant_id) REFERENCES public.applicant(id) ON DELETE CASCADE;


--
-- Name: applications applications_job_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.applications
    ADD CONSTRAINT applications_job_id_fkey FOREIGN KEY (job_id) REFERENCES public.job_post(id) ON DELETE CASCADE;


--
-- Name: chat_messages chat_messages_receiver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_receiver_id_fkey FOREIGN KEY (receiver_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: chat_messages chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.chat_messages
    ADD CONSTRAINT chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: employer employer_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.employer
    ADD CONSTRAINT employer_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: job_post job_post_employer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.job_post
    ADD CONSTRAINT job_post_employer_id_fkey FOREIGN KEY (employer_id) REFERENCES public.employer(id) ON DELETE CASCADE;


--
-- Name: job_taken job_taken_applicant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.job_taken
    ADD CONSTRAINT job_taken_applicant_id_fkey FOREIGN KEY (applicant_id) REFERENCES public.applicant(id) ON DELETE CASCADE;


--
-- Name: job_taken job_taken_job_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.job_taken
    ADD CONSTRAINT job_taken_job_id_fkey FOREIGN KEY (job_id) REFERENCES public.job_post(id) ON DELETE CASCADE;


--
-- Name: notification notification_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: pending_job_post pending_job_post_employer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.pending_job_post
    ADD CONSTRAINT pending_job_post_employer_id_fkey FOREIGN KEY (employer_id) REFERENCES public.employer(id) ON DELETE CASCADE;


--
-- Name: report_on_post report_on_post_reported_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_post
    ADD CONSTRAINT report_on_post_reported_by_fkey FOREIGN KEY (reported_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: report_on_post report_on_post_reported_post_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_post
    ADD CONSTRAINT report_on_post_reported_post_fkey FOREIGN KEY (reported_post) REFERENCES public.job_post(id) ON DELETE CASCADE;


--
-- Name: report_on_user report_on_user_reported_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_user
    ADD CONSTRAINT report_on_user_reported_by_fkey FOREIGN KEY (reported_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: report_on_user report_on_user_reported_user_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.report_on_user
    ADD CONSTRAINT report_on_user_reported_user_fkey FOREIGN KEY (reported_user) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: saved_jobs saved_jobs_applicant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT saved_jobs_applicant_id_fkey FOREIGN KEY (applicant_id) REFERENCES public.applicant(id) ON DELETE CASCADE;


--
-- Name: saved_jobs saved_jobs_job_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.saved_jobs
    ADD CONSTRAINT saved_jobs_job_id_fkey FOREIGN KEY (job_id) REFERENCES public.job_post(id) ON DELETE CASCADE;


--
-- Name: schedule schedule_applicant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT schedule_applicant_id_fkey FOREIGN KEY (applicant_id) REFERENCES public.applicant(id) ON DELETE CASCADE;


--
-- Name: schedule schedule_job_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: thanglm2006
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT schedule_job_id_fkey FOREIGN KEY (job_id) REFERENCES public.job_post(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

