-- Create the schemas
CREATE SCHEMA IF NOT EXISTS ods;
CREATE SCHEMA IF NOT EXISTS csid;

-----------------------------------------------------------
-- Table: ods.member
-----------------------------------------------------------
CREATE TABLE ods.member (
  memberid BIGINT NOT NULL,
  firstName VARCHAR(255) NOT NULL,
  lastName VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  pass VARCHAR(255) NOT NULL,
  CONSTRAINT PK_member PRIMARY KEY (memberid)
);

-----------------------------------------------------------
-- Table: ods.member_detail
-----------------------------------------------------------
CREATE TABLE ods.member_detail (
  memberid BIGINT NOT NULL,
  occupation CHAR(10),
  address1 CLOB,       -- using CLOB for VARCHAR(max)
  address2 CLOB,
  city CLOB,
  country CLOB,
  zipcode INT,
  phon_number VARCHAR(50),
  user_name CLOB,
  profile_status CHAR(10)
);

ALTER TABLE ods.member_detail
  ADD CONSTRAINT FK_member_detail_member
  FOREIGN KEY (memberid) REFERENCES ods.member(memberid);

-----------------------------------------------------------
-- Table: csid.expense
-----------------------------------------------------------
CREATE TABLE csid.expense (
  memberid BIGINT NOT NULL,
  expensename VARCHAR(255) NOT NULL,
  expensecost VARCHAR(255) NOT NULL
);

ALTER TABLE csid.expense
  ADD CONSTRAINT FK_expense_member
  FOREIGN KEY (memberid) REFERENCES ods.member(memberid);
