-- 登录页自定义配置项
-- 创建日期：2026-01-26

-- 登录页显示模式：fullScreen(全屏)/halfScreen(半屏)
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(99, 'ui.login.display_mode', 'halfScreen', 'string', 'ui', '显示模式', '显示模式：fullScreen(全屏)/halfScreen(半屏)', 0, 99, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页背景模式：gradient(渐变)/image(图片)/video(视频)
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(100, 'ui.login.background_mode', 'gradient', 'string', 'ui', '登录页背景模式', '背景模式：gradient(渐变)/image(图片)/video(视频)', 0, 100, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页背景图片URL（支持静态图和GIF）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(101, 'ui.login.background_image', '', 'string', 'ui', '登录页背景图片', '背景图片URL（支持jpg/png/gif）', 0, 101, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页背景视频URL（MP4格式）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(102, 'ui.login.background_video', '', 'string', 'ui', '登录页背景视频', '背景视频URL（MP4格式）', 0, 102, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页主标题（空则使用系统名称）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(103, 'ui.login.title', '', 'string', 'ui', '登录页主标题', '主标题（为空则使用系统名称）', 0, 103, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页副标题（支持 **关键词** 语法高亮）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(104, 'ui.login.subtitle', '专业**智能**教育管理平台', 'string', 'ui', '登录页副标题', '副标题文字，用 **关键词** 可高亮显示', 0, 104, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页特性列表（JSON数组）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(105, 'ui.login.features', '[{"icon":"users","text":"学生信息一站式管理"},{"icon":"chart","text":"量化考核智能分析"},{"icon":"building","text":"宿舍资源可视化管理"}]', 'json', 'ui', '登录页特性列表', '特性列表JSON数组', 0, 105, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页是否显示Logo
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(106, 'ui.login.show_logo', 'true', 'boolean', 'ui', '显示登录页Logo', '是否显示Logo图标', 0, 106, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页渐变起始颜色
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(107, 'ui.login.gradient_from', '#2563eb', 'string', 'ui', '渐变起始颜色', '登录页渐变背景起始颜色', 0, 107, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页渐变中间颜色
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(108, 'ui.login.gradient_via', '#1d4ed8', 'string', 'ui', '渐变中间颜色', '登录页渐变背景中间颜色', 0, 108, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页渐变结束颜色
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(109, 'ui.login.gradient_to', '#4338ca', 'string', 'ui', '渐变结束颜色', '登录页渐变背景结束颜色', 0, 109, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页文字位置（top/center/bottom）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(110, 'ui.login.text_position', 'center', 'string', 'ui', '文字位置', '品牌文字位置：top(顶部)/center(居中)/bottom(底部)', 0, 110, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页表单背景透明度（0-100）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(111, 'ui.login.form_bg_opacity', '100', 'string', 'ui', '表单背景透明度', '右侧登录表单背景透明度（0-100）', 0, 111, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 登录页遮罩透明度（0-100）
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
(112, 'ui.login.overlay_opacity', '40', 'string', 'ui', '遮罩透明度', '图片/视频背景遮罩透明度（0-100）', 0, 112, 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();
