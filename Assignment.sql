/* =====================================================================
   HỆ THỐNG QUẢN LÝ ĐỀ TÀI HỌC TẬP - SQL Server
   Chạy trong SSMS / Azure Data Studio. Có thể chạy lại nhiều lần.
   ===================================================================== */

IF DB_ID(N'QuanLyDeTai') IS NULL
    CREATE DATABASE QuanLyDeTai COLLATE Vietnamese_100_CI_AS;
GO
USE QuanLyDeTai;
GO

/* ---------- Xóa bảng cũ (theo thứ tự phụ thuộc) ---------- */
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS feedbacks;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS milestones;
DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS group_members;
DROP TABLE IF EXISTS project_groups;
DROP TABLE IF EXISTS topics;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS lecturers;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS semesters;
DROP TABLE IF EXISTS fields;
DROP TABLE IF EXISTS departments;
GO

/* ---------- 1. Khoa / Bộ môn ---------- */
CREATE TABLE departments (
    id    INT IDENTITY(1,1) PRIMARY KEY,
    name  NVARCHAR(150) NOT NULL UNIQUE
);

/* ---------- 2. Lĩnh vực đề tài ---------- */
CREATE TABLE fields (
    id    INT IDENTITY(1,1) PRIMARY KEY,
    name  NVARCHAR(150) NOT NULL UNIQUE
);

/* ---------- 3. Học kỳ ---------- */
CREATE TABLE semesters (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL UNIQUE,      -- vd: HK1 2026-2027
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    is_current  BIT  NOT NULL DEFAULT 0,
    CONSTRAINT CK_semesters_dates CHECK (end_date > start_date)
);

/* ---------- 4. Tài khoản đăng nhập ---------- */
CREATE TABLE users (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    username       VARCHAR(50)   NOT NULL UNIQUE,
    password_hash  VARCHAR(255)  NOT NULL,          -- BCrypt (Spring Security)
    full_name      NVARCHAR(150) NOT NULL,
    email          VARCHAR(150)  NOT NULL UNIQUE,
    phone          VARCHAR(20)   NULL,
    role           VARCHAR(20)   NOT NULL,
    is_active      BIT           NOT NULL DEFAULT 1,
    created_at     DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CK_users_role CHECK (role IN ('ADMIN','LECTURER','STUDENT'))
);

/* ---------- 5. Giảng viên (hồ sơ) ---------- */
CREATE TABLE lecturers (
    user_id         BIGINT PRIMARY KEY,
    lecturer_code   VARCHAR(20)   NOT NULL UNIQUE,
    department_id   INT           NOT NULL,
    academic_title  NVARCHAR(50)  NULL,              -- ThS, TS, PGS...
    CONSTRAINT FK_lecturers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_lecturers_dept FOREIGN KEY (department_id) REFERENCES departments(id)
);

/* ---------- 6. Sinh viên (hồ sơ) ---------- */
CREATE TABLE students (
    user_id        BIGINT PRIMARY KEY,
    student_code   VARCHAR(20)  NOT NULL UNIQUE,
    class_name     NVARCHAR(50) NULL,
    department_id  INT          NOT NULL,
    CONSTRAINT FK_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_students_dept FOREIGN KEY (department_id) REFERENCES departments(id)
);

/* ---------- 7. Đề tài ---------- */
CREATE TABLE topics (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    code          VARCHAR(20)    NOT NULL UNIQUE,
    title         NVARCHAR(255)  NOT NULL,
    description   NVARCHAR(MAX)  NULL,
    requirements  NVARCHAR(MAX)  NULL,
    field_id      INT            NOT NULL,
    lecturer_id   BIGINT         NOT NULL,
    semester_id   INT            NOT NULL,
    max_groups    INT            NOT NULL DEFAULT 1,   -- số nhóm tối đa được nhận
    max_members   INT            NOT NULL DEFAULT 1,   -- số SV tối đa mỗi nhóm
    status        VARCHAR(20)    NOT NULL DEFAULT 'OPEN',
    created_at    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_topics_field    FOREIGN KEY (field_id)    REFERENCES fields(id),
    CONSTRAINT FK_topics_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturers(user_id),
    CONSTRAINT FK_topics_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
    CONSTRAINT CK_topics_status   CHECK (status IN ('DRAFT','OPEN','CLOSED')),
    CONSTRAINT CK_topics_limits   CHECK (max_groups >= 1 AND max_members >= 1)
);
CREATE INDEX IX_topics_lecturer ON topics(lecturer_id);
CREATE INDEX IX_topics_field    ON topics(field_id);
CREATE INDEX IX_topics_semester ON topics(semester_id, status);

