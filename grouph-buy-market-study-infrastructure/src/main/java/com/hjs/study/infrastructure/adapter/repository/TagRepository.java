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

@Repository
public class TagRepository implements ITagRepository {

    @Resource
    private ICrowdTagsDao crowdTagsDao;
    @Resource
    private ICrowdTagsDetailDao crowdTagsDetailDao;
    @Resource
    private ICrowdTagsJobDao crowdTagsJobDao;

    @Resource
    private IRedisService redisService;

    /**
     * 读取标签任务定义，并将持久化对象转换为领域实体。
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
     * 同时将用户写入 MySQL 标签明细和 Redis BitSet 缓存。
     * 忽略唯一索引冲突，使重复执行批任务时保持幂等。
     */
    @Override
    public void addCrowdTagsUserId(String tagId, String userId) {
        CrowdTagsDetail crowdTagsDetailReq = new CrowdTagsDetail();
        crowdTagsDetailReq.setTagId(tagId);
        crowdTagsDetailReq.setUserId(userId);

        try {
            crowdTagsDetailDao.addCrowdTagsUserId(crowdTagsDetailReq);

            // 获取BitSet
            RBitSet bitSet = redisService.getBitSet(tagId);
            bitSet.set(redisService.getIndexFromUserId(userId), true);
        } catch (DuplicateKeyException ignore) {
            // 忽略唯一索引冲突
        }
    }

    /**
     * 标签明细写入完成后，更新人群标签聚合数量。
     */
    @Override
    public void updateCrowdTagsStatistics(String tagId, int count) {
        CrowdTags crowdTagsReq = new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(count);

        crowdTagsDao.updateCrowdTagsStatistics(crowdTagsReq);
    }

}
