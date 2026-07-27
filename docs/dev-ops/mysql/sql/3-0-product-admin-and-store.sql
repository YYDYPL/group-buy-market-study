USE `group_buy_market`;
SET NAMES utf8mb4;

-- ============================================================
-- 拼团商城展示字段、后台配置字段与演示数据
-- 说明：
-- 1. 本脚本在 2-29-group_buy_market.sql 之后执行；
-- 2. 通过 information_schema 判断字段是否存在，允许重复导入；
-- 3. 已有交易表不做破坏性修改，历史订单仍然引用原 activity_id。
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing$$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', table_name_value,
            '` ADD COLUMN `', column_name_value, '` ',
            column_definition_value
        );
        PREPARE statement_to_execute FROM @ddl;
        EXECUTE statement_to_execute;
        DEALLOCATE PREPARE statement_to_execute;
    END IF;
END$$

DELIMITER ;

CALL add_column_if_missing('sku', 'category', "varchar(32) NOT NULL DEFAULT '百货' COMMENT '商城分类'");
CALL add_column_if_missing('sku', 'subtitle', "varchar(256) NOT NULL DEFAULT '' COMMENT '商品卖点'");
CALL add_column_if_missing('sku', 'main_image', "varchar(512) NOT NULL DEFAULT '' COMMENT '商品主图'");
CALL add_column_if_missing('sku', 'gallery_images', "json DEFAULT NULL COMMENT '轮播图 URL 数组'");
CALL add_column_if_missing('sku', 'sales_count', "int unsigned NOT NULL DEFAULT 0 COMMENT '展示销量'");
CALL add_column_if_missing('sku', 'favorable_rate', "decimal(5,2) NOT NULL DEFAULT 100.00 COMMENT '好评率'");
CALL add_column_if_missing('sku', 'service_tags', "json DEFAULT NULL COMMENT '服务标签数组'");
CALL add_column_if_missing('sku', 'sort_order', "int NOT NULL DEFAULT 0 COMMENT '商城排序，越大越靠前'");
CALL add_column_if_missing('sku', 'status', "tinyint(1) NOT NULL DEFAULT 0 COMMENT '展示状态；0草稿、1上架、2下架'");
CALL add_column_if_missing('sku', 'version', "int unsigned NOT NULL DEFAULT 0 COMMENT '乐观锁版本'");

CALL add_column_if_missing('group_buy_activity', 'goods_id', "varchar(16) DEFAULT NULL COMMENT '后台配置关联商品'");
CALL add_column_if_missing('group_buy_activity', 'source', "varchar(8) DEFAULT NULL COMMENT '活动来源'");
CALL add_column_if_missing('group_buy_activity', 'channel', "varchar(8) DEFAULT NULL COMMENT '活动渠道'");
CALL add_column_if_missing('group_buy_activity', 'config_version', "int unsigned NOT NULL DEFAULT 0 COMMENT '商品配置版本'");
CALL add_column_if_missing('group_buy_activity', 'draft_data', "JSON DEFAULT NULL COMMENT '发布快照或草稿商品资料'");

CALL add_column_if_missing('sc_sku_activity', 'status', "tinyint(1) NOT NULL DEFAULT 1 COMMENT '路由状态；0停用、1启用'");

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- 商品主数据。图片全部指向当前 Nginx 目录中的本地资源。
INSERT INTO `sku` (
    `source`, `channel`, `goods_id`, `goods_name`, `original_price`,
    `category`, `subtitle`, `main_image`, `gallery_images`,
    `sales_count`, `favorable_rate`, `service_tags`, `sort_order`, `status`, `version`
) VALUES
('s01','c01','9890001','《手写MyBatis：渐进式源码实践》',100.00,
 '学习办公','从零手写源码，系统掌握 MyBatis 核心设计','images/products/product-9890001.png',
 JSON_ARRAY('images/products/product-9890001.png','images/sku-13811216-01.png','images/sku-13811216-02.png'),
 2865,99.80,JSON_ARRAY('全场包邮','正品保障','7天无理由'),80,1,0),
('s01','c01','9890002','丹东红颜草莓净重1.5kg',39.90,
 '生鲜水果','当季采摘，大果香甜，产地泡沫箱直发','images/products/product-9890002.png',
 JSON_ARRAY('images/products/product-9890002.png'),
 12843,98.90,JSON_ARRAY('坏果包赔','产地直发','全场包邮'),100,1,0),
('s01','c01','9890003','原生木浆抽纸24包',35.90,
 '家清纸品','加厚四层柔韧不易破，家庭整箱装','images/products/product-9890003.png',
 JSON_ARRAY('images/products/product-9890003.png'),
 32654,99.20,JSON_ARRAY('全场包邮','破损包赔','48小时发货'),95,1,0),
('s01','c01','9890004','除菌香氛洗衣液3kg×2',69.90,
 '家清纸品','持久留香，深层洁净，家庭囤货装','images/products/product-9890004.png',
 JSON_ARRAY('images/products/product-9890004.png'),
 8762,98.70,JSON_ARRAY('全场包邮','正品保障','破损包退'),90,1,0),
('s01','c01','9890005','每日坚果零食礼盒30袋',59.90,
 '食品零食','每日独立包装，多种坚果果干科学搭配','images/products/product-9890005.png',
 JSON_ARRAY('images/products/product-9890005.png'),
 16980,99.10,JSON_ARRAY('新鲜日期','坏包包赔','全场包邮'),88,1,0),
