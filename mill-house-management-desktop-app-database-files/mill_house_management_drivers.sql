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
-- Table structure for table `drivers`
--

DROP TABLE IF EXISTS `drivers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drivers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `staff_id` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `vehicle_type` varchar(50) DEFAULT NULL,
  `vehicle_plate` varchar(20) DEFAULT NULL,
  `status` enum('Available','On Delivery','Off Duty','Maintenance') DEFAULT 'Available',
  `vehicle_status` varchar(255) DEFAULT NULL,
  `last_location` varchar(255) DEFAULT NULL,
  `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`),
  KEY `staff_id` (`staff_id`),
  CONSTRAINT `drivers_ibfk_1` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`) ON DELETE CASCADE,
  CONSTRAINT `drivers_ibfk_10` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_100` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_101` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_102` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_103` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_104` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_105` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_106` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_107` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_108` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_109` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_11` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_110` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_111` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_112` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_113` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_114` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_115` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_116` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_117` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_118` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_119` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_12` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_120` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_121` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_122` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_123` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_124` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_125` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_126` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_127` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_128` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_129` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_13` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_130` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_131` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_132` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_133` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_134` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_135` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_136` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_14` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_15` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_16` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_17` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_18` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_19` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_2` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_20` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_21` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_22` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_23` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_24` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_25` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_26` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_27` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_28` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_29` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_3` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_30` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_31` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_32` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_33` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_34` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_35` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_36` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_37` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_38` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_39` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_4` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_40` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_41` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_42` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_43` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_44` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_45` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_46` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_47` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_48` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_49` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_5` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_50` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_51` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_52` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_53` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_54` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_55` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_56` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_57` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_58` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_59` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_6` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_60` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_61` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_62` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_63` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_64` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_65` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_66` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_67` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_68` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_69` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_7` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_70` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_71` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_72` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_73` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_74` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_75` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_76` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_77` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_78` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_79` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_8` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_80` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_81` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_82` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_83` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_84` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_85` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_86` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_87` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_88` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_89` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_9` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_90` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_91` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_92` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_93` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_94` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_95` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_96` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_97` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_98` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`),
  CONSTRAINT `drivers_ibfk_99` FOREIGN KEY (`staff_id`) REFERENCES `staff_members` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-08 12:58:58
