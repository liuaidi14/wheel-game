-- ============================================================
-- 对战模块后端优化 ①：wheel_groups 表加 type 字段
-- 执行前请备份数据库！
-- ============================================================

-- 1. 加 type 字段（默认 'life'，兼容已有数据）
ALTER TABLE wheel_groups ADD COLUMN type VARCHAR(20) DEFAULT 'life';

-- 2.（可选）如果你之前在前端用 battleGroupIds 标记过某些组为对战组，
--    可以手动把这些组的 type 改为 'battle'。
--    把下面的 1, 2, 3 替换成你实际的对战组 ID：
-- UPDATE wheel_groups SET type = 'battle' WHERE id IN (1, 2, 3);

-- 3. 修复 wheel_attributes 唯一约束（必须执行！）
--    原约束 uk_user_attr_name 是 (user_id, name)，导致同一用户在不同组不能有同名属性，
--    copyGroup 复制属性时会报 Duplicate entry。改为 (user_id, group_id, name)。
ALTER TABLE wheel_attributes DROP INDEX uk_user_attr_name;
ALTER TABLE wheel_attributes ADD UNIQUE KEY uk_user_group_attr_name (user_id, group_id, name);

-- 4. 验证
SELECT id, user_id, name, type FROM wheel_groups;
SHOW INDEX FROM wheel_attributes WHERE Key_name = 'uk_user_group_attr_name';
