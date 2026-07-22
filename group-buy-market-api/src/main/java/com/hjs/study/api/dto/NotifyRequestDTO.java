package com.hjs.study.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 拼团完成通知请求。
 *
 * <p>通知任务在队伍达到成团目标后组装该结构，可通过 HTTP 回调或 MQ 消息发送给下游。下游
 * 应按队伍 ID 或业务唯一键实现幂等，避免通知重试造成重复处理。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-31 10:08
 */
@Data
public class NotifyRequestDTO {

    /** 已完成拼团的队伍 ID。 */
    private String teamId;
    /** 该队伍内所有完成支付订单的外部交易单号列表。 */
    private List<String> outTradeNoList;

}
