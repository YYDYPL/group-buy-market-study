USE `group_buy_market`;
SET NAMES utf8mb4;

-- 商城订单中心会按 team_id 查询全体成员。该脚本只补索引，允许重复执行。
DELIMITER $$

DROP PROCEDURE IF EXISTS add_index_if_missing$$
CREATE PROCEDURE add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_columns_value VARCHAR(256)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND INDEX_NAME = index_name_value
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', table_name_value,
            '` ADD INDEX `', index_name_value, '` (', index_columns_value, ')'
        );
        PREPARE statement_to_execute FROM @ddl;
        EXECUTE statement_to_execute;
        DEALLOCATE PREPARE statement_to_execute;
    END IF;
END$$

DELIMITER ;

CALL add_index_if_missing('group_buy_order_list', 'idx_team_id', '`team_id`');

DROP PROCEDURE IF EXISTS add_index_if_missing;
