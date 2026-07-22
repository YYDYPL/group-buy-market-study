package com.hjs.study.domain.trade.model.aggregate;

import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.TradePaySuccessEntity;
import com.hjs.study.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团队伍结算聚合。
 * <p>
 * 当外部支付平台回传“支付成功”后，领域层并不会只修改某一张订单表，
 * 而是会把“支付成功的订单”“所属拼团队伍”“付款用户”一起组装成结算聚合，
 * 再交给仓储完成整笔业务落账。
 * <p>
 * 这样设计的价值在于：支付成功后需要同步推进多类业务对象，
 * 包括子单状态、主团进度、成团判定、回调任务生成等，
 * 使用聚合可以确保这些动作围绕同一个业务上下文展开。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团组队结算聚合
 * @create 2025-01-26 16:38
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamSettlementAggregate {

    /**
     * 支付成功所属的用户实体。
     * 结算时需要明确是哪位用户完成了支付，以便更新其订单状态并拼接通知参数。
     */
    private UserEntity userEntity;
    /**
     * 当前拼团队伍实体。
     * 保存团队现有进度、目标人数、有效期、回调配置等信息，
     * 是判断是否成团以及结算后是否要发通知的关键载体。
     */
    private GroupBuyTeamEntity groupBuyTeamEntity;
    /**
     * 外部支付成功事件转换得到的交易实体。
     * 它描述的是“哪一笔外部单已经支付成功”，
     * 便于领域层将支付成功信号与内部预购订单做关联。
     */
    private TradePaySuccessEntity tradePaySuccessEntity;

}
