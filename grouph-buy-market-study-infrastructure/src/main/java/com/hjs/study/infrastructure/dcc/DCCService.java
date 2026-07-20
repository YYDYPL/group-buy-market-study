package com.hjs.study.infrastructure.dcc;

import com.hjs.study.types.annotation.DCCValue;
import com.hjs.study.types.common.Constants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 动态配置中心服务。
 * <p>
 * 该类负责承接 DCC（Dynamic Config Center，动态配置中心）下发的运行时配置，
 * 并把字符串形式的配置项转换为业务代码更容易使用的布尔判断或范围判断。
 * 当前主要承载四类能力：
 * 1. 系统整体降级开关；
 * 2. 灰度切量范围控制；
 * 3. 渠道黑名单拦截；
 * 4. 仓储缓存开关。
 * <p>
 * 使用 {@link DCCValue} 注解后，字段值会在应用运行期间被动态刷新，
 * 因此无需重启应用就能调整开关策略。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 动态配置服务
 * @create 2025-01-03 15:38
 */
@Service
public class DCCService {

    /**
     * 系统降级开关。
     * <p>
     * 配置值含义：
     * 0 表示关闭降级，系统按正常完整链路执行；
     * 1 表示开启降级，上层业务可根据该值跳过部分非核心流程或直接快速失败。
     */
    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;

    /**
     * 灰度切量范围。
     * <p>
     * 默认值为 100，表示 100% 用户都命中；
     * 如果配置为 30，则只有哈希后尾号落在 0~30 范围内的用户命中。
     * 该能力通常用于新功能灰度、限流试放量等场景。
     */
    @DCCValue("cutRange:100")
    private String cutRange;

    /**
     * 渠道黑名单列表。
     * <p>
     * 采用 {@code source + channel} 拼接后的字符串形式存储，
     * 多个黑名单项之间使用 {@link Constants#SPLIT} 分隔。
     * 命中后表示该来源渠道被拦截，不允许继续参与相关业务流程。
     */
    @DCCValue("scBlacklist:s02c02")
    private String scBlacklist;

    /**
     * 仓储缓存开关。
     * <p>
     * 当前项目约定：
     * 0 表示开启缓存；
     * 1 表示关闭缓存，直接走数据库。
     * 这个命名虽然叫 openSwitch，但本质上更像“缓存是否允许使用”的配置。
     */
    @DCCValue("cacheSwitch:0")
    private String cacheOpenSwitch;

    /**
     * 判断系统是否开启整体降级。
     *
     * @return {@code true} 表示已开启降级；{@code false} 表示关闭降级
     */
    public boolean isDowngradeSwitch() {
        return "1".equals(downgradeSwitch);
    }

    /**
     * 判断用户是否命中当前灰度切量范围。
     * <p>
     * 实现思路：
     * 1. 对用户 ID 做哈希，保证同一用户每次结果一致；
     * 2. 取哈希值后两位，得到 0~99 的区间值；
     * 3. 与 cutRange 做比较，落入范围则命中切量。
     * <p>
     * 这种做法的优点是：
     * 计算成本低；
     * 分布相对均匀；
     * 不需要额外存储灰度用户名单。
     *
     * @param userId 用户 ID
     * @return {@code true} 表示命中灰度范围；{@code false} 表示未命中
     */
    public boolean isCutRange(String userId) {
        // 计算哈希码的绝对值
        int hashCode = Math.abs(userId.hashCode());

        // 获取最后两位
        int lastTwoDigits = hashCode % 100;

        // 判断是否在切量范围内
        if (lastTwoDigits <= Integer.parseInt(cutRange)) {
            return true;
        }

        return false;
    }

    /**
     * 判断指定来源渠道是否命中黑名单。
     * <p>
     * 这里会把 {@code source} 和 {@code channel} 直接拼接，
     * 与配置中心下发的黑名单列表逐项比较。
     *
     * @param source 渠道标识
     * @param channel 来源标识
     * @return {@code true} 表示拦截；{@code false} 表示放行
     */
    public boolean isSCBlackIntercept(String source, String channel) {
        List<String> list = Arrays.asList(scBlacklist.split(Constants.SPLIT));
        return list.contains(source + channel);
    }

    /**
     * 判断缓存功能是否开启。
     * <p>
     * 当前约定中：
     * 配置值为 0 时返回 {@code true}，表示启用缓存；
     * 配置值为 1 时返回 {@code false}，表示关闭缓存。
     *
     * @return {@code true} 表示开启缓存；{@code false} 表示关闭缓存
     */
    public boolean isCacheOpenSwitch() {
        return "0".equals(cacheOpenSwitch);
    }

}
