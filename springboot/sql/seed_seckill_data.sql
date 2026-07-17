-- =============================================
-- 秒杀测试数据种子脚本（完全自包含）
-- 使用方式：在 MySQL 客户端或 IDEA Database 工具中执行本文件
-- =============================================

-- 1. 插入景点分类（如果没有）
INSERT IGNORE INTO scenic_category (id, name, create_time) VALUES (1, '自然风光', NOW());
INSERT IGNORE INTO scenic_category (id, name, create_time) VALUES (2, '历史人文', NOW());

-- 2. 插入景点（如果没有）
INSERT IGNORE INTO scenic_spot (id, name, description, location, category_id, price, opening_hours, image_url, longitude, latitude, create_time, update_time)
VALUES (1, '黄鹤楼景区', '国家5A级旅游景区，武汉标志性景点，登楼可俯瞰长江美景', '湖北省武汉市武昌区', 1, 120.00, '08:00-18:00', 'https://example.com/huanghelou.jpg', 114.3025, 30.5472, NOW(), NOW());

INSERT IGNORE INTO scenic_spot (id, name, description, location, category_id, price, opening_hours, image_url, longitude, latitude, create_time, update_time)
VALUES (2, '三峡大坝', '世界最大水利枢纽工程，壮观的现代工程奇迹', '湖北省宜昌市', 1, 180.00, '08:00-17:30', 'https://example.com/sanxia.jpg', 111.0033, 30.8235, NOW(), NOW());

-- 3. 插入测试门票（幂等：已存在则跳过）
INSERT IGNORE INTO ticket (id, scenic_id, ticket_name, price, discount_price, ticket_type, valid_period, description, stock, status, create_time, update_time)
VALUES (1, 1, '黄鹤楼-成人票', 120.00, 100.00, '成人', '购买后30天内有效', '标准成人门票，含景区所有景点参观', 500, 1, NOW(), NOW());

INSERT IGNORE INTO ticket (id, scenic_id, ticket_name, price, discount_price, ticket_type, valid_period, description, stock, status, create_time, update_time)
VALUES (2, 1, '黄鹤楼-学生票', 60.00, 50.00, '学生', '购买后30天内有效', '全日制在校学生凭学生证购买', 300, 1, NOW(), NOW());

INSERT IGNORE INTO ticket (id, scenic_id, ticket_name, price, discount_price, ticket_type, valid_period, description, stock, status, create_time, update_time)
VALUES (3, 1, '黄鹤楼-家庭套票', 360.00, 280.00, '套票', '购买后60天内有效', '2大1小家庭套票，含景区所有景点', 200, 1, NOW(), NOW());

INSERT IGNORE INTO ticket (id, scenic_id, ticket_name, price, discount_price, ticket_type, valid_period, description, stock, status, create_time, update_time)
VALUES (4, 2, '三峡大坝-成人票', 180.00, 150.00, '成人', '购买后30天内有效', '三峡大坝全景游览，含坛子岭、185平台', 300, 1, NOW(), NOW());

-- 4. 插入秒杀活动（时间窗口已开始+7天有效，立即生效）
INSERT IGNORE INTO seckill_activity (id, ticket_id, seckill_price, seckill_stock, limit_per_user, start_time, end_time, status)
VALUES (1, 1, 39.90, 100, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1);

INSERT IGNORE INTO seckill_activity (id, ticket_id, seckill_price, seckill_stock, limit_per_user, start_time, end_time, status)
VALUES (2, 2, 19.90, 50, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1);

INSERT IGNORE INTO seckill_activity (id, ticket_id, seckill_price, seckill_stock, limit_per_user, start_time, end_time, status)
VALUES (3, 3, 99.90, 30, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1);

INSERT IGNORE INTO seckill_activity (id, ticket_id, seckill_price, seckill_stock, limit_per_user, start_time, end_time, status)
VALUES (4, 4, 59.90, 60, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 1);

-- 5. 验证结果
SELECT '=== 景点 ===' AS info;
SELECT id, name, location, price FROM scenic_spot;

SELECT '=== 门票 ===' AS info;
SELECT id, scenic_id, ticket_name, price, discount_price, stock, status FROM ticket;

SELECT '=== 秒杀活动 ===' AS info;
SELECT a.id, a.ticket_id, t.ticket_name, a.seckill_price, a.seckill_stock, a.limit_per_user,
       a.start_time, a.end_time, a.status
FROM seckill_activity a
LEFT JOIN ticket t ON t.id = a.ticket_id;
