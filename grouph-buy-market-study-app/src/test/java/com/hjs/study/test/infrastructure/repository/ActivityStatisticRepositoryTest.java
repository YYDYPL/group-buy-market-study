package com.hjs.study.test.infrastructure.repository;

import com.hjs.study.domain.activity.model.valobj.TeamStatisticVO;
import com.hjs.study.infrastructure.adapter.repository.ActivityRepository;
import com.hjs.study.infrastructure.dao.IGroupBuyOrderDao;
import com.hjs.study.infrastructure.dao.IGroupBuyOrderListDao;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 活动历史累计统计测试。
 */
public class ActivityStatisticRepositoryTest {

    @Test
    public void shouldKeepHistoricalTeamsAndParticipantsInStatistics() {
        IGroupBuyOrderDao orderDao = mock(IGroupBuyOrderDao.class);
        IGroupBuyOrderListDao orderListDao = mock(IGroupBuyOrderListDao.class);
        when(orderDao.queryAllTeamCountByActivityId(100123L)).thenReturn(21);
        when(orderDao.queryAllTeamCompleteCountByActivityId(100123L)).thenReturn(6);
        when(orderListDao.queryAllUserCountByActivityId(100123L)).thenReturn(59);

        ActivityRepository repository = new ActivityRepository();
        ReflectionTestUtils.setField(repository, "groupBuyOrderDao", orderDao);
        ReflectionTestUtils.setField(repository, "groupBuyOrderListDao", orderListDao);

        TeamStatisticVO result = repository.queryTeamStatisticByActivityId(100123L);

        Assert.assertEquals(Integer.valueOf(21), result.getAllTeamCount());
        Assert.assertEquals(Integer.valueOf(6), result.getAllTeamCompleteCount());
        Assert.assertEquals(Integer.valueOf(59), result.getAllTeamUserCount());
    }
}
