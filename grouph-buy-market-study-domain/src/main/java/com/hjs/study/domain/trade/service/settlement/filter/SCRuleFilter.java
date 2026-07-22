package com.hjs.study.domain.trade.service.settlement.filter;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 渠道黑名单校验节点。
 * <p>
 * 有些来源/渠道可能因为商务下线、风控限制或系统开关原因被临时禁用，
 * 即便收到了支付成功通知，也不允许进入记账和结算流程。
 * 这个节点就是结算责任链中的第一道渠道入口闸门。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description SC 渠道来源过滤 - 当某个签约渠道下架后，则不会记账
 * @create 2025-01-29 09:16
 */
@Slf4j
@Service
public class SCRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    /** 交易仓储，负责查询渠道是否被黑名单拦截。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-渠道黑名单校验{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 先做来源渠道黑名单判断，避免无效结算继续往后执行。
        boolean intercept = repository.isSCBlackIntercept(requestParameter.getSource(), requestParameter.getChannel());
        if (intercept) {
            log.error("{}{} 渠道黑名单拦截", requestParameter.getSource(), requestParameter.getChannel());
            throw new AppException(ResponseCode.E0105);
        }
        // 通过后再进入外部单号合法性检查。
        return next(requestParameter, dynamicContext);
    }

}
