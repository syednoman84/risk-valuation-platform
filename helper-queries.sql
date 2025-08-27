-------------------------------------
-- Assumptions Queries
-------------------------------------
select * from assumption_set;
select * from assumption_file;
select * from assumption_value;

-------------------------------------
-- Model Management Queries
-------------------------------------
select * from loan_models order by created_at desc;

-------------------------------------
-- Position File Queries
-------------------------------------
select * from position_file order by uploaded_at desc;
select * from position_file where id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from loan where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from payment_schedule where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from rate_schedule where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from custom_fields where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';

-------------------------------------
-- Model Execution Queries
-------------------------------------
select * from model_executions order by created_at desc;
select * from model_execution_errors where execution_id='5ae653b7-ea68-491b-9105-c17c0eacd674';
select * from model_execution_chunks where execution_id='5ae653b7-ea68-491b-9105-c17c0eacd674';
select * from execution_results where execution_id='5ae653b7-ea68-491b-9105-c17c0eacd674';

-------------------------------------
-- Drop Create queries
-------------------------------------
-- drop database rvp_position_db;
-- drop database rvp_model_db;
-- drop database rvp_assumption_db;
-- drop database rvp_model_execution_db;
-- create database rvp_position_db;
-- create database rvp_model_db;
-- create database rvp_assumption_db;
-- create database rvp_model_execution_db;
