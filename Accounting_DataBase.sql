create database accountingCycle;

CREATE TABLE projects (
    project_id INT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE InitialDetails (
    id INT AUTO_INCREMENT PRIMARY KEY,
    companyName VARCHAR(255) NOT NULL,
    accountingPeriod VARCHAR(50) NOT NULL,   
    monthStarted INT NOT NULL,               
    yearStarted INT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

CREATE TABLE journal_entries (
    entry_id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT,
    transaction_no INT,
    account_name VARCHAR(255),
    debit DECIMAL(18,2),
    credit DECIMAL(18,2),
    description TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(project_id)
);
