package com.hjs.study.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 活动拼团队伍统计值对象。
 * <p>
 * 用于活动首页、活动详情页顶部等场景展示活动热度数据，
 * 比如共有多少团、成团多少个、累计有多少人参与。
 * 它属于纯展示型值对象，不承担行为逻辑。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 队伍统计值对象
 * @create 2025-02-02 15:21
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamStatisticVO {

    /** 当前活动下的队伍总数。 */
    private Integer allTeamCount;
    /** 当前活动下已经成团的队伍数量。 */
    private Integer allTeamCompleteCount;
    /** 当前活动下累计参与拼团的人次总量。 */
    private Integer allTeamUserCount;

}
