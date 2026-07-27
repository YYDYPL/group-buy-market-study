package com.hjs.study.test.infrastructure.repository;

import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.infrastructure.adapter.repository.ProductConfigRepository;
import com.hjs.study.infrastructure.dao.IProductConfigDao;
import com.hjs.study.infrastructure.dao.po.GroupBuyActivity;
import com.hjs.study.infrastructure.dao.po.GroupBuyDiscount;
import com.hjs.study.infrastructure.dao.po.ProductConfig;
import com.hjs.study.infrastructure.redis.IRedisService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商品配置仓储的草稿隔离、发布切换和乐观锁测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductConfigRepositoryTest {

    @Mock
    private IProductConfigDao productConfigDao;
    @Mock
    private IRedisService redisService;
    @InjectMocks
    private ProductConfigRepository repository;

    @Test
    public void shouldSaveProductSnapshotWithoutPublishingSkuFields() {
        ProductConfigEntity entity = entity("9890001", 5, 200L, "NEW00001");
        entity.setGoodsName("只在草稿中可见的名称");
        when(productConfigDao.querySkuByGoodsId("9890001"))
                .thenReturn(ProductConfig.builder().goodsId("9890001").version(5).build());
        when(productConfigDao.updateSkuDraft(any(ProductConfig.class))).thenReturn(1);
        when(productConfigDao.insertDiscount(any(ProductConfig.class))).thenReturn(1);
        when(productConfigDao.insertActivity(any(ProductConfig.class))).thenReturn(1);
        when(productConfigDao.queryProductConfigByGoodsId("9890001"))
                .thenReturn(ProductConfig.builder().goodsId("9890001").version(6).build());

        repository.saveDraft(entity);

        ArgumentCaptor<ProductConfig> activityCaptor = ArgumentCaptor.forClass(ProductConfig.class);
        verify(productConfigDao).insertActivity(activityCaptor.capture());
        Assert.assertTrue(activityCaptor.getValue().getDraftData().contains("只在草稿中可见的名称"));
        Assert.assertEquals(Integer.valueOf(6), activityCaptor.getValue().getVersion());
        verify(productConfigDao).abandonOldDrafts("9890001");
        verify(productConfigDao, never()).updateSkuPublish(any(ProductConfig.class));
    }

    @Test
    public void shouldSwitchRouteAndEvictOldAndNewCachesOnPublish() {
        ProductConfig draft = po("9890001", 6, 200L, "NEW00001", "s02", "c02");
        ProductConfig active = po("9890001", 6, 100L, "OLD00001", "s01", "c01");
        when(productConfigDao.queryDraftByGoodsId("9890001")).thenReturn(draft);
        when(productConfigDao.queryActiveByGoodsId("9890001")).thenReturn(active);
        when(productConfigDao.updateSkuPublish(draft)).thenReturn(1);
        when(productConfigDao.updateActivityStatus(any(ProductConfig.class))).thenReturn(1);
        when(productConfigDao.updateRouteStatus(active)).thenReturn(1);
        when(productConfigDao.upsertRoute(draft)).thenReturn(1);
        when(productConfigDao.queryProductConfigByGoodsId("9890001")).thenReturn(draft);

        repository.publish("9890001", 6);

        Assert.assertEquals(Integer.valueOf(2), active.getActivityStatus());
        Assert.assertEquals(Integer.valueOf(0), active.getStatus());
        Assert.assertEquals(Integer.valueOf(1), draft.getActivityStatus());
        verify(productConfigDao).updateRouteStatus(active);
        verify(productConfigDao).upsertRoute(draft);
        verify(redisService).remove(GroupBuyActivity.cacheRedisKey(100L));
        verify(redisService).remove(GroupBuyDiscount.cacheRedisKey("OLD00001"));
        verify(redisService).remove(GroupBuyActivity.cacheRedisKey(200L));
        verify(redisService).remove(GroupBuyDiscount.cacheRedisKey("NEW00001"));
    }

    @Test
    public void shouldStopWritingRelatedTablesWhenOptimisticLockFails() {
        ProductConfigEntity entity = entity("9890001", 3, 200L, "NEW00001");
        when(productConfigDao.querySkuByGoodsId("9890001"))
                .thenReturn(ProductConfig.builder().goodsId("9890001").version(4).build());
        when(productConfigDao.updateSkuDraft(any(ProductConfig.class))).thenReturn(0);

        try {
            repository.saveDraft(entity);
            Assert.fail("预期抛出版本冲突异常");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("其他页面"));
        }

        verify(productConfigDao, never()).insertDiscount(any(ProductConfig.class));
        verify(productConfigDao, never()).insertActivity(any(ProductConfig.class));
    }

    private ProductConfigEntity entity(String goodsId, int version, long activityId, String discountId) {
        return ProductConfigEntity.builder()
                .goodsId(goodsId).goodsName("测试商品").originalPrice(new BigDecimal("100.00"))
                .category("测试").subtitle("测试").mainImage("images/test.png")
                .galleryImages("[]").serviceTags("[]").salesCount(1)
                .favorableRate(new BigDecimal("99.00")).sortOrder(1).productStatus(1)
                .version(version).activityId(activityId).activityName("测试活动")
                .discountId(discountId).discountName("测试优惠").discountDesc("测试")
                .discountType(0).marketPlan("ZJ").marketExpr("10")
                .groupType(0).takeLimitCount(3).target(2).validTime(30)
                .source("s01").channel("c01").build();
    }

    private ProductConfig po(
            String goodsId, int version, long activityId, String discountId, String source, String channel) {
        ProductConfig po = new ProductConfig();
        po.setGoodsId(goodsId);
        po.setVersion(version);
        po.setActivityId(activityId);
        po.setDiscountId(discountId);
        po.setSource(source);
        po.setChannel(channel);
        return po;
    }
}
