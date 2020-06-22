/*
 Navicat Premium Data Transfer

 Source Server         : 阿里云docker
 Source Server Type    : MySQL
 Source Server Version : 50729
 Source Host           : 101.201.125.82:3306
 Source Schema         : order

 Target Server Type    : MySQL
 Target Server Version : 50729
 File Encoding         : 65001

 Date: 22/06/2020 16:38:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for customer_order
-- ----------------------------
DROP TABLE IF EXISTS `customer_order`;
CREATE TABLE `customer_order`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `customer_id` bigint(20) NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NULL DEFAULT NULL,
  `amount` int(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
