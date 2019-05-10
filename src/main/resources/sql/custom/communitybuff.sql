/*
Navicat MySQL Data Transfer

Source Server         : l2jdb
Source Server Version : 50509
Source Host           : localhost:3306
Source Database       : l2jdb

Target Server Type    : MYSQL
Target Server Version : 50509
File Encoding         : 65001

Date: 2011-03-01 22:31:23
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `communitybuff`
-- ----------------------------
DROP TABLE IF EXISTS `communitybuff`;
CREATE TABLE `communitybuff` (
  `key` int(11) DEFAULT NULL,
  `skillID` int(11) DEFAULT NULL,
  `buff_id` int(11) DEFAULT NULL,
  `price` int(11) DEFAULT NULL,
  `itemid` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of communitybuff
-- ----------------------------
INSERT INTO `communitybuff` VALUES (1, 1077, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (2, 1242, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (3, 1086, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (4, 1240, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (5, 1045, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (6, 1048, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (7, 1087, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (8, 1257, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (9, 1068, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (10, 1040, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (11, 1036, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (12, 1268, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (13, 1204, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (14, 1062, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (15, 1388, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (16, 1397, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (17, 1085, 2, 10000, 57);
INSERT INTO `communitybuff` VALUES (18, 1059, 2, 10000, 57);
INSERT INTO `communitybuff` VALUES (19, 1389, 2, 10000, 57);
INSERT INTO `communitybuff` VALUES (20, 1303, 2, 10000, 57);
INSERT INTO `communitybuff` VALUES (21, 264, 6, 10000, 57);
INSERT INTO `communitybuff` VALUES (22, 265, 6, 10000, 57);
INSERT INTO `communitybuff` VALUES (23, 267, 6, 10000, 57);
INSERT INTO `communitybuff` VALUES (24, 268, 6, 10000, 57);
INSERT INTO `communitybuff` VALUES (25, 269, 4, 10000, 57);
INSERT INTO `communitybuff` VALUES (26, 271, 4, 10000, 57);
INSERT INTO `communitybuff` VALUES (27, 274, 4, 10000, 57);
INSERT INTO `communitybuff` VALUES (28, 275, 4, 10000, 57);
INSERT INTO `communitybuff` VALUES (29, 304, 6, 10000, 57);
INSERT INTO `communitybuff` VALUES (30, 310, 4, 10000, 57);
INSERT INTO `communitybuff` VALUES (31, 349, 6, 10000, 57);
INSERT INTO `communitybuff` VALUES (32, 364, 4, 10000, 57);
INSERT INTO `communitybuff` VALUES (33, 273, 5, 10000, 57);
INSERT INTO `communitybuff` VALUES (34, 276, 5, 10000, 57);
INSERT INTO `communitybuff` VALUES (35, 363, 5, 10000, 57);
INSERT INTO `communitybuff` VALUES (36, 1413, 3, 10000, 57);
INSERT INTO `communitybuff` VALUES (37, 365, 5, 10000, 57);
INSERT INTO `communitybuff` VALUES (38, 1363, 1, 10000, 57);
INSERT INTO `communitybuff` VALUES (39, 1035, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (40, 1043, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (41, 1044, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (42, 1073, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (43, 1078, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (44, 1032, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (45, 1243, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (46, 1460, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (47, 1259, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (48, 1304, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (49, 1353, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (50, 1354, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (51, 1355, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (52, 1357, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (53, 1356, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (54, 1191, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (55, 1033, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (56, 1182, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (57, 1189, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (58, 1392, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (59, 1393, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (60, 1352, 0, 10000, 57); 				  
INSERT INTO `communitybuff` VALUES (61, 272, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (62, 277, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (63, 307, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (64, 309, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (65, 311, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (66, 366, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (67, 530, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (68, 266, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (69, 270, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (70, 305, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (71, 306, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (72, 308, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (73, 529, 0, 10000, 57);				   
INSERT INTO `communitybuff` VALUES (74, 1007, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (75, 1009, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (76, 1002, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (77, 1006, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (78, 1251, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (79, 1252, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (80, 1253, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (81, 1284, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (82, 1308, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (83, 1309, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (84, 1310, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (85, 1362, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (86, 1390, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (87, 1391, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (88, 1461, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (89, 1003, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (90, 1004, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (91, 1005, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (92, 1008, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (93, 1249, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (94, 1250, 0, 10000, 57);  
INSERT INTO `communitybuff` VALUES (95, 1260, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (96, 1261, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (97, 1282, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (98, 1364, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (99, 1365, 0, 10000, 57); 
INSERT INTO `communitybuff` VALUES (100, 1414, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (101, 1415, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (102, 1416, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (103, 4699, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (104, 4700, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (105, 4702, 0, 10000, 57);
INSERT INTO `communitybuff` VALUES (106, 4703, 0, 10000, 57);