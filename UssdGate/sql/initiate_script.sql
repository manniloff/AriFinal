create database ussdgate;

grant all on ussdgate.* to ussdgate@'localhost' identified by 'ussdgate';
grant all on ussdgate.* to ussdgate@'127.0.0.1' identified by 'ussdgate';

use ussdgate;

create table ussdgate_settings (
id int  NOT NULL AUTO_INCREMENT,
`st_type` varchar(50),
`name` varchar(50),
`value` varchar(255),
PRIMARY KEY (`id`)
)
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_bin;

CREATE TABLE `ss_map_message_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dpc` int(11) NOT NULL,
  `spc` int(11) NOT NULL,
  `in_tstamp` timestamp NULL DEFAULT NULL,
  `out_tstamp` timestamp NULL DEFAULT NULL,
  `invoke_id` int(11) NOT NULL,
  `dialog_id` bigint(20) NOT NULL,
  `msisdn` bigint NOT NULL,
  `ussd_text` varchar(255) DEFAULT NULL,
  `service_code` varchar(50),
  `message_type` enum('processUnstructuredSSRequest_Request', 'processUnstructuredSSRequest_Response', 'unstructuredSSRequest_Request', 'unstructuredSSRequest_Response', 'unstructuredSSNotify_Request', 'unstructuredSSNotify_Response','UnstructuredSSResponse') DEFAULT NULL,
  `source` enum('http','map','app') DEFAULT NULL,
  `initial_dialog_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `dialog_id` (`dialog_id`,`message_type`),
  KEY `initial_dialog_id` (`initial_dialog_id`),
  KEY `msisdn` (`msisdn`,`message_type`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

DELIMITER ;;
CREATE PROCEDURE `store_mapMsg`(
	in p_dpc int,
	in p_spc int,
	in p_in_tstamp timestamp,
	in p_out_tstamp timestamp,
	in p_invoke_id int,
	in p_dialog_id bigint(20),
	in p_msisdn bigint,
	in p_ussd_text varchar(255),	
	in p_message_type varchar(255),
	in p_source varchar(15),
	in p_initial_dialog_id bigint(20),
	in p_service_code varchar(50)
)
    DETERMINISTIC
BEGIN
  insert into ss_map_message_log(
	dpc,
	spc,
	in_tstamp,
	out_tstamp,
	invoke_id,
	dialog_id,
	msisdn,
	ussd_text,	
	message_type,
	source,
	initial_dialog_id,
	service_code
	) 
  values(
	p_dpc,
	p_spc,
	p_in_tstamp,
	p_out_tstamp,
	p_invoke_id,
	p_dialog_id,
	p_msisdn,
	p_ussd_text,	
	p_message_type,
	p_source,
	p_initial_dialog_id,
	p_service_code
	);
END ;;

CREATE TABLE `ss_route_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `connectionsps` varchar(255) DEFAULT NULL,
  `destination_address` varchar(255) DEFAULT NULL,
  `protocol_type` varchar(255) DEFAULT NULL,
  `service_code` varchar(255) DEFAULT NULL,
  `ussd_text` varchar(255) DEFAULT NULL,
  `ussdsc` varchar(255) DEFAULT NULL,
  `proxy_mode` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_l96qgfr475vpoqb9c4ifsb1oi` (`ussd_text`,`proxy_mode`),
  UNIQUE KEY `UK_325oqmcx6agp068e8tsi2se1r` (`service_code`,`proxy_mode`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


-- Insert some sample data
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'serviceCenter', '99366399113');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'opc', '509');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'dpc', '6110');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'opcssn', '8');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'dpcssn', '8');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtType', 'GT0100');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtNatureOfAddress', 'INTERNATIONAL');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtTranslationType', '0');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtNumberingPlan', 'ISDN_TELEPHONY');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'routingIndicator', 'ROUTING_BASED_ON_GLOBAL_TITLE');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('map', 'addressIndicator', '16');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('db', 'mapMsgWrProc', 'store_mapMsg');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('db', 'routingRuleTable', 'ss_route_rule');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('http', 'resptimeout', '10000');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('app', 'threads', '1000');
INSERT INTO `ussdgate`.`ussdgate_settings` (`st_type`, `name`, `value`) VALUES ('app', 'forwardFailure', 'false');

INSERT INTO `ussdgate`.`ss_route_rule` (`destination_address`, `protocol_type`, `service_code`, `ussd_text`, `proxy_mode`) VALUES ('http://127.0.0.1:7080/UssdGate/test', 'HTTP', '#444#', '#444#', '0');
