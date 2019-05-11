/*
Navicat MySQL Data Transfer

Source Server         : l2jdb
Source Server Version : 50509
Source Host           : localhost:3306
Source Database       : l2jdb

Target Server Type    : MYSQL
Target Server Version : 50509
File Encoding         : 65001

Date: 2011-03-01 22:31:28
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `comteleport`
-- ----------------------------
DROP TABLE IF EXISTS `comteleport`;
CREATE TABLE `comteleport` (
  `TpId` int(11) NOT NULL AUTO_INCREMENT,
  `charId` int(11) DEFAULT NULL,
  `Xpos` text,
  `Ypos` text,
  `Zpos` text,
  `name` text,
  PRIMARY KEY (`TpId`)
) ENGINE=MyISAM AUTO_INCREMENT=325 DEFAULT CHARSET=cp1251;

-- ----------------------------
-- Records of comteleport
-- ----------------------------
INSERT INTO `comteleport` VALUES ('321', '268482583', '-78438', '-44496', '-10724', ' 12');
INSERT INTO `comteleport` VALUES ('322', '268482583', '-78280', '-44248', '-10619', ' 123');
INSERT INTO `comteleport` VALUES ('323', '268482583', '-81928', '-55288', '-10619', ' gfhgh');
INSERT INTO `comteleport` VALUES ('324', '268482583', '-81928', '-55288', '-10619', ' ????');
