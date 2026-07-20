package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 人群标签定义表对应的 PO 对象。
 * <p>
 * 这个类承载的是“标签元数据”，描述一个标签本身是什么、叫什么、覆盖多少用户，
 * 并不直接保存用户明细。真正的用户归属关系保存在 {@code crowd_tags_detail} 表中。
 * 业务侧会通过 {@code GroupBuyActivity.tagId} 关联到这里，实现活动的人群定向投放。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签
 * @create 2024-12-28 11:42
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTags {

    /** 数据库自增主键，仅用于表内唯一标识这一行记录。 */
    private Long id;
    /** 标签业务唯一标识，对外关联时通常使用这个字段而不是主键 id。 */
    private String tagId;
    /** 标签名称，用于后台配置展示和业务语义识别。 */
    private String tagName;
    /** 标签说明，描述该标签圈定的是哪一类用户。 */
    private String tagDesc;
    /**
     * 标签覆盖用户数。
     * <p>
     * 这是一个冗余统计字段，便于后台直接展示当前标签下有多少用户，
     * 实际数据通常由批量任务计算后回写。
     */
    private Integer statistics;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录最后一次更新时间。 */
    private Date updateTime;

}
