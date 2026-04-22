package com.oldboss.silverjob.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdminStatsVO {
    private Long elderlyCount;
    private Long employerCount;
    private Long childCount;
    private Long taskCount;
    private Long waitingCount;
    private Long workingCount;
    private Long pendingPaymentCount;
    private Long doneCount;
    private Long bindCount;
    private Long confirmedBindCount;
    private List<Map<String, Object>> orderStatusDist;
    private List<Map<String, Object>> taskTypeDist;
    private List<Map<String, Object>> userTrend;
    private List<Map<String, Object>> taskTrend;
}