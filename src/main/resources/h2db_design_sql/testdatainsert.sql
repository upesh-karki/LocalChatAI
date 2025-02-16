INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (1, 'John', 'Doe', 'john.doe@example.com', 'password1');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (2, 'Jane', 'Smith', 'jane.smith@example.com', 'password2');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (3, 'Michael', 'Brown', 'michael.brown@example.com', 'password3');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (4, 'Emily', 'Jones', 'emily.jones@example.com', 'password4');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (5, 'David', 'Wilson', 'david.wilson@example.com', 'password5');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (6, 'Sarah', 'Taylor', 'sarah.taylor@example.com', 'password6');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (7, 'Robert', 'Miller', 'robert.miller@example.com', 'password7');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (8, 'Linda', 'Davis', 'linda.davis@example.com', 'password8');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (9, 'William', 'Garcia', 'william.garcia@example.com', 'password9');

INSERT INTO ods.member (memberid, firstName, lastName, email, pass) VALUES
  (10, 'Barbara', 'Martinez', 'barbara.martinez@example.com', 'password10');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (1, 'Engineer  ', '123 Main St', 'Apt 101', 'New York', 'USA', 10001, '555-0101', 'johndoe', 'Active');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (2, 'Designer  ', '456 Oak Ave', 'Suite 202', 'Los Angeles', 'USA', 90001, '555-0202', 'janesmith', 'Active');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (3, 'Manager   ', '789 Pine Rd', NULL, 'Chicago', 'USA', 60007, '555-0303', 'michaelbrown', 'Inactive');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (4, 'Developer ', '321 Elm St', 'Floor 3', 'Houston', 'USA', 77001, '555-0404', 'emilyjones', 'Active');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (5, 'Analyst   ', '654 Maple Dr', NULL, 'Phoenix', 'USA', 85001, '555-0505', 'davidwilson', 'Active');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (6, 'Consultant', '987 Cedar Ln', 'Suite 10', 'Philadelphia', 'USA', 19019, '555-0606', 'sarahtaylor', 'Inactive');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (7, 'Architect ', '159 Spruce St', 'Apt 7B', 'San Antonio', 'USA', 78205, '555-0707', 'robertmiller', 'Active');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (8, 'Technician', '753 Birch Blvd', 'Unit 3', 'San Diego', 'USA', 92101, '555-0808', 'lindadavis', 'Active');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (9, 'Operator  ', '852 Walnut St', NULL, 'Dallas', 'USA', 75201, '555-0909', 'williamgarcia', 'Inactive');

INSERT INTO ods.member_detail (memberid, occupation, address1, address2, city, country, zipcode, phon_number, user_name, profile_status)
VALUES (10, 'Director  ', '951 Chestnut Ave', 'Office 12', 'San Jose', 'USA', 95101, '555-1010', 'barbaramartinez', 'Active');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (1, 'Groceries', '75.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (2, 'Utilities', '120.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (3, 'Rent', '950.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (4, 'Internet', '60.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (5, 'Transportation', '40.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (6, 'Dining Out', '85.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (7, 'Entertainment', '50.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (8, 'Healthcare', '200.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (9, 'Clothing', '100.00');

INSERT INTO csid.expense (memberid, expensename, expensecost)
VALUES (10, 'Miscellaneous', '30.00');

