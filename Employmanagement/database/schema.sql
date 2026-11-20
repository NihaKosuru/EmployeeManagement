-- Employee Management API - MySQL 8 schema and sample data
-- Run with:  mysql -u root -p < database/schema.sql
-- Warning: this drops and recreates the employee_db tables.

CREATE DATABASE IF NOT EXISTS employee_db;
USE employee_db;

DROP TABLE IF EXISTS project_assignments;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    version  BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_department_name UNIQUE (name)
);

CREATE TABLE employees (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)   NOT NULL,
    email         VARCHAR(150)   NOT NULL,
    designation   VARCHAR(100)   NOT NULL,
    salary        DECIMAL(12, 2) NOT NULL,
    department_id BIGINT         NOT NULL,
    version       BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_email UNIQUE (email),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES departments (id),
    INDEX idx_employee_name (name),
    INDEX idx_employee_department (department_id)
);

CREATE TABLE projects (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)   NOT NULL,
    description VARCHAR(500),
    budget      DECIMAL(15, 2) NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_project_name UNIQUE (name)
);

CREATE TABLE project_assignments (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id   BIGINT       NOT NULL,
    project_id    BIGINT       NOT NULL,
    role          VARCHAR(100) NOT NULL,
    assigned_date DATE         NOT NULL,
    PRIMARY KEY (id),
    -- An employee can be assigned to the same project only once
    CONSTRAINT uk_employee_project UNIQUE (employee_id, project_id),
    CONSTRAINT fk_assignment_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    INDEX idx_assignment_project (project_id)
);

-- Sample data

INSERT INTO departments (id, name, location) VALUES
    (1, 'Engineering', 'Bengaluru'),
    (2, 'Human Resources', 'Hyderabad'),
    (3, 'Finance', 'Mumbai');

INSERT INTO employees (id, name, email, designation, salary, department_id) VALUES
    (1, 'Arjun Sharma',   'arjun.sharma@example.com',   'Senior Software Engineer', 1800000.00, 1),
    (2, 'Priya Reddy',    'priya.reddy@example.com',    'Software Engineer',        1200000.00, 1),
    (3, 'Rahul Verma',    'rahul.verma@example.com',    'Tech Lead',                2400000.00, 1),
    (4, 'Sneha Iyer',     'sneha.iyer@example.com',     'HR Manager',               1500000.00, 2),
    (5, 'Karthik Nair',   'karthik.nair@example.com',   'Recruiter',                 800000.00, 2),
    (6, 'Ananya Gupta',   'ananya.gupta@example.com',   'Financial Analyst',        1100000.00, 3);

INSERT INTO projects (id, name, description, budget) VALUES
    (1, 'Payroll System',   'Monthly payroll processing for all employees', 5000000.00),
    (2, 'Hiring Portal',    'Online portal for job applications',           3000000.00),
    (3, 'Expense Tracker',  'Tracking of employee expense claims',          2000000.00);

INSERT INTO project_assignments (employee_id, project_id, role, assigned_date) VALUES
    (1, 1, 'Backend Developer', '2025-01-15'),
    (3, 1, 'Technical Lead',    '2025-01-10'),
    (6, 1, 'Business Analyst',  '2025-02-01'),
    (2, 2, 'Backend Developer', '2025-03-01'),
    (4, 2, 'Product Owner',     '2025-03-01'),
    (2, 3, 'Backend Developer', '2025-04-05');
