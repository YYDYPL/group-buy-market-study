package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 回调通知任务表对应的 PO 对象。
 * <p>
 * 当拼团成功、退单或其他需要同步外部系统的场景发生时，
 * 系统不会直接把“通知是否成功”耦合在主交易流程里，而是先落一条通知任务。
 * 后续可以由定时任务或消息重试机制继续补偿处理。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 通知回调任务
 * @create 2025-01-26 18:19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyTask {

    /** 数据库自增主键。 */
    private Long id;
    /** 回调任务关联的活动 ID。 */
    private Long activityId;
    /** 回调任务关联的拼团队伍 ID。 */
    private String teamId;
    /** 回调业务种类，用于区分成团通知、已支付退单、未支付退单等场景。 */
    private String notifyCategory;
    /** 回调投递方式，例如 HTTP 或 MQ。 */
    private String notifyType;
    /** MQ 回调时使用的 Topic 名称；若走 HTTP，这个字段通常为空。 */
    private String notifyMQ;
    /** HTTP 回调地址；若走 MQ，这个字段通常为空。 */
    private String notifyUrl;
    /** 已发起的回调次数，每次重试通常都会累加。 */
    private Integer notifyCount;
    /** 回调状态：0 初始、1 完成、2 重试中、3 最终失败。 */
    private Integer notifyStatus;
    /** 回调参数的 JSON 字符串，常见内容包括 teamId、outTradeNoList 等。 */
    private String parameterJson;
    /** 任务幂等唯一标识，防止相同业务重复插入多条通知任务。 */
    private String uuid;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录更新时间。 */
    private Date updateTime;

}