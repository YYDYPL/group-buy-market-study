package com.hjs.study.test.infrastructure.dao;

import com.hjs.study.infrastructure.dao.INotifyTaskDao;
import com.hjs.study.infrastructure.dao.po.NotifyTask;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotifyTaskDaoTest {

    @Resource
    private INotifyTaskDao notifyTaskDao;

    @Test
    public void test_queryUnExecutedNotifyTaskList() {
        List<NotifyTask> notifyTasks = notifyTaskDao.queryUnExecutedNotifyTaskList();
        Assert.assertNotNull(notifyTasks);
    }

}
