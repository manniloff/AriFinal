create database BeepCallSig;

grant all on BeepCallSig.* to beepcallsig@'localhost' identified by 'beepcallsig';

use BeepCallSig;

create table beeps (
id bigint AUTO_INCREMENT,
session_id int,
cic int,
dpc int,
calling_party bigint, -- msisdnA
called_party bigint, -- msisdnB
`timestamp` timestamp,
`message_type` enum('IAM','REL','CPG','ACM'),
statu_indicator tinyint,
cause_indicator tinyint,
PRIMARY KEY (`id`),
INDEX beeps_ind (`session_id`, `cic`)
)
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_bin;
ALTER TABLE `BeepCallSig`.`beeps` 
CHANGE COLUMN `message_type` `message_type` ENUM('IAM','REL','CPG','ACM','ANM','RLC') CHARACTER SET 'utf8' COLLATE 'utf8_bin' NULL DEFAULT NULL ;

create table isup_settings (
id int AUTO_INCREMENT,
`st_type` varchar(50),
`name` varchar(50),
`value` varchar(255),
PRIMARY KEY (`id`)
)
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_bin;



DELIMITER ;;
CREATE PROCEDURE BeepCallSig.`store_isup_events`(
	in p_session_id int,
	in p_cic int,
	in p_dpc int,
	in p_calling_party bigint, -- msisdnA
	in p_called_party bigint, -- msisdnB
	in p_timestamp timestamp,
	in p_message_type varchar(5),
	in p_statu_indicator tinyint,
	in p_cause_indicator tinyint
)
    DETERMINISTIC
BEGIN
  insert into BeepCallSig.beeps(
	session_id,
	cic,
	dpc,
	calling_party,
	called_party,
	`timestamp`,
	message_type,
	statu_indicator,
	cause_indicator) 
  values(
	p_session_id,
	p_cic,
	p_dpc,
	p_calling_party,
	p_called_party,
	p_timestamp,
	p_message_type,
	p_statu_indicator,
	p_cause_indicator);
END ;;

DELIMITER ;;
CREATE PROCEDURE BeepCallSig.`store_isup_calls`(
	in p_start_date timestamp,
	in P_end_date timestamp,
	in p_calling_party bigint, -- msisdnA
	in p_called_party bigint, -- msisdnB
	in p_reason int
)
    DETERMINISTIC
BEGIN
  insert into `BeepCall`.`calls_archive`(
	start_date,
	end_date,
	CallingNumber,
	CalledNumber,
	reason) 
  values(
	p_start_date,
	P_end_date,
	p_calling_party,
	p_called_party,
	p_reason);
END ;;
