package com.hjs.study.domain.tag.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * 人群标签批次任务实体。
 * <p>
 * 该实体描述的是“标签域真正执行计算时需要用到的规则快照”，
 * 例如按参与次数统计还是按消费金额统计、统计时间窗口是什么、命中阈值是多少。
 * 领域服务拿到它后，就能按照这份规则去圈选用户。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 批次任务对象
 * @create 2024-12-28 13:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTagsJobEntity {

    /** 标签统计类型，例如按参与次数统计或按消费金额统计。 */
    private Integer tagType;
    /** 标签命中规则阈值，例如参与 N 次或累计金额达到某个值。 */
    private String tagRule;
    /** 统计窗口开始时间，只统计该时间点之后的数据。 */
    private Date statStartTime;
    /** 统计窗口结束时间，只统计该时间点之前的数据。 */
    private Date statEndTime;

}