/* ---------- 8. Nhóm sinh viên (đăng ký cá nhân = nhóm 1 người) ---------- */
CREATE TABLE project_groups (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    leader_id   BIGINT        NOT NULL,
    created_at  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_groups_leader FOREIGN KEY (leader_id) REFERENCES students(user_id)
);

CREATE TABLE group_members (
    group_id    BIGINT    NOT NULL,
    student_id  BIGINT    NOT NULL,
    joined_at   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (group_id, student_id),
    CONSTRAINT FK_gm_group   FOREIGN KEY (group_id)   REFERENCES project_groups(id) ON DELETE CASCADE,
    CONSTRAINT FK_gm_student FOREIGN KEY (student_id) REFERENCES students(user_id)
);

/* ---------- 9. Đăng ký đề tài ---------- */
CREATE TABLE registrations (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    topic_id      BIGINT        NOT NULL,
    group_id      BIGINT        NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    note          NVARCHAR(500) NULL,                -- lời nhắn của SV
    reject_reason NVARCHAR(500) NULL,                -- lý do GV từ chối
    registered_at DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    reviewed_at   DATETIME2     NULL,
    CONSTRAINT FK_reg_topic FOREIGN KEY (topic_id) REFERENCES topics(id),
    CONSTRAINT FK_reg_group FOREIGN KEY (group_id) REFERENCES project_groups(id),
    CONSTRAINT UQ_reg_topic_group UNIQUE (topic_id, group_id),
    CONSTRAINT CK_reg_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
);
CREATE INDEX IX_reg_topic_status ON registrations(topic_id, status);
CREATE INDEX IX_reg_group        ON registrations(group_id);

/* ---------- 10. Mốc tiến độ ---------- */
CREATE TABLE milestones (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    registration_id  BIGINT        NOT NULL,
    title            NVARCHAR(200) NOT NULL,
    description      NVARCHAR(MAX) NULL,
    due_date         DATE          NOT NULL,
    progress_percent TINYINT       NOT NULL DEFAULT 0,
    status           VARCHAR(20)   NOT NULL DEFAULT 'NOT_STARTED',
    CONSTRAINT FK_ms_reg FOREIGN KEY (registration_id) REFERENCES registrations(id) ON DELETE CASCADE,
    CONSTRAINT CK_ms_progress CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT CK_ms_status CHECK (status IN ('NOT_STARTED','IN_PROGRESS','SUBMITTED','COMPLETED','OVERDUE'))
);
CREATE INDEX IX_ms_reg ON milestones(registration_id);

/* ---------- 11. Báo cáo nộp (upload file) ---------- */
CREATE TABLE reports (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    milestone_id  BIGINT         NOT NULL,
    submitted_by  BIGINT         NOT NULL,
    version_no    INT            NOT NULL DEFAULT 1,
    file_name     NVARCHAR(255)  NOT NULL,
    file_path     NVARCHAR(500)  NOT NULL,
    file_size     BIGINT         NOT NULL,
    note          NVARCHAR(500)  NULL,
    submitted_at  DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_rep_ms   FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    CONSTRAINT FK_rep_user FOREIGN KEY (submitted_by) REFERENCES students(user_id),
    CONSTRAINT UQ_rep_version UNIQUE (milestone_id, version_no)
);

/* ---------- 12. Nhận xét của giảng viên ---------- */
CREATE TABLE feedbacks (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id    BIGINT        NOT NULL,
    lecturer_id  BIGINT        NOT NULL,
    content      NVARCHAR(MAX) NOT NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_fb_report   FOREIGN KEY (report_id)   REFERENCES reports(id) ON DELETE CASCADE,
    CONSTRAINT FK_fb_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturers(user_id)
);

