package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.ResultCode;
import com.school.management.entity.CheckCategory;
import com.school.management.entity.CheckItem;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckCategoryMapper;
import com.school.management.mapper.CheckItemMapper;
import com.school.management.service.CheckDictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查字典服务实现类 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckDictionaryServiceImpl implements CheckDictionaryService {

    private final CheckCategoryMapper categoryMapper;
    private final CheckItemMapper itemMapper;

    // ==================== 检查类别管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCheckCategory(Map<String, Object> request) {
        // 兼容前端两种字段命名方式: categoryName/typeName, categoryCode/typeCode
        String categoryName = request.get("categoryName") != null ?
            (String) request.get("categoryName") : (String) request.get("typeName");
        String categoryCode = request.get("categoryCode") != null ?
            (String) request.get("categoryCode") : (String) request.get("typeCode");

        log.info("创建检查类别: {}", categoryName);

        CheckCategory category = new CheckCategory();
        category.setCategoryCode(categoryCode);
        category.setCategoryName(categoryName);
        category.setCategoryType((String) request.get("categoryType"));

        // defaultMaxScore 可选，默认100
        if (request.get("defaultMaxScore") != null) {
            category.setDefaultMaxScore(new BigDecimal(request.get("defaultMaxScore").toString()));
        } else {
            category.setDefaultMaxScore(new BigDecimal("100"));
        }

        category.setDescription((String) request.get("description"));
        category.setIcon((String) request.get("icon"));
        category.setSortOrder((Integer) request.getOrDefault("sortOrder", 0));

        // 设置默认检查轮次
        if (request.get("defaultRounds") != null) {
            category.setDefaultRounds((Integer) request.get("defaultRounds"));
        } else {
            category.setDefaultRounds(1);
        }

        // 兼容 status 和 isActive 字段
        if (request.get("status") != null) {
            category.setStatus((Integer) request.get("status"));
        } else if (request.get("isActive") != null) {
            category.setStatus((Integer) request.get("isActive"));
        } else {
            category.setStatus(1);
        }

        categoryMapper.insert(category);

        log.info("检查类别创建成功, ID: {}", category.getId());

        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckCategory(Long id, Map<String, Object> request) {
        log.info("更新检查类别: id={}", id);

        CheckCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查类别不存在");
        }

        // 兼容 categoryName 和 typeName
        if (request.containsKey("categoryName")) {
            category.setCategoryName((String) request.get("categoryName"));
        } else if (request.containsKey("typeName")) {
            category.setCategoryName((String) request.get("typeName"));
        }

        // 兼容 categoryCode 和 typeCode
        if (request.containsKey("categoryCode")) {
            category.setCategoryCode((String) request.get("categoryCode"));
        } else if (request.containsKey("typeCode")) {
            category.setCategoryCode((String) request.get("typeCode"));
        }

        if (request.containsKey("defaultMaxScore")) {
            category.setDefaultMaxScore(new BigDecimal(request.get("defaultMaxScore").toString()));
        }
        if (request.containsKey("description")) {
            category.setDescription((String) request.get("description"));
        }
        if (request.containsKey("icon")) {
            category.setIcon((String) request.get("icon"));
        }
        if (request.containsKey("sortOrder")) {
            category.setSortOrder((Integer) request.get("sortOrder"));
        }

        // 更新默认检查轮次
        if (request.containsKey("defaultRounds")) {
            category.setDefaultRounds((Integer) request.get("defaultRounds"));
        }

        // 兼容 status 和 isActive
        if (request.containsKey("status")) {
            category.setStatus((Integer) request.get("status"));
        } else if (request.containsKey("isActive")) {
            category.setStatus((Integer) request.get("isActive"));
        }

        categoryMapper.updateById(category);

        log.info("检查类别更新成功, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheckCategory(Long id) {
        log.info("删除检查类别: id={}", id);

        CheckCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查类别不存在");
        }

        // 级联删除该类别下的所有检查项（扣分项）
        int itemCount = categoryMapper.countItemsByCategoryId(id);
        if (itemCount > 0) {
            int deletedItems = itemMapper.deleteByCategoryId(id);
            log.info("级联删除检查项: categoryId={}, deletedCount={}", id, deletedItems);
        }

        categoryMapper.deleteById(id);

        log.info("检查类别删除成功, id={}", id);
    }

    @Override
    public Map<String, Object> getCheckCategoryDetail(Long id) {
        log.info("获取检查类别详情: id={}", id);

        CheckCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查类别不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", category.getId());
        result.put("categoryCode", category.getCategoryCode());
        result.put("categoryName", category.getCategoryName());
        result.put("categoryType", category.getCategoryType());
        result.put("defaultMaxScore", category.getDefaultMaxScore());
        result.put("description", category.getDescription());
        result.put("icon", category.getIcon());
        result.put("sortOrder", category.getSortOrder());
        result.put("status", category.getStatus());

        return result;
    }

    @Override
    public IPage<Map<String, Object>> pageCheckCategories(Page<Map<String, Object>> page, Map<String, Object> query) {
        log.info("分页查询检查类别: page={}, query={}", page.getCurrent(), query);

        return categoryMapper.selectCategoryPage(page, query);
    }

    @Override
    public List<Map<String, Object>> getAllEnabledCategories() {
        log.info("查询所有启用的检查类别");

        List<CheckCategory> categories = categoryMapper.selectAllEnabled();

        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckCategory category : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", category.getId());
            map.put("categoryCode", category.getCategoryCode());
            map.put("categoryName", category.getCategoryName());
            map.put("categoryType", category.getCategoryType());
            map.put("defaultMaxScore", category.getDefaultMaxScore());
            map.put("icon", category.getIcon());
            result.add(map);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getCategoriesByType(String categoryType) {
        log.info("根据类别类型查询: categoryType={}", categoryType);

        List<CheckCategory> categories = categoryMapper.selectByCategoryType(categoryType);

        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckCategory category : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", category.getId());
            map.put("categoryCode", category.getCategoryCode());
            map.put("categoryName", category.getCategoryName());
            map.put("defaultMaxScore", category.getDefaultMaxScore());
            result.add(map);
        }

        return result;
    }

    // ==================== 检查项管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCheckItem(Map<String, Object> request) {
        log.info("创建检查项: {}", request.get("itemName"));

        CheckItem item = new CheckItem();
        // 支持Number和String类型的categoryId (JavaScript大整数会转换为字符串)
        Object categoryIdObj = request.get("categoryId");
        if (categoryIdObj instanceof Number) {
            item.setCategoryId(((Number) categoryIdObj).longValue());
        } else if (categoryIdObj instanceof String) {
            item.setCategoryId(Long.parseLong((String) categoryIdObj));
        } else {
            throw new BusinessException("categoryId参数类型错误，必须为数字或字符串");
        }
        item.setItemCode((String) request.get("itemCode"));
        item.setItemName((String) request.get("itemName"));
        item.setItemDescription((String) request.get("itemDescription"));
        item.setDeductMode((Integer) request.get("deductMode"));
        item.setDefaultDeductScore(new BigDecimal(request.get("defaultDeductScore").toString()));
        item.setMinDeductScore(new BigDecimal(request.get("minDeductScore").toString()));
        item.setMaxDeductScore(new BigDecimal(request.get("maxDeductScore").toString()));
        item.setCheckPoints((String) request.get("checkPoints"));
        item.setSortOrder((Integer) request.getOrDefault("sortOrder", 0));
        item.setStatus(1);

        itemMapper.insert(item);

        log.info("检查项创建成功, ID: {}", item.getId());

        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCheckItem(Long id, Map<String, Object> request) {
        log.info("更新检查项: id={}", id);

        CheckItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查项不存在");
        }

        if (request.containsKey("itemName")) {
            item.setItemName((String) request.get("itemName"));
        }
        if (request.containsKey("itemDescription")) {
            item.setItemDescription((String) request.get("itemDescription"));
        }
        if (request.containsKey("defaultDeductScore")) {
            item.setDefaultDeductScore(new BigDecimal(request.get("defaultDeductScore").toString()));
        }
        if (request.containsKey("minDeductScore")) {
            item.setMinDeductScore(new BigDecimal(request.get("minDeductScore").toString()));
        }
        if (request.containsKey("maxDeductScore")) {
            item.setMaxDeductScore(new BigDecimal(request.get("maxDeductScore").toString()));
        }
        if (request.containsKey("checkPoints")) {
            item.setCheckPoints((String) request.get("checkPoints"));
        }
        if (request.containsKey("sortOrder")) {
            item.setSortOrder((Integer) request.get("sortOrder"));
        }
        if (request.containsKey("status")) {
            item.setStatus((Integer) request.get("status"));
        }

        itemMapper.updateById(item);

        log.info("检查项更新成功, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheckItem(Long id) {
        log.info("删除检查项: id={}", id);

        CheckItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查项不存在");
        }

        itemMapper.deleteById(id);

        log.info("检查项删除成功, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCheckItems(List<Long> ids) {
        log.info("批量删除检查项: ids={}", ids);

        for (Long id : ids) {
            deleteCheckItem(id);
        }

        log.info("批量删除检查项成功, count={}", ids.size());
    }

    @Override
    public Map<String, Object> getCheckItemDetail(Long id) {
        log.info("获取检查项详情: id={}", id);

        CheckItem item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "检查项不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("categoryId", item.getCategoryId());
        result.put("itemCode", item.getItemCode());
        result.put("itemName", item.getItemName());
        result.put("itemDescription", item.getItemDescription());
        result.put("deductMode", item.getDeductMode());
        result.put("defaultDeductScore", item.getDefaultDeductScore());
        result.put("minDeductScore", item.getMinDeductScore());
        result.put("maxDeductScore", item.getMaxDeductScore());
        result.put("checkPoints", item.getCheckPoints());
        result.put("sortOrder", item.getSortOrder());
        result.put("status", item.getStatus());

        return result;
    }

    @Override
    public IPage<Map<String, Object>> pageCheckItems(Page<Map<String, Object>> page, Map<String, Object> query) {
        log.info("分页查询检查项: page={}, query={}", page.getCurrent(), query);

        return itemMapper.selectItemPage(page, query);
    }

    @Override
    public List<Map<String, Object>> getItemsByCategoryId(Long categoryId) {
        log.info("根据类别ID查询检查项: categoryId={}", categoryId);

        List<CheckItem> items = itemMapper.selectEnabledByCategoryId(categoryId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("itemCode", item.getItemCode());
            map.put("itemName", item.getItemName());
            map.put("deductMode", item.getDeductMode());
            map.put("defaultDeductScore", item.getDefaultDeductScore());
            map.put("minDeductScore", item.getMinDeductScore());
            map.put("maxDeductScore", item.getMaxDeductScore());
            result.add(map);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getAllEnabledItems() {
        log.info("查询所有启用的检查项");

        List<CheckItem> items = itemMapper.selectAllEnabled();

        List<Map<String, Object>> result = new ArrayList<>();
        for (CheckItem item : items) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("categoryId", item.getCategoryId());
            map.put("itemCode", item.getItemCode());
            map.put("itemName", item.getItemName());
            map.put("deductMode", item.getDeductMode());
            map.put("defaultDeductScore", item.getDefaultDeductScore());
            result.add(map);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImportCheckItems(Long categoryId, List<Map<String, Object>> items) {
        log.info("批量导入检查项: categoryId={}, count={}", categoryId, items.size());

        List<CheckItem> itemList = new ArrayList<>();
        for (Map<String, Object> itemMap : items) {
            CheckItem item = new CheckItem();
            item.setCategoryId(categoryId);
            item.setItemCode((String) itemMap.get("itemCode"));
            item.setItemName((String) itemMap.get("itemName"));
            item.setItemDescription((String) itemMap.get("itemDescription"));
            item.setDeductMode((Integer) itemMap.get("deductMode"));
            item.setDefaultDeductScore(new BigDecimal(itemMap.get("defaultDeductScore").toString()));
            item.setMinDeductScore(new BigDecimal(itemMap.get("minDeductScore").toString()));
            item.setMaxDeductScore(new BigDecimal(itemMap.get("maxDeductScore").toString()));
            item.setCheckPoints((String) itemMap.get("checkPoints"));
            item.setSortOrder((Integer) itemMap.getOrDefault("sortOrder", 0));
            item.setStatus(1);

            itemList.add(item);
        }

        int count = itemMapper.batchInsert(itemList);

        log.info("批量导入检查项成功, count={}", count);

        return count;
    }
}
