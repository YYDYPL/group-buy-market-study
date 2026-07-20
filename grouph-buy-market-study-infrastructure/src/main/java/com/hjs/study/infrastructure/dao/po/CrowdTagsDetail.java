package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 人群标签明细表对应的 PO 对象。
 * <p>
 * 这张表保存“某个用户是否属于某个人群标签”的最终结果，
 * 是标签过滤节点判断用户是否可见、是否可参与活动的直接依据。
 * 同一个 {@code tagId + userId} 组合在库里只允许存在一条记录。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签明细
 * @create 2024-12-28 11:43
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTagsDetail {

    /** 数据库自增主键。 */
    private Long id;
    /** 标签业务 ID，指向 {@code crowd_tags.tag_id}。 */
    private String tagId;
    /** 命中该标签的用户唯一标识。 */
    private String userId;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录更新时间。 */
    private Date updateTime;

}