/* ---------- 13. Chấm điểm (mỗi đăng ký một điểm) ---------- */
CREATE TABLE grades (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    registration_id  BIGINT        NOT NULL UNIQUE,
    lecturer_id      BIGINT        NOT NULL,
    score            DECIMAL(4,2)  NOT NULL,
    comment          NVARCHAR(500) NULL,
    graded_at        DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_gr_reg      FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT FK_gr_lecturer FOREIGN KEY (lecturer_id)     REFERENCES lecturers(user_id),
    CONSTRAINT CK_gr_score    CHECK (score BETWEEN 0 AND 10)
);

/* ---------- 14. Thông báo ---------- */
CREATE TABLE notifications (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    title       NVARCHAR(200) NOT NULL,
    content     NVARCHAR(500) NULL,
    link        VARCHAR(255)  NULL,
    is_read     BIT           NOT NULL DEFAULT 0,
    created_at  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_noti_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IX_noti_user_unread ON notifications(user_id, is_read, created_at DESC);
GO

/* =====================================================================
   DỮ LIỆU MẪU
   Mật khẩu: thay <BCRYPT_HASH> bằng hash sinh từ ứng dụng
   (BCryptPasswordEncoder) hoặc để DataInitializer của Spring Boot tạo.
   ===================================================================== */
INSERT INTO departments(name) VALUES
 (N'Công nghệ thông tin'), (N'Kinh tế'), (N'Điện - Điện tử');

INSERT INTO fields(name) VALUES
 (N'Phát triển web'), (N'Trí tuệ nhân tạo'), (N'Hệ thống nhúng'),
 (N'An toàn thông tin'), (N'Phân tích dữ liệu');

INSERT INTO semesters(name, start_date, end_date, is_current) VALUES
 (N'HK1 2026-2027', '2026-09-01', '2027-01-15', 1);

INSERT INTO users(username, password_hash, full_name, email, role) VALUES
 ('admin',    '<BCRYPT_HASH>', N'Quản trị viên',      'admin@school.edu.vn', 'ADMIN'),
 ('gv001',    '<BCRYPT_HASH>', N'Nguyễn Văn An',      'gv001@school.edu.vn', 'LECTURER'),
 ('sv001',    '<BCRYPT_HASH>', N'Trần Thị Bình',      'sv001@school.edu.vn', 'STUDENT'),
 ('sv002',    '<BCRYPT_HASH>', N'Lê Văn Cường',       'sv002@school.edu.vn', 'STUDENT');

INSERT INTO lecturers(user_id, lecturer_code, department_id, academic_title)
 SELECT id, 'GV001', 1, N'ThS' FROM users WHERE username = 'gv001';

INSERT INTO students(user_id, student_code, class_name, department_id)
 SELECT id, UPPER(username), N'DH22CNTT', 1 FROM users WHERE role = 'STUDENT';

INSERT INTO topics(code, title, description, field_id, lecturer_id, semester_id, max_groups, max_members)
 SELECT 'DT001', N'Xây dựng website quản lý thư viện',
        N'Quản lý sách, độc giả, mượn trả.', 1, u.id, 1, 2, 3
 FROM users u WHERE u.username = 'gv001';
GO

/* =====================================================================
   VIEW & TRUY VẤN HỮU ÍCH
   ===================================================================== */
CREATE OR ALTER VIEW v_topic_overview AS
SELECT  t.id, t.code, t.title, t.status,
        f.name  AS field_name,
        u.full_name AS lecturer_name,
        t.max_groups,
        (SELECT COUNT(*) FROM registrations r
          WHERE r.topic_id = t.id AND r.status = 'APPROVED') AS approved_groups
FROM topics t
JOIN fields f    ON f.id = t.field_id
JOIN users  u    ON u.id = t.lecturer_id;
GO

/* Thống kê cho dashboard: số đề tài theo lĩnh vực */
-- SELECT f.name, COUNT(*) AS so_de_tai
-- FROM topics t JOIN fields f ON f.id = t.field_id
-- GROUP BY f.name;

/* Đề tài còn chỗ nhận đăng ký */
-- SELECT * FROM v_topic_overview
-- WHERE status = 'OPEN' AND approved_groups < max_groups;
