package com.oldboss.silverjob.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserCommentStatsVO {

    private Long userId;

    private String userName;

    private Double avgRating;

    private Integer totalScore;

    private Integer commentCount;
}