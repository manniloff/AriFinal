-- MySQL dump 10.13  Distrib 5.6.24, for Win64 (x86_64)
--
-- Host: 10.5.1.1    Database: smsgateway
-- ------------------------------------------------------
-- Server version	5.5.46-0ubuntu0.14.04.2-log

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
-- Table structure for table `alert_waiting_list`
--

DROP TABLE IF EXISTS `alert_waiting_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alert_waiting_list` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `dcs` tinyint(4) unsigned NOT NULL,
  `pid` tinyint(4) unsigned NOT NULL,
  `inserted` timestamp NULL DEFAULT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `priority` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `segmentLen` enum('160','140','70','153','134','67') COLLATE utf8_unicode_ci DEFAULT '160',
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `sendSMAttempts` tinyint(4) unsigned NOT NULL,
  `nextAttempt` timestamp NULL DEFAULT NULL,
  `alertType` enum('1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '1',
  `errorState` enum('6','10') COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `archive_tables_scripts`
--

DROP TABLE IF EXISTS `archive_tables_scripts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `archive_tables_scripts` (
  `tableName` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `createScript` text COLLATE utf8_unicode_ci,
  `dropScript` text COLLATE utf8_unicode_ci,
  `insertQuery` text COLLATE utf8_unicode_ci,
  `deleteQuery` text COLLATE utf8_unicode_ci,
  `processed` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`tableName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `archive_tables_scripts`
--

LOCK TABLES `archive_tables_scripts` WRITE;
/*!40000 ALTER TABLE `archive_tables_scripts` DISABLE KEYS */;
INSERT INTO `archive_tables_scripts` VALUES ('mo_incoming_log','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_mo_incoming_log (messageId bigint(20) unsigned NOT NULL,systemId int(11) unsigned NOT NULL,fromAD varchar(15) COLLATE utf8_unicode_ci NOT NULL,fromTON enum(\'0\',\'1\',\'2\',\'3\',\'4\',\'5\',\'6\',\'7\') COLLATE utf8_unicode_ci NOT NULL,fromNP enum(\'0\',\'1\',\'3\',\'4\',\'5\',\'6\',\'7\',\'8\',\'9\',\'15\') COLLATE utf8_unicode_ci NOT NULL,toAD bigint(20) NOT NULL,toAN enum(\'0\',\'1\',\'2\',\'3\',\'4\',\'5\',\'6\',\'7\') COLLATE utf8_unicode_ci NOT NULL,toNP enum(\'0\',\'1\',\'3\',\'4\',\'5\',\'6\',\'7\',\'8\',\'9\',\'15\') COLLATE utf8_unicode_ci NOT NULL,message varchar(8000) COLLATE utf8_unicode_ci NOT NULL,dcs tinyint(3) unsigned NOT NULL,pid tinyint(3) unsigned NOT NULL,occurred timestamp NULL DEFAULT NULL,PRIMARY KEY (messageId)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;','DROP TABLE IF EXISTS smsgateway_archive.%mo_incoming_log;','INSERT smsgateway_archive.%_mo_incoming_log(messageId,systemId,fromAD,fromTON,fromNP,toAD,toAN,toNP,message,dcs,pid,occurred) SELECT messageId,systemId,fromAD,fromTON,fromNP,toAD,toAN,toNP,message,dcs,pid,occurred FROM mo_incoming_log;','DELETE FROM mo_incoming_log;','2016-05-12 06:10:37'),('mtfsm_request','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_mtfsm_request (messageId bigint(20) NOT NULL,systemId tinyint(4) unsigned DEFAULT NULL,toAD bigint(20) NOT NULL,addedInQueue timestamp NULL DEFAULT NULL,KEY IX_%_MTFS_REQ_MESSAGEID (messageId), INDEX IX_%_MTFS_REQ_ADDEDINQUEUE (addedInQueue)) ENGINE=InnoDB;','DROP TABLE IF EXISTS smsgateway_archive.%_mtfsm_request;','INSERT smsgateway_archive.%_mtfsm_request SELECT messageId, systemId, toAD, addedInQueue FROM mtfsm_request;','DELETE FROM mtfsm_request;','2016-05-12 06:10:38'),('mtfsm_response','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_mtfsm_response (messageId bigint(20) NOT NULL,systemId tinyint(4) unsigned DEFAULT NULL,toAD bigint(20) NOT NULL,errorCode tinyint(3) unsigned NOT NULL, errorMessage varchar(256) NULL, receivedResponse TIMESTAMP NULL DEFAULT NULL,  dialogId bigint(20) NULL,KEY IX_%_MTFSM_RES_MESSAGEID (messageId),INDEX IX_%_MTFSM_RES_RECEIVEDRESPONSE (receivedResponse)) ENGINE=InnoDB;','DROP TABLE IF EXISTS smsgateway_archive.%_mtfsm_response;','INSERT smsgateway_archive.%_mtfsm_response SELECT messageId,systemId,toAD,errorCode,errorMessage,receivedResponse,dialogId FROM mtfsm_response;','DELETE FROM mtfsm_response;','2016-05-12 06:10:38'),('reportsmds_request','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_reportsmds_request (messageId bigint(20) NOT NULL, systemId tinyint(4) unsigned DEFAULT NULL, toAD BIGINT(20) NOT NULL, addedInQueue TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY IX_%_REPORTSMDS_REQ_MESSAGEID (messageId), INDEX IX_%_REPORTSMDS_REQ_ADDEDINQUEUE (addedInQueue)) ENGINE=InnoDB;','DROP TABLE IF EXISTS smsgateway_archive.%_reportsmds_request;','INSERT smsgateway_archive.%_reportsmds_request SELECT messageId,systemId,toAD,addedInQueue FROM reportsmds_request;','DELETE FROM reportsmds_request;','2016-05-12 06:10:38'),('reportsmds_response','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_reportsmds_response (messageId bigint(20) NOT NULL, systemId tinyint(4) unsigned NOT NULL, errorCode tinyint(3) unsigned DEFAULT NULL, errorMessage varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL, receivedResponse timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, dialogId bigint(20) NOT NULL, KEY IX_%_REPORTSMDS_RES_MESSAGEID (messageId), INDEX IX_%_REPORTSMDS_RES_RECEIVEDRESPONSE(receivedResponse)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;','DROP TABLE IF EXISTS smsgateway_archive.%_reportsmds_response;','INSERT smsgateway_archive.%_reportsmds_response SELECT messageId,systemId,errorCode,errorMessage,receivedResponse,dialogId FROM reportsmds_response;','DELETE FROM reportsmds_response;','2016-05-12 06:10:39'),('smpp_dlr_logs','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_smpp_dlr_logs (messageId bigint(20) NOT NULL, started timestamp NULL DEFAULT NULL, finished timestamp NULL DEFAULT NULL, errorMessage varchar(256) DEFAULT NULL, KEY IX_%_DLRLOGS_MESSAGEID (messageId), INDEX IX_%_DLRLOGS_STARTED (started) ) ENGINE=InnoDB;','DROP TABLE IF EXISTS smsgateway_archive.%_smpp_dlr_logs;','INSERT smsgateway_archive.%_smpp_dlr_logs(messageId, started, finished, errorMessage) SELECT messageId, started, finished, errorMessage FROM smpp_dlr_logs;','DELETE FROM smpp_dlr_logs;','2016-05-12 06:10:39'),('smpp_dlr_sended_log','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_smpp_dlr_sended_log (messageId bigint(20) unsigned NOT NULL,attempts tinyint(4) unsigned NOT NULL,sendedDLR timestamp NULL DEFAULT NULL,PRIMARY KEY (messageId)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;','DROP TABLE IF EXISTS smsgateway_archive.%_smpp_dlr_sended_log;','INSERT smsgateway_archive.%_smpp_dlr_sended_log(messageId,attempts,sendedDLR) SELECT messageId,attempts,sendedDLR FROM smpp_dlr_sended_log;','DELETE FROM smpp_dlr_sended_log;','2016-05-12 06:10:39'),('smpp_error_logs','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_smpp_error_logs (id bigint(20) NOT NULL AUTO_INCREMENT,systemId tinyint(4) unsigned NOT NULL,errorCode smallint(5) DEFAULT NULL,errorMessage varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,destAddr bigint(20) NOT NULL,sequenceNumber int(11) NOT NULL,sourceAddr varchar(15) COLLATE utf8_unicode_ci NOT NULL,occurred datetime NOT NULL,PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;','DROP TABLE IF EXISTS smsgateway_archive.%_smpp_error_logs;','INSERT smsgateway_archive.%_smpp_error_logs(id,systemId,errorCode,errorMessage,destAddr,sequenceNumber,sourceAddr,occurred) SELECT id,systemId,errorCode,errorMessage,destAddr,sequenceNumber,sourceAddr,occurred FROM smpp_error_logs;','DELETE FROM smpp_error_logs;','2016-05-12 06:10:39'),('smpp_incoming_dlr_log','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_smpp_incoming_dlr_log (messageId bigint(20) NOT NULL,receivedDLR timestamp NULL DEFAULT NULL,sendDLRUntil timestamp NULL DEFAULT NULL,state enum(\'2\',\'3\',\'5\',\'8\',\'9\') COLLATE utf8_unicode_ci NOT NULL DEFAULT \'8\',  sendSMAttempts tinyint(3) unsigned NOT NULL,  PRIMARY KEY (messageId)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;','DROP TABLE IF EXISTS smsgateway_archive.%_smpp_incoming_dlr_log;','INSERT smsgateway_archive.%_smpp_incoming_dlr_log(messageId, receivedDLR, sendDLRUntil, state, sendSMAttempts) SELECT messageId,receivedDLR,sendDLRUntil,state,sendSMAttempts FROM smpp_incoming_dlr_log;','DELETE FROM smpp_incoming_dlr_log','2016-05-12 06:10:39'),('smpp_mo_response_log','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_smpp_mo_response_log (messageId bigint(20) unsigned NOT NULL, accessId int(11) unsigned NOT NULL,state enum(\'DELIVRD\',\'EXPIRED\',\'DELETED\',\'UNDELIV\',\'ACCEPTED\',\'UNKNOWN\',\'REJECTD\') COLLATE utf8_unicode_ci NOT NULL,errorMessage varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,started timestamp NULL DEFAULT NULL,finished timestamp NULL DEFAULT NULL,PRIMARY KEY (messageId)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;','DROP TABLE IF EXISTS smsgateway_archive.%_smpp_mo_response_log;','INSERT smsgateway_archive.%_smpp_mo_response_log (messageId,accessId,state,errorMessage,started,finished) select messageId,accessId,state,errorMessage,started,finished FROM smpp_mo_response_log;','DELETE FROM smpp_mo_response_log;','2016-05-12 06:10:40'),('sms_archive','','','','call spClearSmsArchive(30);','2016-05-12 06:10:40'),('srism_request','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_srism_request (messageId bigint(20) NOT NULL, systemId tinyint(4) unsigned NOT NULL, toAD bigint(20) NOT NULL, addedInQueue timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY IX_%_SRISM_REQ_MESSAGEID (messageId), INDEX IX_%_SRISM_REQ_ADDEDINQUEUE (addedInQueue))ENGINE=InnoDB;','DROP TABLE IF EXISTS smsgateway_archive.%_srism_request;','INSERT smsgateway_archive.%_srism_request SELECT messageId,systemId,toAD,addedInQueue FROM srism_request;','DELETE FROM srism_request;','2016-05-12 06:10:40'),('srism_response','CREATE TABLE IF NOT EXISTS smsgateway_archive.%_srism_response (messageId bigint(20) NOT NULL, dialogId bigint(20) NOT NULL, errorCode tinyint(3) unsigned DEFAULT NULL, errorMessage varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,receivedResponse timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, mscAddress BIGINT(20) unsigned DEFAULT NULL,  KEY IX_%_SRISM_RESPONSE_MESSAGEID (messageId),INDEX IX_%_SRISM_RESPONSE_RECEIVEDRESPONSE (receivedResponse)) ENGINE=InnoDB;','DROP TABLE IF EXISTS smsgateway_archive.%_srism_response;','INSERT smsgateway_archive.%_srism_response SELECT messageId,dialogId,errorCode,errorMessage,receivedResponse, mscAddress FROM srism_response;','DELETE FROM srism_response;','2016-05-12 06:10:41');
/*!40000 ALTER TABLE `archive_tables_scripts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `content_providers`
--

DROP TABLE IF EXISTS `content_providers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `content_providers` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `provider_name` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `creation_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_modification_date` timestamp NULL DEFAULT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `comment` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_UNIQUE` (`provider_name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `content_providers`
--

LOCK TABLES `content_providers` WRITE;
/*!40000 ALTER TABLE `content_providers` DISABLE KEYS */;
INSERT INTO `content_providers` VALUES (1,'Test-unifun','2016-04-18 14:42:21',NULL,'','test client');
/*!40000 ALTER TABLE `content_providers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `content_providers_access`
--

DROP TABLE IF EXISTS `content_providers_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `content_providers_access` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `content_providers_id` int(10) unsigned NOT NULL,
  `login` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(8) COLLATE utf8_unicode_ci NOT NULL,
  `host` varchar(15) COLLATE utf8_unicode_ci NOT NULL DEFAULT '127.0.0.1',
  `ton` enum('0','1') COLLATE utf8_unicode_ci NOT NULL DEFAULT '1',
  `np` enum('0','1') COLLATE utf8_unicode_ci NOT NULL DEFAULT '1',
  `speed_limit` tinyint(3) unsigned NOT NULL,
  `sms_parts` tinyint(3) unsigned NOT NULL,
  `sms_type` enum('HIDDEN','REGULAR','ANY') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'ANY',
  `change_submit_date_type` enum('CANCEL','REPLACE','ANY','NO_ACTIVE') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'NO_ACTIVE',
  `can_send_sms_from_time` time DEFAULT NULL,
  `can_send_sms_to_time` time DEFAULT NULL,
  `days_of_week` enum('ANY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY','WORKINGDAYS','WEEKEND') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'ANY',
  `direction` enum('ANY','MT','MO') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'MT',
  `access_type` enum('HTTP','SMPP') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'SMPP',
  `expired_type` enum('EXPIREDIN','EXPIREDAT') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'EXPIREDAT',
  `delivery_type` enum('DELIVERYIN','DELIVERYAT') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'DELIVERYAT',
  `queue_size` smallint(5) unsigned NOT NULL,
  `default_sms_live_timeInMin` smallint(5) unsigned DEFAULT NULL,
  `creation_datetime` timestamp NULL DEFAULT NULL,
  `last_modification_datetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  `comment` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `login_UNIQUE` (`login`),
  KEY `FK_CONTENT_PROVIDERS_ID_idx` (`content_providers_id`),
  CONSTRAINT `FK_CONTENT_PROVIDERS_ID` FOREIGN KEY (`content_providers_id`) REFERENCES `content_providers` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `content_providers_access`
--

LOCK TABLES `content_providers_access` WRITE;
/*!40000 ALTER TABLE `content_providers_access` DISABLE KEYS */;
INSERT INTO `content_providers_access` VALUES (1,1,'unifun','un!B@lk','10.160.9.12','0','0',50,10,'ANY','ANY',NULL,NULL,'ANY','MT','SMPP','EXPIREDIN','DELIVERYIN',50,180,'2016-04-25 15:49:19','2016-06-21 13:56:55','','SMS BULK');
/*!40000 ALTER TABLE `content_providers_access` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `content_providers_access_charset_restrictions`
--

DROP TABLE IF EXISTS `content_providers_access_charset_restrictions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `content_providers_access_charset_restrictions` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `accessId` int(11) unsigned DEFAULT NULL,
  `charset` enum('GSM7','GSM8','UCS2') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'GSM7',
  `enabled` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `charset_UNIQUE` (`accessId`,`charset`),
  CONSTRAINT `FK_CPA_CHARSET_RESTRICTIONS_ACCESSID` FOREIGN KEY (`accessId`) REFERENCES `content_providers_access` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `content_providers_access_charset_restrictions`
--

LOCK TABLES `content_providers_access_charset_restrictions` WRITE;
/*!40000 ALTER TABLE `content_providers_access_charset_restrictions` DISABLE KEYS */;
INSERT INTO `content_providers_access_charset_restrictions` VALUES (1,1,'GSM7',''),(2,1,'UCS2',''),(6,1,'GSM8','');
/*!40000 ALTER TABLE `content_providers_access_charset_restrictions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `content_providers_access_restrictions`
--

DROP TABLE IF EXISTS `content_providers_access_restrictions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `content_providers_access_restrictions` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `content_providers_access_id` int(10) unsigned NOT NULL,
  `source_address` varchar(15) COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_ton` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci DEFAULT NULL,
  `source_np` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci DEFAULT NULL,
  `dest_ton` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci DEFAULT NULL,
  `dest_np` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_CONTENT_PROVIDERS_ACCESS_ID_idx` (`content_providers_access_id`),
  CONSTRAINT `FK_CONTENT_PROVIDERS_ACCESS_ID` FOREIGN KEY (`content_providers_access_id`) REFERENCES `content_providers_access` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `content_providers_access_restrictions`
--

LOCK TABLES `content_providers_access_restrictions` WRITE;
/*!40000 ALTER TABLE `content_providers_access_restrictions` DISABLE KEYS */;
INSERT INTO `content_providers_access_restrictions` VALUES (1,1,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `content_providers_access_restrictions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `data_coding_list`
--

DROP TABLE IF EXISTS `data_coding_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `data_coding_list` (
  `dcsId` tinyint(3) unsigned NOT NULL,
  `charset` enum('GSM7','GSM8','UCS2','ISO-8859-1','US-ASCII','UTF-8','UTF-16','UTF-16BE','UTF-16LE') COLLATE utf8_unicode_ci NOT NULL,
  `compressed` bit(1) NOT NULL,
  `enabled` bit(1) NOT NULL,
  UNIQUE KEY `dcsId_UNIQUE` (`dcsId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data_coding_list`
--

LOCK TABLES `data_coding_list` WRITE;
/*!40000 ALTER TABLE `data_coding_list` DISABLE KEYS */;
INSERT INTO `data_coding_list` VALUES (0,'US-ASCII','\0',''),(1,'US-ASCII','\0',''),(2,'US-ASCII','\0',''),(3,'ISO-8859-1','\0',''),(4,'UTF-8','\0',''),(5,'UTF-8','\0',''),(6,'UTF-8','\0',''),(7,'UTF-8','\0',''),(8,'UTF-16','\0',''),(9,'UTF-16','\0',''),(10,'UTF-16','\0',''),(11,'UTF-16','\0',''),(16,'US-ASCII','\0',''),(17,'US-ASCII','\0',''),(18,'US-ASCII','\0',''),(19,'US-ASCII','\0',''),(20,'UTF-8','\0',''),(21,'UTF-8','\0',''),(22,'UTF-8','\0',''),(23,'UTF-8','\0',''),(24,'UTF-16','\0',''),(25,'UTF-16','\0',''),(26,'UTF-16','\0',''),(27,'UTF-16','\0',''),(64,'US-ASCII','\0',''),(65,'US-ASCII','\0',''),(66,'US-ASCII','\0',''),(67,'US-ASCII','\0',''),(68,'UTF-8','\0',''),(69,'UTF-8','\0',''),(70,'UTF-8','\0',''),(71,'UTF-8','\0',''),(72,'UTF-16','\0',''),(73,'UTF-16','\0',''),(74,'UTF-16','\0',''),(75,'UTF-16','\0',''),(80,'US-ASCII','\0',''),(81,'US-ASCII','\0',''),(82,'US-ASCII','\0',''),(83,'US-ASCII','\0',''),(84,'UTF-8','\0',''),(85,'UTF-8','\0',''),(86,'UTF-8','\0',''),(87,'UTF-8','\0',''),(88,'UTF-16','\0',''),(89,'UTF-16','\0',''),(90,'UTF-16','\0',''),(91,'UTF-16','\0',''),(208,'US-ASCII','\0',''),(209,'US-ASCII','\0',''),(210,'US-ASCII','\0',''),(211,'US-ASCII','\0',''),(212,'US-ASCII','\0',''),(213,'US-ASCII','\0',''),(214,'US-ASCII','\0',''),(215,'US-ASCII','\0',''),(216,'US-ASCII','\0',''),(217,'US-ASCII','\0',''),(218,'US-ASCII','\0',''),(219,'US-ASCII','\0',''),(220,'US-ASCII','\0',''),(221,'US-ASCII','\0',''),(222,'US-ASCII','\0',''),(223,'US-ASCII','\0',''),(224,'UTF-16','\0',''),(225,'UTF-16','\0',''),(226,'UTF-16','\0',''),(227,'UTF-16','\0',''),(228,'UTF-16','\0',''),(229,'UTF-16','\0',''),(230,'UTF-16','\0',''),(231,'UTF-16','\0',''),(232,'UTF-16','\0',''),(233,'UTF-16','\0',''),(234,'UTF-16','\0',''),(235,'UTF-16','\0',''),(236,'UTF-16','\0',''),(237,'UTF-16','\0',''),(238,'UTF-16','\0',''),(239,'UTF-16','\0',''),(240,'US-ASCII','\0',''),(241,'US-ASCII','\0',''),(242,'US-ASCII','\0',''),(243,'US-ASCII','\0',''),(244,'UTF-8','\0',''),(245,'UTF-8','\0',''),(246,'UTF-8','\0',''),(247,'UTF-8','\0',''),(248,'US-ASCII','\0',''),(249,'US-ASCII','\0',''),(250,'US-ASCII','\0',''),(251,'US-ASCII','\0',''),(252,'UTF-8','\0',''),(253,'UTF-8','\0',''),(254,'UTF-8','\0',''),(255,'UTF-8','\0','');
/*!40000 ALTER TABLE `data_coding_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `entity_reset_dlrqueue`
--

DROP TABLE IF EXISTS `entity_reset_dlrqueue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `entity_reset_dlrqueue` (
  `messageId` bigint(20) unsigned NOT NULL,
  `systemId` int(11) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(15) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `state` enum('2','3','5','8','9') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `dcs` tinyint(4) NOT NULL,
  `attempts` tinyint(4) NOT NULL,
  `inserted` timestamp NULL DEFAULT NULL,
  `nextAttempt` timestamp NULL DEFAULT NULL,
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL,
  `receivedDLR` timestamp NULL DEFAULT NULL,
  `sendDLRUntil` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `entity_reset_dlrqueue`
--

LOCK TABLES `entity_reset_dlrqueue` WRITE;
/*!40000 ALTER TABLE `entity_reset_dlrqueue` DISABLE KEYS */;
/*!40000 ALTER TABLE `entity_reset_dlrqueue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `erroneous_requets_log`
--

DROP TABLE IF EXISTS `erroneous_requets_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `erroneous_requets_log` (
  `messageId` bigint(20) NOT NULL,
  `errormessage` longtext COLLATE utf8_unicode_ci NOT NULL,
  `methodtype` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `occurred` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `systemId` tinyint(4) NOT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `erroneous_requets_log`
--

LOCK TABLES `erroneous_requets_log` WRITE;
/*!40000 ALTER TABLE `erroneous_requets_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `erroneous_requets_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `hibernate_sequence`
--

DROP TABLE IF EXISTS `hibernate_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hibernate_sequence` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hibernate_sequence`
--

LOCK TABLES `hibernate_sequence` WRITE;
/*!40000 ALTER TABLE `hibernate_sequence` DISABLE KEYS */;
/*!40000 ALTER TABLE `hibernate_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mo_incoming_log`
--

DROP TABLE IF EXISTS `mo_incoming_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mo_incoming_log` (
  `messageId` bigint(20) unsigned NOT NULL,
  `systemId` int(11) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `dcs` tinyint(3) unsigned NOT NULL,
  `pid` tinyint(3) unsigned NOT NULL,
  `occurred` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mo_incoming_log`
--

LOCK TABLES `mo_incoming_log` WRITE;
/*!40000 ALTER TABLE `mo_incoming_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `mo_incoming_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mo_routing_rules`
--

DROP TABLE IF EXISTS `mo_routing_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mo_routing_rules` (
  `address` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `access_id` int(11) unsigned NOT NULL,
  `enabled` bit(1) NOT NULL,
  PRIMARY KEY (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mo_routing_rules`
--

LOCK TABLES `mo_routing_rules` WRITE;
/*!40000 ALTER TABLE `mo_routing_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `mo_routing_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `msisdn_black_list`
--

DROP TABLE IF EXISTS `msisdn_black_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `msisdn_black_list` (
  `msisdn` bigint(20) NOT NULL,
  `direction` enum('ANY','MT','MO') COLLATE utf8_unicode_ci NOT NULL DEFAULT 'ANY',
  `started` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`msisdn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msisdn_black_list`
--

LOCK TABLES `msisdn_black_list` WRITE;
/*!40000 ALTER TABLE `msisdn_black_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `msisdn_black_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mtfsm_request`
--

DROP TABLE IF EXISTS `mtfsm_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mtfsm_request` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned DEFAULT NULL,
  `toAD` bigint(20) NOT NULL,
  `addedInQueue` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `IX_MTFS_REQ_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mtfsm_response`
--

DROP TABLE IF EXISTS `mtfsm_response`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mtfsm_response` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `errorCode` tinyint(3) unsigned DEFAULT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  `receivedResponse` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dialogId` bigint(20) NOT NULL,
  KEY `IX_MTFSM_RES_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `next_attempt_waiting_list`
--

DROP TABLE IF EXISTS `next_attempt_waiting_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `next_attempt_waiting_list` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `dcs` tinyint(4) unsigned NOT NULL,
  `pid` tinyint(4) unsigned NOT NULL,
  `inserted` timestamp NULL DEFAULT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `priority` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `segmentLen` enum('160','140','70','153','134','67') COLLATE utf8_unicode_ci DEFAULT '160',
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `sendSMAttempts` tinyint(4) unsigned NOT NULL,
  `nextAttempt` timestamp NULL DEFAULT NULL,
  `errorState` tinyint(4) unsigned DEFAULT NULL,
  `handledType` enum('1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '1',
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `next_attempt_waiting_list`
--

LOCK TABLES `next_attempt_waiting_list` WRITE;
/*!40000 ALTER TABLE `next_attempt_waiting_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `next_attempt_waiting_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `number_reg_ex`
--

DROP TABLE IF EXISTS `number_reg_ex`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `number_reg_ex` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `expression` varchar(128) COLLATE utf8_unicode_ci NOT NULL,
  `ton` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `np` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `check_for_source_add` bit(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `number_reg_ex`
--

LOCK TABLES `number_reg_ex` WRITE;
/*!40000 ALTER TABLE `number_reg_ex` DISABLE KEYS */;
INSERT INTO `number_reg_ex` VALUES (1,'^[a-zA-Z0-9_]{3,8}$','5','0',''),(2,'^[0-9]{12,15}$','1','1',''),(3,'^[0-9]{3,8}$','3','0',''),(4,'^[a-c*#0-9]{3,8}$','0','0',''),(5,'^(99897)([0-9]{7})$','1','1','\0');
/*!40000 ALTER TABLE `number_reg_ex` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reportsmds_request`
--

DROP TABLE IF EXISTS `reportsmds_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reportsmds_request` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned DEFAULT NULL,
  `toAD` bigint(20) NOT NULL,
  `addedInQueue` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `IX_REPORTSMDS_REQ_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `reportsmds_response`
--

DROP TABLE IF EXISTS `reportsmds_response`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reportsmds_response` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `errorCode` tinyint(3) unsigned DEFAULT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  `receivedResponse` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `dialogId` bigint(20) NOT NULL,
  KEY `IX_REPORTSMDS_RES_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `send_waiting_list`
--

DROP TABLE IF EXISTS `send_waiting_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `send_waiting_list` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `dcs` tinyint(4) unsigned NOT NULL,
  `pid` tinyint(4) unsigned NOT NULL,
  `inserted` timestamp NULL DEFAULT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `priority` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `segmentLen` enum('160','140','70','153','134','67') COLLATE utf8_unicode_ci DEFAULT '160',
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `send_waiting_list`
--

LOCK TABLES `send_waiting_list` WRITE;
/*!40000 ALTER TABLE `send_waiting_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `send_waiting_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `server_cancelsm_request`
--

DROP TABLE IF EXISTS `server_cancelsm_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `server_cancelsm_request` (
  `messageId` bigint(20) NOT NULL,
  `accessId` int(11) NOT NULL,
  `requestType` enum('SMPP','HTTP') COLLATE utf8_unicode_ci NOT NULL,
  `queueType` enum('SMSQUEUE','ALERT','NEXTATTEMPT') COLLATE utf8_unicode_ci NOT NULL,
  `occurred` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isValid` bit(1) DEFAULT NULL,
  KEY `IX_SERVER_CANCELSM_REQUEST_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `server_cancelsm_request`
--

LOCK TABLES `server_cancelsm_request` WRITE;
/*!40000 ALTER TABLE `server_cancelsm_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `server_cancelsm_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_client`
--

DROP TABLE IF EXISTS `smpp_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_client` (
  `id` tinyint(4) unsigned NOT NULL AUTO_INCREMENT,
  `client_name` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `comment` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_client`
--

LOCK TABLES `smpp_client` WRITE;
/*!40000 ALTER TABLE `smpp_client` DISABLE KEYS */;
INSERT INTO `smpp_client` VALUES (1,'unifun','unifun');
/*!40000 ALTER TABLE `smpp_client` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_client_config`
--

DROP TABLE IF EXISTS `smpp_client_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_client_config` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `systemId` varchar(32) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `groupID` smallint(5) unsigned NOT NULL,
  `clientPriority` tinyint(3) unsigned NOT NULL,
  `systemType` varchar(5) COLLATE utf8_unicode_ci NOT NULL,
  `serviceType` varchar(5) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `ton` enum('0','1','2','3','4','5','6') COLLATE utf8_unicode_ci NOT NULL,
  `np` enum('0','1','3','4','5','6','8','9','10','13','18') COLLATE utf8_unicode_ci NOT NULL,
  `host` varchar(15) COLLATE utf8_unicode_ci NOT NULL DEFAULT '0.0.0.0',
  `port` mediumint(8) unsigned NOT NULL,
  `timeout` tinyint(3) unsigned NOT NULL COMMENT 'in seconds',
  `pduProcessorDegree` tinyint(3) unsigned NOT NULL,
  `bindType` enum('1','2','9') COLLATE utf8_unicode_ci NOT NULL,
  `reconnectTries` tinyint(3) unsigned NOT NULL,
  `reconnectTriesTime` tinyint(3) unsigned NOT NULL COMMENT 'in seconds',
  `speedLimit` smallint(5) unsigned NOT NULL,
  `concatenateType` enum('1','2','3') COLLATE utf8_unicode_ci NOT NULL COMMENT '1 - UDH\n2 - SAR\n3 - PayLoad',
  `remoteIdType` enum('HEX','LONG') COLLATE utf8_unicode_ci NOT NULL,
  `dlrIdType` enum('HEX','LONG') COLLATE utf8_unicode_ci NOT NULL,
  `defaultSMSLiveTime` smallint(5) unsigned NOT NULL COMMENT 'in minutes',
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `systemId_UNIQUE` (`systemId`,`host`,`port`) USING BTREE,
  KEY `FK_SMPP_CLIENTS_GROUPS_idx` (`groupID`) USING BTREE,
  CONSTRAINT `smpp_client_config_ibfk_1` FOREIGN KEY (`groupID`) REFERENCES `smpp_clients_groups` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_client_config`
--

LOCK TABLES `smpp_client_config` WRITE;
/*!40000 ALTER TABLE `smpp_client_config` DISABLE KEYS */;
INSERT INTO `smpp_client_config` VALUES (1,'unifun','unifun',1,10,'unifu','unifu','1','1','127.0.0.1',44455,60,100,'9',255,5,20,'1','LONG','LONG',5,'\0'),(2,'38201','test1234',1,10,'','','1','1','54.171.89.149',3334,60,100,'9',255,5,20,'1','LONG','LONG',5,'\0');
/*!40000 ALTER TABLE `smpp_client_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_client_dlr_waiting_list`
--

DROP TABLE IF EXISTS `smpp_client_dlr_waiting_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_client_dlr_waiting_list` (
  `remoteId` bigint(20) NOT NULL,
  `messageId` bigint(20) NOT NULL,
  `clientId` int(10) unsigned NOT NULL,
  `waitDlrUntil` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`remoteId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_client_dlr_waiting_list`
--

LOCK TABLES `smpp_client_dlr_waiting_list` WRITE;
/*!40000 ALTER TABLE `smpp_client_dlr_waiting_list` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_client_dlr_waiting_list` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_client_error_logs`
--

DROP TABLE IF EXISTS `smpp_client_error_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_client_error_logs` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `systemId` smallint(6) unsigned NOT NULL,
  `sequenceNumber` int(10) unsigned NOT NULL,
  `errorCode` smallint(6) NOT NULL,
  `errorMessage` varchar(512) COLLATE utf8_unicode_ci DEFAULT NULL,
  `occurred` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_client_error_logs`
--

LOCK TABLES `smpp_client_error_logs` WRITE;
/*!40000 ALTER TABLE `smpp_client_error_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_client_error_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_client_incoming_dlr`
--

DROP TABLE IF EXISTS `smpp_client_incoming_dlr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_client_incoming_dlr` (
  `remoteId` bigint(20) unsigned NOT NULL,
  `messageId` bigint(20) DEFAULT NULL,
  `state` enum('2','3','5','8','9') COLLATE utf8_unicode_ci NOT NULL,
  `clientId` int(10) unsigned NOT NULL,
  `occurred` timestamp NULL DEFAULT NULL,
  KEY `IX_REMOTEID` (`remoteId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_client_incoming_dlr`
--

LOCK TABLES `smpp_client_incoming_dlr` WRITE;
/*!40000 ALTER TABLE `smpp_client_incoming_dlr` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_client_incoming_dlr` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_client_submit_log`
--

DROP TABLE IF EXISTS `smpp_client_submit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_client_submit_log` (
  `messageId` bigint(20) NOT NULL,
  `remoteId` bigint(20) NOT NULL,
  `clientId` int(10) unsigned NOT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  `started` timestamp NULL DEFAULT NULL,
  `finished` timestamp NULL DEFAULT NULL,
  KEY `IX_SMPP_SUBMITSM_LOG_TRANSACTIONID` (`messageId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_client_submit_log`
--

LOCK TABLES `smpp_client_submit_log` WRITE;
/*!40000 ALTER TABLE `smpp_client_submit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_client_submit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_clients_groups`
--

DROP TABLE IF EXISTS `smpp_clients_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_clients_groups` (
  `id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `groupName` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `priority` tinyint(3) unsigned NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_clients_groups`
--

LOCK TABLES `smpp_clients_groups` WRITE;
/*!40000 ALTER TABLE `smpp_clients_groups` DISABLE KEYS */;
INSERT INTO `smpp_clients_groups` VALUES (1,'testname',0,'');
/*!40000 ALTER TABLE `smpp_clients_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_dlr_logs`
--

DROP TABLE IF EXISTS `smpp_dlr_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_dlr_logs` (
  `messageId` bigint(20) unsigned NOT NULL,
  `started` timestamp NULL DEFAULT NULL,
  `finished` timestamp NULL DEFAULT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  KEY `IX_DLRLOGS_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smpp_dlr_sended_log`
--

DROP TABLE IF EXISTS `smpp_dlr_sended_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_dlr_sended_log` (
  `messageId` bigint(20) unsigned NOT NULL,
  `attempts` tinyint(4) unsigned NOT NULL,
  `sendedDLR` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `smpp_error_logs`
--

DROP TABLE IF EXISTS `smpp_error_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_error_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `systemId` tinyint(4) unsigned NOT NULL,
  `errorCode` smallint(5) DEFAULT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  `destAddr` bigint(20) NOT NULL,
  `sequenceNumber` int(11) NOT NULL,
  `sourceAddr` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `occurred` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_error_logs`
--

LOCK TABLES `smpp_error_logs` WRITE;
/*!40000 ALTER TABLE `smpp_error_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_error_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_incoming_dlr_log`
--

DROP TABLE IF EXISTS `smpp_incoming_dlr_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_incoming_dlr_log` (
  `messageId` bigint(20) NOT NULL,
  `receivedDLR` timestamp NULL DEFAULT NULL,
  `sendDLRUntil` timestamp NULL DEFAULT NULL,
  `state` enum('2','3','5','8','9') COLLATE utf8_unicode_ci NOT NULL DEFAULT '8',
  `sendSMAttempts` tinyint(3) unsigned NOT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smpp_mo_response_log`
--

DROP TABLE IF EXISTS `smpp_mo_response_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_mo_response_log` (
  `messageId` bigint(20) unsigned NOT NULL,
  `accessId` int(11) unsigned NOT NULL,
  `state` enum('DELIVRD','EXPIRED','DELETED','UNDELIV','ACCEPTED','UNKNOWN','REJECTD') COLLATE utf8_unicode_ci NOT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  `started` timestamp NULL DEFAULT NULL,
  `finished` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_mo_response_log`
--

LOCK TABLES `smpp_mo_response_log` WRITE;
/*!40000 ALTER TABLE `smpp_mo_response_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_mo_response_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_server_config`
--

DROP TABLE IF EXISTS `smpp_server_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_server_config` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `server_name` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `service_type` varchar(24) COLLATE utf8_unicode_ci NOT NULL,
  `server_port` mediumint(8) unsigned NOT NULL,
  `inerface_version` enum('IF_33','IF_34','IF_50') COLLATE utf8_unicode_ci NOT NULL,
  `processor_degree` smallint(5) unsigned NOT NULL,
  `timeout` tinyint(3) unsigned NOT NULL,
  `waitbind` tinyint(3) unsigned NOT NULL,
  `send_dlr_per_sec` smallint(5) unsigned NOT NULL,
  `concatinate_type` enum('1','2','3') COLLATE utf8_unicode_ci NOT NULL,
  `next_part_waiting` tinyint(3) unsigned NOT NULL,
  `poolSize` smallint(5) unsigned NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_server_config`
--

LOCK TABLES `smpp_server_config` WRITE;
/*!40000 ALTER TABLE `smpp_server_config` DISABLE KEYS */;
INSERT INTO `smpp_server_config` VALUES (1,'TEST-UNIFUN','',44555,'IF_34',50,2,20,50,'1',60,200,'');
/*!40000 ALTER TABLE `smpp_server_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smpp_transmittable_connections`
--

DROP TABLE IF EXISTS `smpp_transmittable_connections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smpp_transmittable_connections` (
  `systemId` tinyint(4) unsigned NOT NULL,
  `started` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`systemId`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smpp_transmittable_connections`
--

LOCK TABLES `smpp_transmittable_connections` WRITE;
/*!40000 ALTER TABLE `smpp_transmittable_connections` DISABLE KEYS */;
/*!40000 ALTER TABLE `smpp_transmittable_connections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smsdata`
--

DROP TABLE IF EXISTS `smsdata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smsdata` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `dcs` tinyint(4) unsigned NOT NULL,
  `pid` tinyint(4) unsigned NOT NULL,
  `inserted` timestamp NULL DEFAULT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `priority` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `segmentLen` enum('160','140','70','153','134','67') COLLATE utf8_unicode_ci DEFAULT '160',
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  PRIMARY KEY (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smsdata_archive`
--

DROP TABLE IF EXISTS `smsdata_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smsdata_archive` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `state` enum('2','3','4','5','8','9') COLLATE utf8_unicode_ci NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL,
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL,
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `dcs` tinyint(4) unsigned NOT NULL,
  `pid` tinyint(4) unsigned NOT NULL,
  `inserted` timestamp NULL DEFAULT NULL,
  `sendedSM` timestamp NULL DEFAULT NULL,
  `sendSMAttempts` tinyint(4) unsigned NOT NULL,
  `sendedDLR` timestamp NULL DEFAULT NULL,
  `sendDLRAttempts` tinyint(4) unsigned NOT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `priority` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL,
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL,
  `deleteFromArchiveDate` date DEFAULT NULL,
  PRIMARY KEY (`messageId`),
  KEY `IXC_SMSDATA_ARCHIVE_DELETE_FROM_ARCHIVE_DATE` (`deleteFromArchiveDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smsgateway_settings`
--

DROP TABLE IF EXISTS `smsgateway_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smsgateway_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `st_type` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `value` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smsgateway_settings`
--

LOCK TABLES `smsgateway_settings` WRITE;
/*!40000 ALTER TABLE `smsgateway_settings` DISABLE KEYS */;
INSERT INTO `smsgateway_settings` VALUES (2,'serviceCenter','map','998970000010'),(3,'opc','map','13076'),(4,'dpc','map','13259'),(5,'opcssn','map','8'),(6,'dpcssn','map','6'),(7,'gtType','map','GT0100'),(8,'gtNatureOfAddress','map','INTERNATIONAL'),(9,'gtTranslationType','map','0'),(10,'gtNumberingPlan','map','ISDN_TELEPHONY'),(11,'routingIndicator','map','ROUTING_BASED_ON_GLOBAL_TITLE'),(12,'addressIndicator','map','16'),(13,'mapMsgWrProc','db','store_mapMsg'),(14,'routingRuleTable','db','ss_route_rule'),(15,'timeout','http','60000'),(16,'threads','app','1000'),(17,'forwardFailure','app','false'),(18,'configPath','app','cfg/'),(19,'log4jcfg','app','log4j.properties'),(20,'nextAttemptToReSendInMin','app','5'),(21,'defaultSMSLiveTimeMin','app','240'),(22,'nextAttemptForAlertMin','app','720'),(23,'maxSchedulerTimeInMin','app','0'),(24,'TPSPerSec','checkAlertQueue','1'),(25,'Quantity','checkAlertQueue','25'),(26,'TPSPerSec','checkNextAttemptQueue','1'),(27,'Quantity','checkNextAttemptQueue','25'),(28,'TPSPerSec','dlrWorkers','200'),(29,'TPSPerSec','mapWorkers','100'),(30,'TPSPerSec','moveFromSMSQueue','1'),(31,'Quantity','moveFromSMSQueue','50'),(32,'sendDLRUntilInMin','app','3'),(33,'TPSPerSec','getSMSToSend','1'),(34,'Quantity','getSMSToSend','50'),(35,'threadPoolSize','serverConrollerConfig','200'),(36,'sendDLRUntilInMin','serverConrollerConfig','1'),(37,'checkSMPPAccessList','serverConrollerConfig','1'),(38,'dlrReSendMaxAttempts','serverConrollerConfig','5'),(39,'dlrReSendIntervalAttemptSec','serverConrollerConfig','5'),(40,'dlrSendSpeedPerSec','serverConrollerConfig','200'),(41,'updateConfigInfoInMin','app','1'),(42,'contentProviderSmsQueueSize','app','1000'),(43,'host','http','0.0.0.0'),(44,'port','http','8089'),(45,'threadPoolSize','http','100'),(46,'jettytimeout','http','20000'),(47,'threadPoolSize','clientConrollerConfig','100'),(48,'checkDLRWaitingList','clientConrollerConfig','1'),(49,'defaultSmsLiveTime','clientConrollerConfig','5'),(50,'MSCLocalPrefix','app','998'),(51,'reSendSubmitPerSec','clientConrollerConfig','5'),(52,'increaseWaitDLRFromSMSC','clientConrollerConfig','30'),(53,'gtAddressStringAN','map','international_number'),(54,'gtAddressNP','map','ISDN'),(55,'sendInRoaming','map','true'),(57,'hlrdpcssn','map','6'),(59,'mscdpcssn','map','8'),(61,'sendInRoamingOtherSigtran','app','false');
/*!40000 ALTER TABLE `smsgateway_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `smsqueue`
--

DROP TABLE IF EXISTS `smsqueue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `smsqueue` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `priority` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL,
  `pid` tinyint(4) unsigned NOT NULL,
  `dcs` tinyint(4) unsigned NOT NULL,
  `segmentLen` enum('160','140','70','153','134','67') COLLATE utf8_unicode_ci NOT NULL DEFAULT '160',
  `inserted` timestamp NULL DEFAULT NULL,
  `scheduledtime` timestamp NULL DEFAULT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  PRIMARY KEY (`messageId`),
  KEY `IXC_SMSQUEUE_SCHEDULEDTIME_PRIORITY` (`scheduledtime`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `smsqueue`
--

LOCK TABLES `smsqueue` WRITE;
/*!40000 ALTER TABLE `smsqueue` DISABLE KEYS */;
/*!40000 ALTER TABLE `smsqueue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `srism_request`
--

DROP TABLE IF EXISTS `srism_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `srism_request` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `toAD` bigint(20) NOT NULL,
  `addedInQueue` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `IX_SRISM_REQ_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `srism_response`
--

DROP TABLE IF EXISTS `srism_response`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `srism_response` (
  `messageId` bigint(20) NOT NULL,
  `dialogId` bigint(20) NOT NULL,
  `errorCode` tinyint(3) unsigned DEFAULT NULL,
  `errorMessage` varchar(256) COLLATE utf8_unicode_ci DEFAULT NULL,
  `receivedResponse` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mscAddress` bigint(20) unsigned DEFAULT NULL,
  KEY `IX_SRISM_RESPONSE_MESSAGEID` (`messageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `substitution_sourche_address`
--

DROP TABLE IF EXISTS `substitution_sourche_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `substitution_sourche_address` (
  `sourche_original` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `sourche_substitution` varchar(15) COLLATE utf8_unicode_ci DEFAULT NULL,
  UNIQUE KEY `idsubstitution_sourche_address_UNIQUE` (`sourche_original`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `substitution_sourche_address`
--

LOCK TABLES `substitution_sourche_address` WRITE;
/*!40000 ALTER TABLE `substitution_sourche_address` DISABLE KEYS */;
/*!40000 ALTER TABLE `substitution_sourche_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `temp_content_provide_access_queue_size`
--

DROP TABLE IF EXISTS `temp_content_provide_access_queue_size`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `temp_content_provide_access_queue_size` (
  `systemId` int(11) NOT NULL,
  `size` int(11) NOT NULL,
  PRIMARY KEY (`systemId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `temp_content_provide_access_queue_size`
--

LOCK TABLES `temp_content_provide_access_queue_size` WRITE;
/*!40000 ALTER TABLE `temp_content_provide_access_queue_size` DISABLE KEYS */;
/*!40000 ALTER TABLE `temp_content_provide_access_queue_size` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tempalertlist`
--

DROP TABLE IF EXISTS `tempalertlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tempalertlist` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `msisdn` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `toNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `nextAttempt` timestamp NULL DEFAULT NULL,
  `errorState` enum('7','10','20','32','33','50','60') COLLATE utf8_unicode_ci NOT NULL DEFAULT '7',
  `sendUntil` timestamp NULL DEFAULT NULL,
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '1',
  PRIMARY KEY (`messageId`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tempalertlist`
--

LOCK TABLES `tempalertlist` WRITE;
/*!40000 ALTER TABLE `tempalertlist` DISABLE KEYS */;
/*!40000 ALTER TABLE `tempalertlist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tempsmsqueue`
--

DROP TABLE IF EXISTS `tempsmsqueue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tempsmsqueue` (
  `messageId` bigint(20) NOT NULL,
  `systemId` tinyint(4) unsigned NOT NULL,
  `fromAD` varchar(15) COLLATE utf8_unicode_ci NOT NULL,
  `fromNP` enum('0','1','3','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `fromTON` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `toAD` bigint(20) NOT NULL,
  `toAN` enum('0','1','2','3','4','5','6','7') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `toNP` enum('0','1','2','4','5','6','7','8','9','15') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  `message` varchar(8000) COLLATE utf8_unicode_ci NOT NULL,
  `quantity` tinyint(4) unsigned NOT NULL,
  `priority` enum('0','1','2','3','4','5','6','7','8','9','10') COLLATE utf8_unicode_ci NOT NULL,
  `pid` smallint(6) NOT NULL,
  `dcs` smallint(6) NOT NULL,
  `segmentLen` enum('160','140','70','153','134','67') COLLATE utf8_unicode_ci NOT NULL DEFAULT '160',
  `inserted` timestamp NULL DEFAULT NULL,
  `scheduledtime` timestamp NULL DEFAULT NULL,
  `senduntil` timestamp NULL DEFAULT NULL,
  `dlrResponseType` enum('0','1','2','3') COLLATE utf8_unicode_ci NOT NULL DEFAULT '0',
  PRIMARY KEY (`messageId`)
) ENGINE=MEMORY DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tempsmsqueue`
--

LOCK TABLES `tempsmsqueue` WRITE;
/*!40000 ALTER TABLE `tempsmsqueue` DISABLE KEYS */;
/*!40000 ALTER TABLE `tempsmsqueue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'smsgateway'
--
/*!50003 DROP PROCEDURE IF EXISTS `spCheckContentProviderAccessSmsQueueSize` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spCheckContentProviderAccessSmsQueueSize`()
BEGIN
	SELECT	systemId, count(systemId)
    FROM	smsqueue
    GROUP BY systemId;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spClearSmsArchive` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spClearSmsArchive`(qLimit int)
BEGIN

	DECLARE removeDate DATE;
    DECLARE rowsCount INT;
    DECLARE itarations INT;
    DECLARE residue INT;
    SET removeDate = DATE(DATE_ADD(NOW(), INTERVAL 1 DAY));
    
    SET rowsCount = (SELECT COUNT(1) FROM smsdata_archive WHERE deleteFromArchiveDate = removeDate);
    
    SET itarations = rowsCount DIV qLimit;
    SET residue = rowsCount % qLimit;
    
    IF(rowsCount != 0) THEN   
		WHILE itarations != 0 DO
			DELETE FROM smsdata_archive WHERE deleteFromArchiveDate = removeDate LIMIT qLimit;
			SET itarations = itarations - 1;
		END WHILE;
		
		DELETE FROM smsdata_archive WHERE deleteFromArchiveDate = removeDate LIMIT residue;
    END IF;
/*
	DELETE FROM smsdata_archive WHERE (inserted < DATE_ADD(NOW(), INTERVAL -_days DAY) AND sendedDLR IS NULL AND sendedSM IS NULL);
    DELETE FROM smsdata_archive WHERE (sendedSM < DATE_ADD(NOW(), INTERVAL -_days DAY) AND sendedDLR IS NULL);
    DELETE FROM smsdata_archive WHERE sendedDLR < DATE_ADD(NOW(), INTERVAL -_days DAY);
*/
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spGetConnectionRestrictions` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spGetConnectionRestrictions`(sysId int)
BEGIN
	SELECT	c.id, clientId, login, password, ton, np, speedLimit, fromAd, fromTon, fromNP, toAd, toNp, toTon, startAt, stopAt, liveTime, sm_parts, encoding
	FROM	smpp_connections			c
	JOIN	smpp_connection_directions	d ON d.connectionId = c.id
	JOIN	smpp_connection_scheduling	s ON s.connectionId = c.id
	JOIN	smpp_connection_sm_config	sc ON sc.connectionId = c.id
    WHERE	c.id = sysId OR sysId IS NULL;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spGetLastSMSQueueId` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spGetLastSMSQueueId`()
BEGIN
	DECLARE lastid BIGINT;
    set lastid = (	SELECT 	max(id) 
					FROM	(SELECT	 MAX(messageId) AS id
							 FROM	smsqueue
                             UNION
                             SELECT	 MAX(messageId) AS id
							 FROM	smsdata
                             UNION
                             SELECT	 MAX(messageId)	AS id
							 FROM	smsdata_archive
                             ) AS tbl
				);
	IF (lastid IS NULL) THEN
		SELECT 10000;
	ELSE
		SELECT lastid + 1000;
	END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spGetNextSMPPDLRRequest` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spGetNextSMPPDLRRequest`(
	  divider	TINYINT
	, remainder TINYINT
	, qLimit 	INT
)
BEGIN

	SELECT	s.messageId, s.systemId, s.fromAD, s.fromNP, s.fromTON, s.toAD, s.toAN, s.toNP, s.state, s.sendedSM, s.dlrResponseType, s.dlrAttempts + 1 as dlrAttempts, s.sendSMAttempts
	FROM	smpp_transmittable_connections	t 
	JOIN	dlr_awaiting_send				s ON t.systemId = s.systemId
	WHERE	s.messageId % divider = remainder
	LIMIT	qLimit;
/*
    DECLARE id bigint;
    
    SET id = (	SELECT	s.messageId
				FROM	smpp_transmittable_connections	t 
				JOIN	dlr_awaiting_send				s ON t.systemId = s.systemId
				WHERE	s.messageId % divider = remainder
				LIMIT	1);
                
	SELECT	messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, state, sendedSM, dlrResponseType, attempts + 1 as attempts
	FROM	dlr_awaiting_send
	WHERE	messageId = id;
    
    DELETE FROM	dlr_awaiting_send WHERE	messageId = id;
    */
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spGetSmppConnection` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spGetSmppConnection`(_login varchar(50), _password varchar(50), _ton varchar(2), _np varchar(2))
BEGIN
	SELECT	c.id, clientId, login, password, ton, np, speedLimit, fromAd, fromTon, fromNP, toAd, toNp, toTon, startAt, stopAt, liveTime, sm_parts, encoding
	FROM	smpp_connections			c
	JOIN	smpp_connection_directions	d ON d.connectionId = c.id
	JOIN	smpp_connection_scheduling	s ON s.connectionId = c.id
	JOIN	smpp_connection_sm_config	sc ON sc.connectionId = c.id
    WHERE	c.login = _login
      AND	c.password = _password
      AND	c.ton = _ton
      AND	c.np = _np
	LIMIT	1;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spMoveToArchive` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spMoveToArchive`()
BEGIN
    DECLARE _tableName VARCHAR(50);
    DECLARE _createScript TEXT;
    DECLARE _dropScript TEXT;
    DECLARE _insertQuery TEXT;
    DECLARE _deleteQuery TEXT;
    
    /* ð┐ðÁÐÇðÁð╝ðÁð¢ð¢ð░ÐÅ hadler - a*/
    DECLARE done integer default 0;
    
    /*ð×ð▒ÐèÐÅð▓ð╗ðÁð¢ð©ðÁ ð║ÐâÐÇÐüð¥ÐÇð░*/
    DECLARE archive CURSOR FOR 
    SELECT	tableName, createScript, dropScript, insertQuery, deleteQuery
    FROM	archive_tables_scripts;
    
    /*HANDLER ð¢ð░ðÀð¢ð░ÐçðÁð¢ð©ðÁ, ð║ð¥Ðéð¥ÐÇð¥ð│ð¥ ð┐ð¥ÐÅÐüð¢ð©ð╝ ÐçÐâÐéÐî ð¢ð©ðÂðÁ*/
	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done=1;
    /* ð¥Ðéð║ÐÇÐïÐéð©ðÁ ð║ÐâÐÇÐüð¥ÐÇð░ */
    OPEN archive;
    /*ð©ðÀð▓ð╗ðÁð║ð░ðÁð╝ ð┤ð░ð¢ð¢ÐïðÁ */
	WHILE done = 0 DO 
		FETCH archive INTO _tableName, _createScript, _dropScript, _insertQuery, _deleteQuery;
        /*STARTING FLAG*/
        SET @startQuery = CONCAT(REPLACE('UPDATE archive_tables_scripts SET processed = NULL WHERE tableName = ''%''', '%', _tableName));
		PREPARE stmt FROM @startQuery; 
		EXECUTE stmt; 
		DEALLOCATE PREPARE stmt;
        /*CREATE NEW ARCHIVE TABLE*/
        SET @createQuery = CONCAT(REPLACE(_createScript COLLATE utf8_unicode_ci, '%', CAST(DATE_FORMAT(ADDDATE(NOW(), INTERVAL -1 DAY),'%Y%m%d') AS CHAR )));
		IF (@createQuery != '') THEN
			PREPARE stmt1 FROM @createQuery; 
			EXECUTE stmt1; 
			DEALLOCATE PREPARE stmt1;
        END IF;
        /*INSERT INTO NEW TABLE EXISTING LOGS*/
        SET @insertQuery = CONCAT(REPLACE(_insertQuery COLLATE utf8_unicode_ci, '%', CAST(DATE_FORMAT(ADDDATE(NOW(), INTERVAL -1 DAY),'%Y%m%d') AS CHAR )));
		IF (@insertQuery  != '') THEN
			PREPARE stmt2 FROM @insertQuery; 
			EXECUTE stmt2; 
			DEALLOCATE PREPARE stmt2;
        END IF;
        
        /*DELETE EXISTING LOGS*/
        SET @deleteQuery = CONCAT(_deleteQuery);
        IF (@deleteQuery != '') THEN
			PREPARE stmt3 FROM @deleteQuery; 
			EXECUTE stmt3;
			DEALLOCATE PREPARE stmt3;
        END IF;
        /*DROP EXPIRED LOGS*/
        SET @dropQuery = CONCAT(REPLACE(_dropScript COLLATE utf8_unicode_ci, '%', CAST(DATE_FORMAT(ADDDATE(NOW(), INTERVAL -3 MONTH),'%Y%m%d') AS CHAR)));
        IF(@dropQuery != '') THEN
			PREPARE stmt4 FROM @dropQuery; 
			EXECUTE stmt4;
			DEALLOCATE PREPARE stmt4;
        END IF;
        /*ENDING FLAG*/
        SET @endQuery = CONCAT(REPLACE('UPDATE archive_tables_scripts SET processed = CURRENT_TIMESTAMP WHERE tableName = ''%''', '%', _tableName));
		PREPARE stmt5 FROM @endQuery; 
		EXECUTE stmt5; 
		DEALLOCATE PREPARE stmt5;
        
    END WHILE;
    
    CLOSE archive;

END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spMoveToSMSData` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spMoveToSMSData`(in qLimit int)
BEGIN

	INSERT	tempsmsqueue(messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, priority, pid, dcs, segmentLen, inserted, scheduledtime, senduntil, dlrResponseType)
	SELECT	messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, priority, pid, dcs, segmentLen, inserted, scheduledtime, senduntil, dlrResponseType
	FROM	smsqueue	q
	WHERE	scheduledtime < CURRENT_TIMESTAMP
      /*AND	NOT EXISTS (	SELECT 1
							FROM	srism_awaiting_response	sr
                            WHERE	sr.toAD = q.toAD
                            LIMIT	1)
	  AND	NOT EXISTS (	SELECT 1
							FROM	mtfsm_awaiting_response	mr
                            WHERE	mr.toAD = q.toAD
                            LIMIT	1)*/
	ORDER BY priority
	LIMIT	qLimit;
    
    INSERT	smsdata(messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, pid, dcs, inserted, senduntil, priority, segmentLen, dlrResponseType)
    SELECT	messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, pid, dcs, inserted, senduntil, priority, segmentLen, dlrResponseType
    FROM	tempsmsqueue;
    
    INSERT	send_waiting_list(messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, pid, dcs, inserted, senduntil, priority, segmentLen, dlrResponseType)
    SELECT	messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, pid, dcs, inserted, senduntil, priority, segmentLen, dlrResponseType
    FROM	tempsmsqueue;
    
    DELETE	s, t
    FROM	smsqueue 		s
    JOIN	tempsmsqueue	t
    WHERE	 s.messageId = t.messageId; 

END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spProcessCancelSM` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spProcessCancelSM`(
	_messageId	BIGINT,
    _accessId	INT,
    _reqType	VARCHAR(5),
    _queueType	VARCHAR(15))
BEGIN
	DECLARE result BIT;
    
    SET result = FALSE;
    # check smsQueue
    IF(_queueType = 'SMSQUEUE') THEN
		IF EXISTS(	SELECT 1 
					FROM smsqueue
					WHERE messageId = _messageId
					LIMIT 1 FOR UPDATE) THEN 
			SET result = TRUE;
			INSERT	smsdata_archive(messageId, systemId, state, fromAD, fromTON, fromNP,
					toAD, toAN, toNP, message, quantity, dcs, pid, inserted, sendedSM, sendSMAttempts, sendedDLR, sendDLRAttempts, senduntil,
					priority, dlrResponseType , deleteFromArchiveDate)
			SELECT	messageId, systemId, '4', fromAD, fromTON, fromNP,
					toAD, toAN, toNP, message, quantity, dcs, pid, inserted, null, 0, null, 0, senduntil,
					priority, dlrResponseType, date(date_add(now(), interval 1 month)) 
			FROM smsqueue
			WHERE messageId = _messageId
			LIMIT 1;
			DELETE FROM smsqueue WHERE messageId = _messageId LIMIT 1;
		END IF;
    # check alert_waiting_list. subscriber is absent
    ELSE IF (_queueType = 'ALERT') THEN 
		SET result = TRUE;
        
        INSERT	smsdata_archive(messageId, systemId, state, fromAD, fromTON, fromNP,
				toAD, toAN, toNP, message, quantity, dcs, pid, inserted, sendedSM, sendSMAttempts, sendedDLR, sendDLRAttempts, senduntil,
				priority, dlrResponseType , deleteFromArchiveDate)
		SELECT	messageId, systemId, '4', fromAD, fromTON, fromNP,
				toAD, toAN, toNP, message, quantity, dcs, pid, inserted, null, sendSMAttempts, null, 0, senduntil,
				priority, dlrResponseType, date(date_add(now(), interval 1 month)) 
		FROM alert_waiting_list
		WHERE messageId = _messageId
		LIMIT 1;
        
        DELETE a,s
        FROM alert_waiting_list a
        JOIN smsdata			s on a.messageId = s.messageId
        WHERE a.messageId = _messageId;
	# check next_attempt_waiting_list. subscriber does not receive sms.
    ELSE IF (_queueType = 'NEXTATTEMPT') THEN 
		SET result = TRUE; 
        
        INSERT	smsdata_archive(messageId, systemId, state, fromAD, fromTON, fromNP,
				toAD, toAN, toNP, message, quantity, dcs, pid, inserted, sendedSM, sendSMAttempts, sendedDLR, sendDLRAttempts, senduntil,
				priority, dlrResponseType , deleteFromArchiveDate)
		SELECT	messageId, systemId, '4', fromAD, fromTON, fromNP,
				toAD, toAN, toNP, message, quantity, dcs, pid, inserted, null, sendSMAttempts, null, 0, senduntil,
				priority, dlrResponseType, date(date_add(now(), interval 1 month)) 
		FROM next_attempt_waiting_list
		WHERE messageId = _messageId
		LIMIT 1;
        
		DELETE n,s
        FROM next_attempt_waiting_list	n
        JOIN smsdata					s on n.messageId = s.messageId
        WHERE n.messageId = _messageId;
    END IF;
    END IF;
    END IF;
    
    #addl request log
    INSERT server_cancelsm_request(messageId, accessId, occurred, requestType, queueType, isValid)
	VALUES (_messageId, _accessId, now(), _reqType, _queueType, result);
    
	SELECT result;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spResetQueues` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spResetQueues`()
BEGIN

	#MAP QUEUES
	INSERT INTO send_waiting_list(messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, dcs, pid, inserted, senduntil, priority, segmentLen, dlrResponseType)
	SELECT /*resultType,*/ messageId, systemId, fromAD, fromNP, fromTON, toAD, toAN, toNP, message, quantity, dcs, pid, inserted, senduntil, priority, segmentLen, dlrResponseType
	FROM	(SELECT   d.*
					#, srism_r.addedInQueue as r1, srism_s.receivedResponse as s1, mtfsm_r.addedInQueue as r2
					#, mtfsm_s.receivedResponse as s2, rdsds_r.addedInQueue as r3, rdsds_s.receivedResponse as s3
			 FROM	(SELECT	s.*
					 FROM	smsdata	s
					 WHERE	NOT EXISTS	# Check final states
							(	SELECT	1
								FROM	send_waiting_list	w
								WHERE	w.messageId = s.messageId)
					   AND	NOT EXISTS
							(	SELECT	1
								FROM	alert_waiting_list a
								WHERE	a.messageId = s.messageId)
					   AND	NOT EXISTS
							(	SELECT	1
								FROM	smpp_incoming_dlr_log d
								WHERE	d.messageId = s.messageId)
					   AND	NOT EXISTS
							(	SELECT	1
								FROM	next_attempt_waiting_list	n
								WHERE	n.messageId = s.messageId)
					   AND	NOT EXISTS
							(	SELECT	1
								FROM	smpp_client_dlr_waiting_list	c
								WHERE	c.messageId = s.messageId)		
					   AND	NOT EXISTS
							(	SELECT	1
								FROM	send_waiting_list	l
								WHERE	l.messageId = s.messageId)		)	AS d
			)	AS temp
	#GROUP BY messageId, r1, s1, r2, s2, r3, s3					
	#HAVING	resultType IS NOT NULL OR (resultType IS NULL AND senduntil > now())
		#) 	AS result
	;
    #MAP QUEUES
	
    #DLR QUEUES
    /*
	CREATE TEMPORARY TABLE IF NOT EXISTS messageIds 
		AS (SELECT	i.messageId as messageId
			FROM		smpp_dlr_sended_log		s	
			RIGHT JOIN	smpp_incoming_dlr_log	i on i.messageId = s.messageId
			JOIN	smsdata						d on d.messageId = i.messageId
			WHERE	s.messageId is null);
	
    
    INSERT smsdata_archive(messageId,systemId,state,fromAD,fromTON,fromNP,toAD,toAN,toNP,message,quantity,dcs,pid,inserted,sendedSM,sendSMAttempts,sendedDLR,sendDLRAttempts,senduntil,priority,dlrResponseType)
	SELECT s.messageId, s.systemId, i.state, s.fromAD, s.fromTON, s.fromNP, s.toAD, s.toAN, s.toNP, s.message, s.quantity, s.dcs, s.pid, s.inserted, i.receivedDLR, i.sendSMAttempts, NULL, 0, i.sendDLRUntil, s.priority, s.dlrResponseType
	FROM	messageIds	d
    JOIN	smpp_incoming_dlr_log	i on i.messageId = d.messageId
    JOIN	smsdata					s on s.messageId = i.messageId;
    
    DELETE FROM smsdata where messageId IN (SELECT messageId FROM messageIds);
    DROP TABLE messageIds;
	*/
    #DLR QUEUES
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spSMPPClientProcessDLRRequest` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spSMPPClientProcessDLRRequest`(
	remoteId		BIGINT,
    state 			VARCHAR(10),
    clientId		INT,
    occurred		TIMESTAMP,
    sendDLRUntil	TIMESTAMP)
BEGIN
	DECLARE _messageId BIGINT;
    
    SET _messageId = (	SELECT 	messageId
						FROM	smpp_client_dlr_waiting_list l
                        WHERE	l.remoteId = remoteId	FOR UPDATE);
	
    INSERT smpp_client_incoming_dlr(remoteId, messageId, state, clientId, occurred)
    VALUES (remoteId, _messageId, state, clientId, occurred);
    
	IF(_messageId IS NOT NULL) THEN
		DELETE l 
        FROM smpp_client_dlr_waiting_list l
        WHERE l.remoteId = remoteId;
        
        #add to server dlr log. Info for reset app
        INSERT smpp_incoming_dlr_log(messageId, receivedDLR, sendDLRUntil, state, sendSMAttempts)
        VALUES(_messageId, occurred, sendDLRUntil, state, 1);	# i don't know, how many attempts was made by SMSC
        
        SELECT	messageId, systemId, fromAD, fromTON, fromNP, toAD, toAN, toNP, state, '' AS message, dcs, 1 AS attempts,
			inserted, occurred AS nextAttempt, dlrResponseType, occurred AS receivedDLR, sendDLRUntil
		FROM	smsdata
		WHERE	messageId = _messageId;
        
    END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spSMPPClientSaveSubmitSMReponse` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spSMPPClientSaveSubmitSMReponse`(
	remoteId		BIGINT,	
    messageId		BIGINT,
    clientId		INT,
    errorMessage	VARCHAR(256),
    started			TIMESTAMP,
    finished		TIMESTAMP, 
    waitDLR			TIMESTAMP
    )
BEGIN
	INSERT	smpp_client_submit_log(messageId, clientId, remoteId, errorMessage, started, finished)
	VALUES	(messageId, clientId, remoteId, errorMessage, started, finished);
    
    IF(remoteId > 0) THEN
		INSERT	smpp_client_dlr_waiting_list(remoteId, messageId, clientId, waitDlrUntil)
		VALUES	(remoteId, messageId, clientId, waitDLR);
    END IF;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `spSmppServerSaveDlrResponse` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = '' */ ;
DELIMITER ;;
CREATE DEFINER=`smsufgateway`@`%` PROCEDURE `spSmppServerSaveDlrResponse`(
	_messageId			LONG, 
    _started 			TIMESTAMP, 
    _finished			TIMESTAMP,
    _errorMessage 		VARCHAR(256),
    _attempts			TINYINT,
    _doNextAttempt		BIT)
BEGIN
   
	INSERT INTO smpp_dlr_logs(messageId, started, finished, errorMessage) 
    VALUES (_messageId, _started, _finished, _errorMessage);
	
    IF(_doNextAttempt != TRUE) THEN
		INSERT smpp_dlr_sended_log(messageId, sendedDLR, attempts)
        VALUES(_messageId, _finished, _attempts);
        
        INSERT	smsdata_archive(messageId,systemId,state,fromAD,fromTON,fromNP,
				toAD,toAN,toNP,message,quantity,dcs,pid,inserted,sendedSM,sendSMAttempts,sendedDLR,sendDLRAttempts,senduntil,priority,dlrResponseType, deleteFromArchiveDate)
		SELECT 	s.messageId, s.systemId, d.state, s.fromAD, s.fromTON, s.fromNP, s.toAD, s.toAN, s.toNP, s.message, s.quantity, s.dcs, s.pid,
				s.inserted, d.receivedDLR, d.sendSMAttempts, _finished, _attempts, s.senduntil, s.priority, s.dlrResponseType, date(date_add(now(), interval 3 month))
		FROM	smpp_incoming_dlr_log	d
        JOIN	smsdata					s on s.messageId = d.messageId
		WHERE	d.messageId = _messageId;

		DELETE FROM smsdata where messageId = _messageId;
    END IF;
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

-- Dump completed on 2016-06-21 17:51:41