('s01','c01','9890006','真无线蓝牙降噪耳机',159.00,
 '数码电器','轻巧入耳，长效续航，通勤运动都好用','images/products/product-9890006.png',
 JSON_ARRAY('images/products/product-9890006.png'),
 6531,97.90,JSON_ARRAY('一年质保','7天无理由','极速退款'),85,1,0),
('s01','c01','9890007','316不锈钢保温杯',79.90,
 '家居生活','食品级316内胆，轻量防漏，长效保温','images/products/product-9890007.png',
 JSON_ARRAY('images/products/product-9890007.png'),
 11206,98.80,JSON_ARRAY('全场包邮','破损包赔','7天无理由'),82,1,0),
('s01','c01','9890008','豆腐猫砂6L×4袋',79.90,
 '宠物用品','快速结团低粉尘，可冲厕所，囤货更省心','images/products/product-9890008.png',
 JSON_ARRAY('images/products/product-9890008.png'),
 9328,98.60,JSON_ARRAY('全场包邮','漏袋包赔','48小时发货'),78,1,0)
ON DUPLICATE KEY UPDATE
    `source` = VALUES(`source`),
    `channel` = VALUES(`channel`),
    `goods_name` = VALUES(`goods_name`),
    `original_price` = VALUES(`original_price`),
    `category` = VALUES(`category`),
    `subtitle` = VALUES(`subtitle`),
    `main_image` = VALUES(`main_image`),
    `gallery_images` = VALUES(`gallery_images`),
    `sales_count` = VALUES(`sales_count`),
    `favorable_rate` = VALUES(`favorable_rate`),
    `service_tags` = VALUES(`service_tags`),
    `sort_order` = VALUES(`sort_order`),
    `status` = VALUES(`status`);

INSERT INTO `group_buy_discount` (
    `discount_id`, `discount_name`, `discount_desc`,
    `discount_type`, `market_plan`, `market_expr`, `tag_id`
) VALUES
('25120207','图书拼团直减','拼团立减20元',0,'ZJ','20',NULL),
('26070002','草莓尝鲜价','双人团19.9元',0,'N','19.90',NULL),
('26070003','抽纸囤货直减','拼团立减12元',0,'ZJ','12',NULL),
('26070004','洗衣液满减','满59减20元',0,'MJ','59,20',NULL),
('26070005','坚果礼盒拼团价','双人团39.9元',0,'N','39.90',NULL),
('26070006','耳机限时折扣','拼团享6.9折',0,'ZK','0.69',NULL),
('26070007','保温杯直减','拼团立减25元',0,'ZJ','25',NULL),
('26070008','猫砂囤货满减','满69减20元',0,'MJ','69,20',NULL)
ON DUPLICATE KEY UPDATE
    `discount_name` = VALUES(`discount_name`),
    `discount_desc` = VALUES(`discount_desc`),
    `discount_type` = VALUES(`discount_type`),
    `market_plan` = VALUES(`market_plan`),
    `market_expr` = VALUES(`market_expr`),
    `tag_id` = VALUES(`tag_id`);

INSERT INTO `group_buy_activity` (
    `activity_id`, `activity_name`, `discount_id`, `group_type`,
    `take_limit_count`, `target`, `valid_time`, `status`,
    `start_time`, `end_time`, `tag_id`, `tag_scope`,
    `goods_id`, `source`, `channel`, `config_version`
) VALUES
(100123,'图书3人学习团','25120207',0,3,3,180,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890001','s01','c01',0),
(100124,'草莓双人尝鲜团','26070002',0,3,2,30,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890002','s01','c01',0),
(100125,'抽纸3人囤货团','26070003',0,3,3,20,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890003','s01','c01',0),
(100126,'洗衣液3人家庭团','26070004',0,3,3,30,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890004','s01','c01',0),
(100127,'坚果双人分享团','26070005',0,3,2,25,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890005','s01','c01',0),
(100128,'耳机双人优惠团','26070006',0,3,2,45,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890006','s01','c01',0),
(100129,'保温杯双人拼团','26070007',0,3,2,30,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890007','s01','c01',0),
(100130,'猫砂3人囤货团','26070008',0,3,3,40,1,'2024-01-01 00:00:00','2035-12-31 23:59:59',NULL,NULL,'9890008','s01','c01',0)
ON DUPLICATE KEY UPDATE
    `activity_name` = VALUES(`activity_name`),
    `discount_id` = VALUES(`discount_id`),
    `group_type` = VALUES(`group_type`),
    `take_limit_count` = VALUES(`take_limit_count`),
    `target` = VALUES(`target`),
    `valid_time` = VALUES(`valid_time`),
    `status` = VALUES(`status`),
    `start_time` = VALUES(`start_time`),
    `end_time` = VALUES(`end_time`),
    `tag_id` = VALUES(`tag_id`),
    `tag_scope` = VALUES(`tag_scope`),
    `goods_id` = VALUES(`goods_id`),
    `source` = VALUES(`source`),
    `channel` = VALUES(`channel`);

INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `goods_id`, `status`)
VALUES
('s01','c01',100123,'9890001',1),
('s01','c01',100124,'9890002',1),
('s01','c01',100125,'9890003',1),
('s01','c01',100126,'9890004',1),
('s01','c01',100127,'9890005',1),
('s01','c01',100128,'9890006',1),
('s01','c01',100129,'9890007',1),
('s01','c01',100130,'9890008',1)
ON DUPLICATE KEY UPDATE
    `activity_id` = VALUES(`activity_id`),
    `status` = VALUES(`status`),
    `update_time` = NOW();
