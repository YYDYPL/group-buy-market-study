package com.hjs.study.domain.tag;


import com.hjs.study.domain.tag.adapter.repository.ITagRepository;
import com.hjs.study.domain.tag.model.entity.CrowdTagsJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 人群标签服务实现。
 * <p>
 * 该服务负责把“标签任务定义”真正转成一次可执行的圈人流程：
 * 1. 读取批次任务规则；
 * 2. 采集或计算命中的用户集合；
 * 3. 把用户写入标签明细；
 * 4. 回写标签统计量。
 * 当前示例中的用户数据是模拟数据，真实生产环境通常会接数据仓库或离线脚本结果。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签服务
 * @create 2024-12-28 12:51
 */
@Slf4j
@Service
public class TagService implements ITagService {

    /** 标签域仓储，用于查询任务并写入标签结果。 */
    @Resource
    private ITagRepository repository;

    @Override
    /**
     * 执行标签批次任务。
     * <p>
     * 该方法目前演示的是最小闭环：
     * 查询任务 -> 准备命中用户 -> 落明细 -> 更新统计。
     * 真正在线上场景里，步骤 2 通常由离线数仓、Spark/Flink 或 SQL 脚本预先完成。
     *
     * @param tagId 标签 ID
     * @param batchId 批次 ID
     */
    public void execTagBatchJob(String tagId, String batchId) {
        log.info("人群标签批次任务 tagId:{} batchId:{}", tagId, batchId);

        // 1. 查询批次任务，拿到本次圈人的规则和统计时间窗。
        CrowdTagsJobEntity crowdTagsJobEntity = repository.queryCrowdTagsJobEntity(tagId, batchId);

        // 2. 采集用户数据。
        // 当前示例尚未接入真实数仓计算，因此只演示流程骨架。
        // 实际上这里会根据 crowdTagsJobEntity 中的 tagType、tagRule、statStartTime、statEndTime
        // 去统计符合条件的用户，例如“近 30 天消费满 100 元”或“近 7 天参与 3 次拼团”。

        // 3. 模拟一批命中用户。
        // 这些用户最终会被写入标签明细表，并同步到 Redis BitSet 中。
        List<String> userIdList = new ArrayList<String>() {{
            add("xiaofuge");
            add("liergou");
            add("xfg01");
            add("xfg02");
            add("xfg03");
            add("xfg04");
            add("xfg05");
            add("xfg06");
            add("xfg07");
            add("xfg08");
            add("xfg09");
        }};

        // 4. 逐个写入标签命中明细。
        // 演示代码里是循环插入；在真实环境里更常见的是离线脚本批量写库或流式同步。
        for (String userId : userIdList) {
            repository.addCrowdTagsUserId(tagId, userId);
        }

        // 5. 更新标签统计量，让标签主表知道当前覆盖了多少用户。
        repository.updateCrowdTagsStatistics(tagId, userIdList.size());
    }

}
