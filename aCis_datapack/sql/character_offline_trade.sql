/*
MySQL Data Transfer
Source Host: localhost
Source Database: acis372
Target Host: localhost
Target Database: acis372
Date: 10/03/2024 0:14:36
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for character_offline_trade
-- ----------------------------
DROP TABLE IF EXISTS `character_offline_trade`;
CREATE TABLE `character_offline_trade` (
  `charId` int(10) unsigned NOT NULL,
  `time` bigint(13) unsigned NOT NULL DEFAULT 0,
  `type` tinyint(4) NOT NULL DEFAULT 0,
  `title` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ----------------------------
-- Records 
-- ----------------------------
