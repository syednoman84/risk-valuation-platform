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
select * from rvp_position_db.position_file order by uploaded_at desc;
select * from rvp_position_db.position_file where id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from rvp_position_db.loan where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from rvp_position_db.payment_schedule where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from rvp_position_db.rate_schedule where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';
select * from rvp_position_db.custom_fields where position_file_id='adbabb35-2dda-4eb9-a59b-aa796841e478';

-------------------------------------
-- Model Execution Queries
-------------------------------------
select * from model_executions order by created_at desc;
select * from model_execution_errors where execution_id='970842c5-f740-476f-9131-04a006694def';
select * from model_execution_chunks where execution_id='970842c5-f740-476f-9131-04a006694def';
select * from execution_results where execution_id='a9d41676-c2c7-49a3-af5f-dd91e6a64980';

-------------------------------------
-- Drop Create queries
-------------------------------------
-- set statements
set search_path to postgres;
set search_path to rvp_position_db;
set search_path to rvp_model_db;
set search_path to rvp_assumption_db;
set search_path to rvp_model_execution_db;
-- drop database rvp_position_db;
-- drop database rvp_model_db;
-- drop database rvp_assumption_db;
-- drop database rvp_model_execution_db;
create database rvp_position_db;
create database rvp_model_db;
create database rvp_assumption_db;
create database rvp_model_execution_db;
