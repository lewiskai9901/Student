package com.school.management.application.inspection.v6;

import com.school.management.domain.inspection.model.v6.TemplateCategory;
import com.school.management.domain.inspection.model.v6.TemplateScoreItem;
import com.school.management.domain.inspection.repository.v6.TemplateCategoryRepository;
import com.school.management.infrastructure.persistence.inspection.v6.TemplateScoreItemMapper;
import com.school.management.infrastructure.persistence.inspection.v6.TemplateScoreItemPO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板扣分项应用服务
 */
@Service
public class TemplateItemApplicationService {

    private final TemplateCategoryRepository categoryRepository;
    private final TemplateScoreItemMapper itemMapper;

    public TemplateItemApplicationService(TemplateCategoryRepository categoryRepository,
                                          TemplateScoreItemMapper itemMapper) {
        this.categoryRepository = categoryRepository;
        this.itemMapper = itemMapper;
    }

    /**
     * 获取模板的完整扣分项结构（类别+项目）
     */
    public List<TemplateCategory> getTemplateItems(Long templateId) {
        return categoryRepository.findByTemplateIdWithItems(templateId);
    }

    /**
     * 获取模板的所有类别
     */
    public List<TemplateCategory> getCategories(Long templateId) {
        return categoryRepository.findByTemplateId(templateId);
    }

    /**
     * 创建类别
     */
    @Transactional
    public TemplateCategory createCategory(CreateCategoryCommand command) {
        if (categoryRepository.existsByTemplateIdAndCode(command.getTemplateId(), command.getCategoryCode())) {
            throw new IllegalArgumentException("类别编码已存在");
        }

        TemplateCategory category = TemplateCategory.create(
            command.getTemplateId(),
            command.getCategoryCode(),
            command.getCategoryName()
        );
        category.setDescription(command.getDescription());
        category.setIcon(command.getIcon());
        category.setColor(command.getColor());
        category.setWeight(command.getWeight() != null ? command.getWeight() : BigDecimal.ONE);
        category.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);

