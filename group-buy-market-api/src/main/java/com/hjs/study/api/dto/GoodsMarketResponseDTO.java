package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品拼团营销配置聚合响应。
 *
 * <p>一次响应同时返回命中的活动、服务端优惠试算结果、首页可展示队伍和活动统计，避免商品
 * 页面为不同区域分别发起多次查询。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-02-02 12:20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsMarketResponseDTO {

    /** 本次营销试算实际命中的拼团活动 ID。 */
    private Long activityId;
    /** 商品及服务端计算出的价格信息。 */
    private Goods goods;
    /**
     * 首页展示的可参与队伍列表。
     *
     * <p>业务查询通常优先展示与当前用户相关的队伍，再补充随机进行中队伍；没有可展示队伍时
     * 应返回空集合而不是 {@code null}。</p>
     */
    private List<Team> teamList;
    /** 当前活动维度的开团、成团和参与人数统计。 */
    private TeamStatistic teamStatistic;

    /**
     * 商品价格视图。
     *
     * <p>所有金额均来自服务端营销试算，其中支付价格通常满足：原价减去优惠金额等于支付价，
     * 具体计算规则由命中的折扣策略决定。</p>
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Goods {
        /** 商品 ID。 */
        private String goodsId;
        /** 未应用拼团优惠前的商品原价。 */
        private BigDecimal originalPrice;
        /** 本次活动从原价中抵扣的优惠金额。 */
        private BigDecimal deductionPrice;
        /** 应由用户实际支付的营销成交价格。 */
        private BigDecimal payPrice;
    }

    /**
     * 首页队伍展示视图。
     *
     * <p>同时提供目标人数、支付完成人数和锁单人数，使调用方可以展示队伍当前进度。锁单人数
     * 包含已占位但可能尚未支付的用户，因此不一定等于完成数量。</p>
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Team {
        /** 队伍展示关联的用户 ID，通常用于标识当前用户相关队伍。 */
        private String userId;
        /** 拼团队伍唯一 ID，参团锁单时作为请求参数传回。 */
        private String teamId;
        /** 队伍所属的拼团活动 ID。 */
        private Long activityId;
        /** 队伍达成成团所需的目标人数。 */
        private Integer targetCount;
        /** 已完成支付结算的人数。 */
        private Integer completeCount;
        /** 已占用队伍名额的人数，包含待支付和已支付订单。 */
        private Integer lockCount;
        /** 当前队伍的有效期开始时间。 */
        private Date validStartTime;
        /** 当前队伍的有效期结束时间。 */
        private Date validEndTime;
        /**
         * 面向页面展示的剩余时间字符串。
         *
         * <p>Controller 通常以接口处理时的当前时间作为起点，与 {@link #validEndTime} 计算差值。</p>
         */
        private String validTimeCountdown;
        /** 当前用户关联的外部交易单号，用于跨系统关联并支撑重复请求幂等。 */
        private String outTradeNo;

        /**
         * 计算两个时间点之间的倒计时展示文本。
         *
         * <p>返回格式固定为 {@code HH:mm:ss}：参数为空时返回“无效的时间”，结束时间早于起点时
         * 返回“已结束”。当前格式不包含天数，超过 24 小时的差值只展示扣除整天后的小时部分。</p>
         *
         * @param validStartTime 倒计时计算起点；首页组装时通常传入当前时间
         * @param validEndTime   队伍有效期结束时间
         * @return 倒计时文本、已结束提示或无效时间提示
         */
        public static String differenceDateTime2Str(Date validStartTime, Date validEndTime) {
            // 任一时间缺失都无法计算有效差值，直接返回可读提示。
            if (validStartTime == null || validEndTime == null) {
                return "无效的时间";
            }

            // 使用时间戳相减得到毫秒差，避免受到 Date 对象可变性的额外影响。
            long diffInMilliseconds = validEndTime.getTime() - validStartTime.getTime();

            // 负数表示队伍有效期已经结束。
            if (diffInMilliseconds < 0) {
                return "已结束";
            }

            // 各单位取模后用于 HH:mm:ss；days 当前未拼接到最终返回文本中。
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMilliseconds) % 60;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMilliseconds) % 60;
            long hours = TimeUnit.MILLISECONDS.toHours(diffInMilliseconds) % 24;
            long days = TimeUnit.MILLISECONDS.toDays(diffInMilliseconds);

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

    }

    /**
     * 活动维度的拼团统计视图。
     *
     * <p>这些字段用于展示活动热度，统计范围是当前活动而不是单个队伍。</p>
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TeamStatistic {
        /** 活动累计创建的队伍数量。 */
        private Integer allTeamCount;
        /** 活动累计达到目标并完成成团的队伍数量。 */
        private Integer allTeamCompleteCount;
        /** 当前活动对应商品的累计参团人数。 */
        private Integer allTeamUserCount;
    }

}
