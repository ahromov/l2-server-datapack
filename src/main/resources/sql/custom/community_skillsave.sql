/*
Navicat MySQL Data Transfer

Source Server         : l2jdb
Source Server Version : 50509
Source Host           : localhost:3306
Source Database       : l2jdb

Target Server Type    : MYSQL
Target Server Version : 50509
File Encoding         : 65001

Date: 2011-03-01 22:31:18
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `community_skillsave`
-- ----------------------------
DROP TABLE IF EXISTS `community_skillsave`;
CREATE TABLE `community_skillsave` (
  `charId` int(10) DEFAULT NULL,
  `skills` text,
  `pet` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of community_skillsave
-- ----------------------------
