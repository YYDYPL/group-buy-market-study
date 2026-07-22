package com.hjs.study.api.dto;

import lombok.Data;

/**
 * 商品拼团营销配置查询请求。
 *
 * <p>用户、来源、渠道和商品共同组成活动匹配上下文。服务端会根据这些字段查询渠道商品映射、
 * 生效活动及人群标签，再执行优惠试算。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-02-02 12:19
 */
@Data
public class GoodsMarketRequestDTO {

    /** 当前访问用户 ID，用于人群标签、可参与队伍和用户维度限流判断。 */
    private String userId;
    /** 业务来源标识，例如调用系统或流量来源。 */
    private String source;
    /** 业务渠道标识，与来源共同定位渠道商品及活动配置。 */
    private String channel;
    /** 待查询营销信息的商品 ID。 */
    private String goodsId;

}
