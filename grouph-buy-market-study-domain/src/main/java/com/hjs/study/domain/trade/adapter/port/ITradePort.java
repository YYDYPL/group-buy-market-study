package com.hjs.study.domain.trade.adapter.port;

import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;

/**
 * 交易域外部端口接口。
 * <p>
 * Port 的职责是站在 domain 视角定义“交易域需要向外部系统发起什么动作”，
 * 而不是关心这些动作最终通过 HTTP、MQ 还是 RPC 落地。
 * 当前交易域只抽象出一个核心动作：拼团状态通知。
 * 基础设施层会在 `infrastructure/adapter/port` 中给出真正实现。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易接口服务接口
 * @create 2025-01-31 10:38
 */
public interface ITradePort {

    /**
     * 执行拼团结果通知。
     * <p>
     * 当队伍成团、退款或其他需要告知外部系统的状态发生时，
     * domain 会把已经准备好的通知任务实体交给 Port，由 Port 负责真正发送。
     * 返回值通常用于告诉上层：
     * 1. 通知成功；
     * 2. 本次无需通知；
     * 3. 通知失败，需要后续重试或补偿。
     *
     * @param notifyTask 通知任务实体，包含通知类型、目标地址、消息体和唯一标识
     * @return 通知执行结果标识
     * @throws Exception 外部调用过程中的异常
     */
    String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception;

}
