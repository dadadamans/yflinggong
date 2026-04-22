package com.oldboss.silverjob.common;

import com.oldboss.silverjob.vo.CategoryItemVO;
import com.oldboss.silverjob.vo.CategoryVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum TaskCategory {

    SKILL("skill", "技能型", Arrays.asList(
        "health_care:健康护理",
        "rehab:康复指导",
        "housekeeping:家政服务",
        "cooking:烹饪料理",
        "escort:陪诊陪检",
        "medicine:药物管理",
        "errands:代办事务"
    )),

    EXPERIENCE("experience", "体验型", Arrays.asList(
        "chat:陪伴聊天",
        "chess:棋牌娱乐",
        "art:文艺活动",
        "walk:散步陪同",
        "movie:观影陪伴",
        "phone_teach:手机教学"
    )),

    MUTUAL("mutual", "互助型", Arrays.asList(
        "neighbor:邻里互助",
        "borrow:物品借用",
        "assist:简单协助",
        "weather:天气提醒",
        "emotional:情感慰藉"
    ));

    private final String code;
    private final String label;
    private final List<String> subTypeConfigs;

    TaskCategory(String code, String label, List<String> subTypeConfigs) {
        this.code = code;
        this.label = label;
        this.subTypeConfigs = subTypeConfigs;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public List<CategoryItemVO> getSubTypes() {
        return subTypeConfigs.stream()
            .map(config -> {
                String[] parts = config.split(":");
                CategoryItemVO item = new CategoryItemVO();
                item.setValue(parts[0]);
                item.setLabel(parts[1]);
                return item;
            })
            .collect(Collectors.toList());
    }

    public static TaskCategory getByCode(String code) {
        for (TaskCategory cat : values()) {
            if (cat.code.equals(code)) {
                return cat;
            }
        }
        return null;
    }

    public static TaskCategory getBySubType(String subType) {
        for (TaskCategory cat : values()) {
            for (String config : cat.subTypeConfigs) {
                if (config.startsWith(subType + ":")) {
                    return cat;
                }
            }
        }
        return null;
    }

    public static String getLabelByCode(String code) {
        TaskCategory cat = getByCode(code);
        return cat != null ? cat.label : null;
    }

    public static String getChineseLabel(String taskType) {
        if (taskType == null || !taskType.contains(":")) {
            return taskType;
        }
        String[] parts = taskType.split(":");
        String category = parts[0];
        String subType = parts[1];

        TaskCategory cat = getByCode(category);
        if (cat != null) {
            for (CategoryItemVO sub : cat.getSubTypes()) {
                if (sub.getValue().equals(subType)) {
                    return sub.getLabel();
                }
            }
        }
        return taskType;
    }

    public static List<CategoryVO> toCascadeList() {
        return Arrays.stream(values())
            .map(cat -> {
                CategoryVO vo = new CategoryVO();
                vo.setValue(cat.code);
                vo.setLabel(cat.label);
                vo.setChildren(cat.getSubTypes());
                return vo;
            })
            .collect(Collectors.toList());
    }
}
