package com.hjs.study.api.dto;

import lombok.*;

/**
 * 拼团营销支付锁单请求。
 *
 * <p>用于支付前占用拼团队伍名额并生成营销订单。请求只提交业务标识和通知方式，成交价格由
 * 服务端重新进行营销试算，调用方不应自行传入或决定优惠金额。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-11 13:55
 */
@Data
public class LockMarketPayOrderRequestDTO {

    /** 发起锁单的用户 ID。 */
    private String userId;
    /**
     * 拼团队伍 ID。
     *
     * <p>为空表示用户发起新团，非空表示加入已有队伍；服务端仍会校验队伍状态与剩余名额。</p>
     */
    private String teamId;
    /** 用户选择参与的拼团活动 ID。 */
    private Long activityId;
    /** 本次锁单购买的商品 ID。 */
    private String goodsId;
    /** 业务来源标识，用于匹配渠道商品和活动规则。 */
    private String source;
    /** 业务渠道标识，与来源共同组成营销上下文。 */
    private String channel;
    /**
     * 调用方生成的外部交易单号。
     *
     * <p>服务端使用用户 ID 与该单号查询既有订单，重复请求应返回同一笔锁单结果。</p>
     */
    private String outTradeNo;
    /** 拼团完成后的通知方式及目标配置。 */
    private NotifyConfigVO notifyConfigVO;

    /**
     * 便捷设置 HTTP 回调配置。
     *
     * <p>该方法用于兼容直接传入回调地址的调用代码，会创建新的嵌套配置并把通知类型固定为
     * {@code HTTP}。重复调用会覆盖原有通知配置。</p>
     *
     * @param url 拼团完成后接收通知的 HTTP 地址
     */
    public void setNotifyUrl(String url) {
        NotifyConfigVO notifyConfigVO = new NotifyConfigVO();
        notifyConfigVO.setNotifyType("HTTP");
        notifyConfigVO.setNotifyUrl(url);
        this.notifyConfigVO = notifyConfigVO;
    }

    /**
     * 便捷设置 MQ 通知配置。
     *
     * <p>这里只选择 {@code MQ} 类型，不要求调用方指定路由键；系统在后续结算通知流程中使用
     * 统一配置的成团消息主题。重复调用会覆盖原有通知配置。</p>
     */
    public void setNotifyMQ() {
        NotifyConfigVO notifyConfigVO = new NotifyConfigVO();
        notifyConfigVO.setNotifyType("MQ");
        this.notifyConfigVO = notifyConfigVO;
    }

    /**
     * 拼团完成后的异步通知配置。
     *
     * <p>HTTP 与 MQ 是互斥通知方式：HTTP 使用 {@link #notifyUrl}，MQ 使用
     * {@link #notifyMQ} 或系统默认消息主题。</p>
     */
    @Data
    public static class NotifyConfigVO {
        /**
         * 通知类型，目前支持 {@code MQ} 和 {@code HTTP}，取值需与领域枚举名称一致。
         */
        private String notifyType;
        /**
         * MQ 通知使用的 routing key/消息主题标识；使用系统统一主题时可以为空。
         */
        private String notifyMQ;
        /**
         * HTTP 通知目标地址；当通知类型为 HTTP 时必须提供有效地址。
         */
        private String notifyUrl;
    }

}
