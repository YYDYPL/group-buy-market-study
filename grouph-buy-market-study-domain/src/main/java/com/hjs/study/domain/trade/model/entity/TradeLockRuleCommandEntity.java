package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 锁单规则校验命令实体。
 * <p>
 * 这个对象通常出现在交易域规则过滤入口，用来表达“用户想对哪个活动、哪支队伍发起锁单”。
 * 它是一个命令对象，不是结果对象，所以只保留驱动规则判断所需的最小输入。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团交易命令实体
 * @create 2025-01-25 09:09
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeLockRuleCommandEntity {

    /** 发起本次锁单动作的用户 ID。 */
    private String userId;
    /** 要参与的活动 ID。 */
    private Long activityId;
    /**
     * 目标组队 ID。
     * 为空通常表示开团，不为空表示参团。
     */
    private String teamId;

}
