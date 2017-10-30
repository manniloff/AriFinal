create database CheckSubSig;

grant all on CheckSubSig.* to checksubsig@'localhost' identified by 'sigcheckSub4$';
grant all on CheckSubSig.* to checksubsig@'127.0.0.1' identified by 'sigcheckSub4$';

use CheckSubSig;
create table settings (
id int AUTO_INCREMENT,
`st_type` varchar(50),
`name` varchar(50),
`value` varchar(255),
PRIMARY KEY (`id`)
)
DEFAULT CHARACTER SET = utf8
COLLATE = utf8_bin;


INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('app', 'threads', '100');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('app', 'alerturl', 'http://localhost:7080/test');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'serviceCenter', '22505987070');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'spc', '5072');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'spcssn', '6');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'dpc', '6201');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'dpcssn', '6');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'ri', 'ROUTING_BASED_ON_GLOBAL_TITLE');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'an', 'international_number');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'np', 'ISDN');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'npi', 'ISDNTelephoneNumberingPlan');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'ton', 'InternationalNumber');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtmscNOA', 'INTERNATIONAL');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtmscType', 'GT0100');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtmscTT', '0');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtmscNP', 'ISDN_TELEPHONY');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gtmscDigits', '22505987777');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gthlrNOA', 'INTERNATIONAL');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gthlrType', 'GT0100');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gthlrTT', '0');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gthlrNP', 'ISDN_TELEPHONY');
INSERT INTO `CheckSubSig`.`settings` (`st_type`, `name`, `value`) VALUES ('map', 'gthlrDigits', '22505987666');