        return categoryRepository.save(category);
    }

    /**
     * 更新类别
     */
    @Transactional
    public void updateCategory(Long categoryId, UpdateCategoryCommand command) {
        TemplateCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("类别不存在"));

        category.setCategoryName(command.getCategoryName());
        category.setDescription(command.getDescription());
        category.setIcon(command.getIcon());
        category.setColor(command.getColor());
        category.setWeight(command.getWeight());
        category.setSortOrder(command.getSortOrder());
        category.setIsEnabled(command.getIsEnabled());
        category.setUpdatedAt(LocalDateTime.now());

        categoryRepository.update(category);
    }

    /**
     * 删除类别
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    /**
     * 获取类别下的扣分项
     */
    public List<TemplateScoreItem> getItemsByCategory(Long categoryId) {
        List<TemplateScoreItemPO> pos = itemMapper.selectByCategoryId(categoryId);
        return pos.stream().map(this::toItemDomain).collect(Collectors.toList());
    }

    /**
     * 创建扣分项
     */
    @Transactional
    public TemplateScoreItem createItem(CreateItemCommand command) {
        TemplateScoreItemPO po = new TemplateScoreItemPO();
        po.setCategoryId(command.getCategoryId());
        po.setItemCode(command.getItemCode());
        po.setItemName(command.getItemName());
        po.setDescription(command.getDescription());
        po.setScoringMode(command.getScoringMode() != null ? command.getScoringMode() : "DEDUCTION");
        po.setScore(command.getScore() != null ? command.getScore() : BigDecimal.ONE);
        po.setMinScore(command.getMinScore());
        po.setMaxScore(command.getMaxScore());
        po.setPerPersonScore(command.getPerPersonScore());
        po.setCanLinkIndividual(command.getCanLinkIndividual() != null ? command.getCanLinkIndividual() : false);
        po.setRequiresPhoto(command.getRequiresPhoto() != null ? command.getRequiresPhoto() : false);
        po.setRequiresRemark(command.getRequiresRemark() != null ? command.getRequiresRemark() : false);
        po.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);
        po.setIsEnabled(true);

        itemMapper.insert(po);
        return toItemDomain(po);
    }

    /**
     * 更新扣分项
     */
    @Transactional
    public void updateItem(Long itemId, UpdateItemCommand command) {
        TemplateScoreItemPO po = itemMapper.selectById(itemId);
        if (po == null) {
            throw new IllegalArgumentException("扣分项不存在");
        }

        po.setItemName(command.getItemName());
        po.setDescription(command.getDescription());
        po.setScoringMode(command.getScoringMode());
        po.setScore(command.getScore());
        po.setMinScore(command.getMinScore());
        po.setMaxScore(command.getMaxScore());
        po.setPerPersonScore(command.getPerPersonScore());
        po.setCanLinkIndividual(command.getCanLinkIndividual());
        po.setRequiresPhoto(command.getRequiresPhoto());
        po.setRequiresRemark(command.getRequiresRemark());
        po.setSortOrder(command.getSortOrder());
        po.setIsEnabled(command.getIsEnabled());

        itemMapper.updateById(po);
    }

    /**
     * 删除扣分项
     */
    @Transactional
    public void deleteItem(Long itemId) {
        itemMapper.deleteById(itemId);
    }

    private TemplateScoreItem toItemDomain(TemplateScoreItemPO po) {
        TemplateScoreItem item = new TemplateScoreItem();
        BeanUtils.copyProperties(po, item);
        if (po.getScoringMode() != null) {
            item.setScoringMode(TemplateScoreItem.ScoringMode.valueOf(po.getScoringMode()));
        }
        return item;
    }

    // Command classes
    public static class CreateCategoryCommand {
        private Long templateId;
        private String categoryCode;
        private String categoryName;
        private String description;
        private String icon;
        private String color;
        private BigDecimal weight;
        private Integer sortOrder;

        // Getters and setters
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateCategoryCommand {
        private String categoryName;
        private String description;
        private String icon;
        private String color;
        private BigDecimal weight;
        private Integer sortOrder;
        private Boolean isEnabled;

        // Getters and setters
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Boolean getIsEnabled() { return isEnabled; }
        public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    }

    public static class CreateItemCommand {
        private Long categoryId;
        private String itemCode;
        private String itemName;
        private String description;
        private String scoringMode;
        private BigDecimal score;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private BigDecimal perPersonScore;
        private Boolean canLinkIndividual;
        private Boolean requiresPhoto;
        private Boolean requiresRemark;
        private Integer sortOrder;

        // Getters and setters
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getScoringMode() { return scoringMode; }
        public void setScoringMode(String scoringMode) { this.scoringMode = scoringMode; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public BigDecimal getMinScore() { return minScore; }
        public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        public BigDecimal getPerPersonScore() { return perPersonScore; }
        public void setPerPersonScore(BigDecimal perPersonScore) { this.perPersonScore = perPersonScore; }
        public Boolean getCanLinkIndividual() { return canLinkIndividual; }
        public void setCanLinkIndividual(Boolean canLinkIndividual) { this.canLinkIndividual = canLinkIndividual; }
        public Boolean getRequiresPhoto() { return requiresPhoto; }
        public void setRequiresPhoto(Boolean requiresPhoto) { this.requiresPhoto = requiresPhoto; }
        public Boolean getRequiresRemark() { return requiresRemark; }
        public void setRequiresRemark(Boolean requiresRemark) { this.requiresRemark = requiresRemark; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateItemCommand {
        private String itemName;
        private String description;
        private String scoringMode;
        private BigDecimal score;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private BigDecimal perPersonScore;
        private Boolean canLinkIndividual;
        private Boolean requiresPhoto;
        private Boolean requiresRemark;
        private Integer sortOrder;
        private Boolean isEnabled;

        // Getters and setters
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getScoringMode() { return scoringMode; }
        public void setScoringMode(String scoringMode) { this.scoringMode = scoringMode; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public BigDecimal getMinScore() { return minScore; }
        public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        public BigDecimal getPerPersonScore() { return perPersonScore; }
        public void setPerPersonScore(BigDecimal perPersonScore) { this.perPersonScore = perPersonScore; }
        public Boolean getCanLinkIndividual() { return canLinkIndividual; }
        public void setCanLinkIndividual(Boolean canLinkIndividual) { this.canLinkIndividual = canLinkIndividual; }
        public Boolean getRequiresPhoto() { return requiresPhoto; }
        public void setRequiresPhoto(Boolean requiresPhoto) { this.requiresPhoto = requiresPhoto; }
        public Boolean getRequiresRemark() { return requiresRemark; }
        public void setRequiresRemark(Boolean requiresRemark) { this.requiresRemark = requiresRemark; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Boolean getIsEnabled() { return isEnabled; }
        public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    }
}
