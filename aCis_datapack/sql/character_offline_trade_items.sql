/*
MySQL Data Transfer
Source Host: localhost
Source Database: acis372
Target Host: localhost
Target Database: acis372
Date: 10/03/2024 0:14:47
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for character_offline_trade_items
-- ----------------------------
DROP TABLE IF EXISTS `character_offline_trade_items`;
CREATE TABLE `character_offline_trade_items` (
  `charId` int(10) unsigned NOT NULL,
  `item` int(10) unsigned NOT NULL DEFAULT 0,
  `count` bigint(20) unsigned NOT NULL DEFAULT 0,
  `price` bigint(20) unsigned NOT NULL DEFAULT 0,
  `enchant` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- ----------------------------
-- Records 
-- ----------------------------
