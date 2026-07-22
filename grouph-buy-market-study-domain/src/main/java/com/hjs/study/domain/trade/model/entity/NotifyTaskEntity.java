package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调任务实体。
 * <p>
 * 交易域中的“通知”并不是直接同步推送完就算结束，而是会先沉淀为一个可重试的任务对象。
 * 这样做的目的是把业务结算和外部通知解耦，避免因为外部接口失败拖垮主交易流程。
 * <p>
 * 因此该实体本质上描述的是一条待执行或待重试的通知任务，
 * 例如成团通知、退款通知等。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 回调任务实体
 * @create 2025-01-31 10:41
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyTaskEntity {

    /** 关联的拼团队伍 ID，用于说明这条通知是围绕哪支队伍产生的。 */
    private String teamId;
    /** 回调方式类型，例如 HTTP 或 MQ。 */
    private String notifyType;
    /** MQ 模式下的目标消息主题或队列标识。 */
    private String notifyMQ;
    /** HTTP 模式下的回调地址。 */
    private String notifyUrl;
    /** 已执行的通知次数，用于重试次数控制与补偿监控。 */
    private Integer notifyCount;
    /** 序列化后的通知参数 JSON，真正执行通知时会反序列化使用。 */
    private String parameterJson;
    /** 任务唯一标识，用于分布式环境下去重、加锁和幂等控制。 */
    private String uuid;

    /**
     * 生成任务分布式锁 Key。
     * <p>
     * 通知任务往往会被定时扫描线程并发消费，这里通过唯一标识拼出锁 Key，
     * 避免同一条任务被多个执行器重复处理。
     */
    public String lockKey() {
        return "notify_job_lock_key_" + this.uuid;
    }

}
