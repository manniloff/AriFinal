CREATE DATABASE  IF NOT EXISTS `ussdgate` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `ussdgate`;
-- MySQL dump 10.13  Distrib 5.6.17, for Win32 (x86)
--
-- Host: 127.0.0.1    Database: ussdgate
-- ------------------------------------------------------
-- Server version	5.5.44-0ubuntu0.14.04.1-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ss_map_message_log`
--

DROP TABLE IF EXISTS `ss_map_message_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ss_map_message_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dpc` int(11) NOT NULL,
  `spc` int(11) NOT NULL,
  `in_tstamp` timestamp NULL DEFAULT NULL,
  `out_tstamp` timestamp NULL DEFAULT NULL,
  `invoke_id` int(11) NOT NULL,
  `dialog_id` bigint(20) NOT NULL,
  `msisdn` bigint(20) NOT NULL,
  `ussd_text` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `service_code` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `message_type` enum('processUnstructuredSSRequest_Request','processUnstructuredSSRequest_Response','unstructuredSSRequest_Request','unstructuredSSRequest_Response','unstructuredSSNotify_Request','unstructuredSSNotify_Response','UnstructuredSSResponse') COLLATE utf8_bin DEFAULT NULL,
  `source` enum('http','map','app') COLLATE utf8_bin DEFAULT NULL,
  `initial_dialog_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `dialog_id` (`dialog_id`,`message_type`),
  KEY `initial_dialog_id` (`initial_dialog_id`),
  KEY `msisdn` (`msisdn`,`message_type`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ss_route_rule`
--

DROP TABLE IF EXISTS `ss_route_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ss_route_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `connectionsps` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `destination_address` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `protocol_type` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `service_code` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ussd_text` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ussdsc` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `proxy_mode` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_l96qgfr475vpoqb9c4ifsb1oi` (`ussd_text`,`proxy_mode`),
  UNIQUE KEY `UK_325oqmcx6agp068e8tsi2se1r` (`service_code`,`proxy_mode`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ss_route_rule`
--

LOCK TABLES `ss_route_rule` WRITE;
/*!40000 ALTER TABLE `ss_route_rule` DISABLE KEYS */;
INSERT INTO `ss_route_rule` VALUES (1,NULL,'http://localhost:8888/sms/','HTTP','#608#','#608#','#605#',0),(2,NULL,'http://localhost:8888/sms/','HTTP','608','*608#','#605#',0);
/*!40000 ALTER TABLE `ss_route_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ussdgate_settings`
--

DROP TABLE IF EXISTS `ussdgate_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ussdgate_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `st_type` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `name` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `value` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ussdgate_settings`
--

LOCK TABLES `ussdgate_settings` WRITE;
/*!40000 ALTER TABLE `ussdgate_settings` DISABLE KEYS */;
INSERT INTO `ussdgate_settings` VALUES (1,'map','serviceCenter','22566098088'),(2,'map','opc','735'),(3,'map','dpc','700'),(4,'map','opcssn','8'),(5,'map','dpcssn','7'),(6,'map','gtType','GT0100'),(7,'map','gtNatureOfAddress','INTERNATIONAL'),(8,'map','gtTranslationType','0'),(9,'map','gtNumberingPlan','ISDN_TELEPHONY'),(10,'map','routingIndicator','ROUTING_BASED_ON_GLOBAL_TITLE'),(11,'db','mapMsgWrProc','store_mapMsg'),(12,'db','routingRuleTable','ss_route_rule'),(13,'http','resptimeout','10000'),(14,'app','threads','1000'),(15,'app','forwardFailure','false');
/*!40000 ALTER TABLE `ussdgate_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'ussdgate'
--
/*!50003 DROP PROCEDURE IF EXISTS `store_mapMsg` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `store_mapMsg`(
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
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-02-01 12:08:29
