# baseline_v3 之后的增量迁移目录
#
# ⚠ init-all **不会** apply 这些文件 —— fresh init 只加载 baseline_v3 (已含全部效果)。
# 本目录仅供"已有、且落后于 baseline 的 dev 库"手动按序追赶 (sort -V)。
# 这些迁移非幂等 (项目纪律: 不写条件化迁移, 是裸 ALTER/INSERT) → 对已最新/fresh 的库重放
# 会报"重复列 / DROP 不存在 / 主键冲突"。所以勿让 init-all 跑它们。
#
# 纪律: 改 schema = 同时改 baseline_v3 建表 SQL + 放一个 post-v3 迁移。
# squash 进 baseline 后 (或库可随时重建时) 即清空本目录。
