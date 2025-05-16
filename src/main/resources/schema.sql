-- MySQL dump 10.13  Distrib 8.0.41, for Linux (x86_64)
--
-- Host: localhost    Database: bank
-- ------------------------------------------------------
-- Server version	8.0.41-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

CREATE DATABASE IF NOT EXISTS `bank`;
USE `bank`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `account` (
  `account_id` bigint NOT NULL AUTO_INCREMENT,
  `account_number` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `account_type` enum('Savings','Current','Fixed_Deposit','Operational') NOT NULL,
  `status` enum('Suspended','Active','Inactive') NOT NULL,
  `balance` decimal(15,2) NOT NULL,
  `min_balance` decimal(15,2) NOT NULL,
  `created_at` bigint DEFAULT NULL,
  `modified_at` bigint DEFAULT NULL,
  `performed_by` bigint DEFAULT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `account_number` (`account_number`),
  UNIQUE KEY `account_id` (`account_id`),
  KEY `branch_id` (`branch_id`),
  KEY `account_number_2` (`account_number`),
  KEY `fk_user_id` (`user_id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activityLog`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `activityLog` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `table_name` varchar(20) NOT NULL,
  `log_type` enum('Insert','Update','Login','Logout','Delete') NOT NULL,
  `row_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `account_number` bigint DEFAULT NULL,
  `log_message` varchar(150) NOT NULL,
  `timestamp` bigint NOT NULL,
  `performed_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `user_account_number` (`account_number`),
  KEY `idx_log_type` (`log_type`),
  KEY `idx_timestamp` (`timestamp`),
  KEY `activityLog_ibfk_3` (`performed_by`),
  CONSTRAINT `activityLog_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `activityLog_ibfk_2` FOREIGN KEY (`account_number`) REFERENCES `account` (`account_number`) ON DELETE SET NULL,
  CONSTRAINT `activityLog_ibfk_3` FOREIGN KEY (`performed_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=7422 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `branch`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `branch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ifsc_code` varchar(11) NOT NULL,
  `contact_number` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `created_at` bigint DEFAULT NULL,
  `modified_at` bigint DEFAULT NULL,
  `performed_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ifsc_code` (`ifsc_code`),
  UNIQUE KEY `contact_number` (`contact_number`),
  KEY `ifsc_code_2` (`ifsc_code`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `customer` (
  `user_id` bigint NOT NULL,
  `pan_number` varchar(25) NOT NULL,
  `aadhar_number` bigint NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `pan_number` (`pan_number`),
  UNIQUE KEY `aadhar_number` (`aadhar_number`),
  CONSTRAINT `fk_customer_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customerDetail`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `customerDetail` (
  `user_id` bigint NOT NULL,
  `dob` varchar(10) NOT NULL,
  `father_name` varchar(100) NOT NULL,
  `mother_name` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `marital_status` varchar(10) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `user_id` FOREIGN KEY (`user_id`) REFERENCES `customer` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `message_type` varchar(50) NOT NULL,
  `message_content` text NOT NULL,
  `message_status` varchar(20) NOT NULL,
  `created_at` bigint DEFAULT NULL,
  `modified_at` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `sender_id` (`sender_id`),
  CONSTRAINT `message_ibfk_1` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oauth_provider`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `oauth_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `provider` varchar(50) NOT NULL,
  `provider_user_id` varchar(100) NOT NULL,
  `access_token` text NOT NULL,
  `refresh_token` text,
  `expires_in` int DEFAULT NULL,
  `created_at` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `provider_user_id` (`provider_user_id`),
  KEY `fk_user` (`user_id`),
  CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `staff`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `staff` (
  `user_id` bigint NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  KEY `branch_id` (`branch_id`),
  CONSTRAINT `staff_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `staff_ibfk_2` FOREIGN KEY (`branch_id`) REFERENCES `branch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transaction`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint DEFAULT NULL,
  `account_number` bigint NOT NULL,
  `transaction_account_number` bigint DEFAULT NULL,
  `transaction_type` enum('Credit','Debit','Deposit','Withdraw') NOT NULL,
  `status` enum('Completed','Pending','Cancelled','Active','Expired') NOT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `closing_balance` decimal(15,2) NOT NULL,
  `transaction_time` bigint DEFAULT NULL,
  `performed_by` bigint DEFAULT NULL,
  `bank_name` varchar(255) DEFAULT 'Horizon',
  `transaction_ifsc` varchar(20) NOT NULL,
  PRIMARY KEY (`id`,`account_number`),
  KEY `account_number` (`account_number`),
  KEY `performed_by` (`performed_by`),
  KEY `transaction_ibfk_1` (`customer_id`),
  CONSTRAINT `transaction_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `transaction_ibfk_2` FOREIGN KEY (`account_number`) REFERENCES `account` (`account_number`)
) ENGINE=InnoDB AUTO_INCREMENT=5480 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fullname` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` bigint NOT NULL,
  `role` enum('Customer','Employee','Manager') NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `status` enum('Suspended','Active','Inactive') NOT NULL,
  `created_at` bigint DEFAULT NULL,
  `modified_at` bigint DEFAULT NULL,
  `performed_by` bigint DEFAULT NULL,
  `password_version` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `phone` (`phone`),
  UNIQUE KEY `username` (`username`),
  KEY `username_2` (`username`),
  KEY `fk_performed_by` (`performed_by`),
  CONSTRAINT `fk_performed_by` FOREIGN KEY (`performed_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=148 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_session`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `user_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  `provider_id` bigint DEFAULT NULL,
  `created_at` bigint NOT NULL,
  `expires_at` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_id` (`session_id`),
  KEY `user_id` (`user_id`),
  KEY `provider_id` (`provider_id`),
  CONSTRAINT `user_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_session_ibfk_2` FOREIGN KEY (`provider_id`) REFERENCES `oauth_provider` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-13 14:42:41
