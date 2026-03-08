-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: mill_house_management
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Temporary view structure for view `driver_orders`
--

DROP TABLE IF EXISTS `driver_orders`;
/*!50001 DROP VIEW IF EXISTS `driver_orders`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `driver_orders` AS SELECT 
 1 AS `id`,
 1 AS `customer_id`,
 1 AS `customer_name`,
 1 AS `order_type`,
 1 AS `raw_type`,
 1 AS `estimated_weight`,
 1 AS `delivery_type`,
 1 AS `status`,
 1 AS `customer_address`,
 1 AS `customer_notes`,
 1 AS `operator_notes`,
 1 AS `driver_notes`,
 1 AS `assigned_to`,
 1 AS `assigned_driver`,
 1 AS `order_source`,
 1 AS `created_date`,
 1 AS `updated_date`,
 1 AS `delivery_assigned_date`,
 1 AS `delivery_completed_date`,
 1 AS `payment_method`,
 1 AS `payment_amount`,
 1 AS `status_updated_at`,
 1 AS `mobile_money_type`,
 1 AS `mobile_phone`,
 1 AS `mobile_pin`,
 1 AS `mobile_reference`,
 1 AS `bank_name`,
 1 AS `account_number`,
 1 AS `account_holder`,
 1 AS `payment_reference`,
 1 AS `assigned_driver_name`,
 1 AS `driver_id`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `driver_orders`
--

/*!50001 DROP VIEW IF EXISTS `driver_orders`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `driver_orders` AS select `o`.`id` AS `id`,`o`.`customer_id` AS `customer_id`,`o`.`customer_name` AS `customer_name`,`o`.`order_type` AS `order_type`,`o`.`raw_type` AS `raw_type`,`o`.`estimated_weight` AS `estimated_weight`,`o`.`delivery_type` AS `delivery_type`,`o`.`status` AS `status`,`o`.`customer_address` AS `customer_address`,`o`.`customer_notes` AS `customer_notes`,`o`.`operator_notes` AS `operator_notes`,`o`.`driver_notes` AS `driver_notes`,`o`.`assigned_to` AS `assigned_to`,`o`.`assigned_driver` AS `assigned_driver`,`o`.`order_source` AS `order_source`,`o`.`created_date` AS `created_date`,`o`.`updated_date` AS `updated_date`,`o`.`delivery_assigned_date` AS `delivery_assigned_date`,`o`.`delivery_completed_date` AS `delivery_completed_date`,`o`.`payment_method` AS `payment_method`,`o`.`payment_amount` AS `payment_amount`,`o`.`status_updated_at` AS `status_updated_at`,`o`.`mobile_money_type` AS `mobile_money_type`,`o`.`mobile_phone` AS `mobile_phone`,`o`.`mobile_pin` AS `mobile_pin`,`o`.`mobile_reference` AS `mobile_reference`,`o`.`bank_name` AS `bank_name`,`o`.`account_number` AS `account_number`,`o`.`account_holder` AS `account_holder`,`o`.`payment_reference` AS `payment_reference`,`d`.`name` AS `assigned_driver_name`,`d`.`id` AS `driver_id` from (`orders` `o` left join `drivers` `d` on((`o`.`assigned_driver` = `d`.`name`))) where (`o`.`assigned_driver` is not null) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-08 12:59:06
