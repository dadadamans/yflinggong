package com.oldboss.silverjob.vo;

import lombok.Data;

import java.util.List;

@Data
public class CategoryVO {
    private String value;
    private String label;
    private List<CategoryItemVO> children;
}
