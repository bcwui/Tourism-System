-- 秒杀活动表
CREATE TABLE IF NOT EXISTS `seckill_activity` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    `ticket_id` BIGINT NOT NULL COMMENT '门票ID',
    `seckill_price` DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    `seckill_stock` INT NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    `limit_per_user` INT NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-未开始, 1-进行中, 2-已结束',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_ticket_id` (`ticket_id`),
    INDEX `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';
