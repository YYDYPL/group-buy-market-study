package com.hjs.study.infrastructure.adapter.repository;

import com.hjs.study.domain.tag.adapter.repository.ITagRepository;
import com.hjs.study.domain.tag.model.entity.CrowdTagsJobEntity;
import com.hjs.study.infrastructure.dao.ICrowdTagsDao;
import com.hjs.study.infrastructure.dao.ICrowdTagsDetailDao;
import com.hjs.study.infrastructure.dao.ICrowdTagsJobDao;
import com.hjs.study.infrastructure.dao.po.CrowdTags;
import com.hjs.study.infrastructure.dao.po.CrowdTagsDetail;
import com.hjs.study.infrastructure.dao.po.CrowdTagsJob;
import com.hjs.study.infrastructure.redis.IRedisService;
import org.redisson.api.RBitSet;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * 标签域仓储实现。
 * <p>
 * 该类负责把标签领域需要的能力，落到标签任务表、标签明细表、
 * 标签统计表以及 Redis BitSet 结构上，实现“查任务规则、写用户归属、更新统计数量”的完整闭环。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签仓储
 * @create 2024-12-28 13:12
 */
@Repository
public class TagRepository implements ITagRepository {

    /** 标签主表 DAO，用于更新标签统计量。 */
    @Resource
    private ICrowdTagsDao crowdTagsDao;
    /** 标签明细 DAO，用于写入用户标签归属。 */
    @Resource
    private ICrowdTagsDetailDao crowdTagsDetailDao;
    /** 标签任务 DAO，用于查询批量圈选规则。 */
    @Resource
    private ICrowdTagsJobDao crowdTagsJobDao;

    /** Redis 服务，主要用于维护标签对应的 BitSet 索引。 */
    @Resource
    private IRedisService redisService;

    /**
     * 查询标签批处理任务的领域实体。
     *
     * @param tagId 标签 ID
     * @param batchId 批次 ID
     * @return 标签任务实体；未命中时返回 {@code null}
     */
    @Override
    public CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId) {
        CrowdTagsJob crowdTagsJobReq = new CrowdTagsJob();
        crowdTagsJobReq.setTagId(tagId);
        crowdTagsJobReq.setBatchId(batchId);

        CrowdTagsJob crowdTagsJobRes = crowdTagsJobDao.queryCrowdTagsJob(crowdTagsJobReq);
        if (null == crowdTagsJobRes) return null;

        return CrowdTagsJobEntity.builder()
                .tagType(crowdTagsJobRes.getTagType())
                .tagRule(crowdTagsJobRes.getTagRule())
                .statStartTime(crowdTagsJobRes.getStatStartTime())
                .statEndTime(crowdTagsJobRes.getStatEndTime())
                .build();
    }

    /**
     * 新增一条用户标签归属记录，并同步写入 Redis BitSet。
     * <p>
     * 数据库层面存在 {@code tagId + userId} 唯一索引，
     * 因此这里会忽略重复插入异常，以实现幂等写入。
     *
     * @param tagId 标签 ID
     * @param userId 用户 ID
     */
    @Override
    public void addCrowdTagsUserId(String tagId, String userId) {
        CrowdTagsDetail crowdTagsDetailReq = new CrowdTagsDetail();
        crowdTagsDetailReq.setTagId(tagId);
        crowdTagsDetailReq.setUserId(userId);

        try {
            crowdTagsDetailDao.addCrowdTagsUserId(crowdTagsDetailReq);
        } catch (DuplicateKeyException ignore) {
            // 忽略唯一索引冲突
        }

        // 数据库存事实明细，BitSet 则承担高性能命中判断，两者一起构成“可追溯 + 可快速判定”的双写模型。
        // 即使数据库写入发生了唯一索引冲突，BitSet 仍然会被置位，确保缓存态和真实结果保持一致。
        RBitSet bitSet = redisService.getBitSet(tagId);
        bitSet.set(redisService.getIndexFromUserId(userId), true);
    }

    /**
     * 更新标签覆盖人数统计。
     *
     * @param tagId 标签 ID
     * @param count 本次要累加的用户数量
     */
    @Override
    public void updateCrowdTagsStatistics(String tagId, int count) {
        CrowdTags crowdTagsReq = new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(count);

        crowdTagsDao.updateCrowdTagsStatistics(crowdTagsReq);
    }

}
