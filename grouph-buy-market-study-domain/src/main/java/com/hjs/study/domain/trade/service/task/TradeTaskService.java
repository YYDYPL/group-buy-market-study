package com.hjs.study.domain.trade.service.task;

import com.hjs.study.domain.trade.adapter.port.ITradePort;
import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.service.ITradeTaskService;
import com.hjs.study.types.enums.NotifyTaskHTTPEnumVO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 交易通知任务服务实现。
 * <p>
 * 该服务专门负责消费通知任务表中的待执行记录，
 * 再通过端口调用外部 MQ/HTTP 通道完成真正通知。
 * 这样设计后，主交易流程只需生成任务，不必同步等待外部系统响应。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/12 21:15
 */
@Slf4j
@Service
public class TradeTaskService implements ITradeTaskService {

    /** 通知任务相关的仓储操作，例如查询待执行任务、更新成功/失败/重试状态。 */
    @Resource
    private ITradeRepository repository;
    /** 实际负责发送外部通知的端口，由基础设施层决定是 MQ 还是 HTTP。 */
    @Resource
    private ITradePort port;
    /** 线程池预留给需要异步扩展的场景，当前类主要执行同步任务扫描逻辑。 */
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    
    @Override
    public Map<String, Integer> execNotifyJob() throws Exception {
        log.info("拼团交易-执行回调通知任务");

        // 批量拉取待执行通知任务，适合定时调度器全量扫描触发。
        List<NotifyTaskEntity> notifyTaskEntityList = repository.queryUnExecutedNotifyTaskList();

        return execNotifyJob(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execNotifyJob(String teamId) throws Exception {
        log.info("拼团交易-执行回调通知回调，指定 teamId:{}", teamId);
        // 只处理某一个团队的通知任务，常用于手工补偿或问题排查。
        List<NotifyTaskEntity> notifyTaskEntityList = repository.queryUnExecutedNotifyTaskList(teamId);
        return execNotifyJob(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception {
        log.info("拼团交易-执行回调通知回调，指定 teamId:{} notifyTaskEntity:{}", notifyTaskEntity.getTeamId(), JSON.toJSONString(notifyTaskEntity));
        // 直接执行一条任务，便于结算/退款完成后立即补一枪通知。
        return execNotifyJob(Collections.singletonList(notifyTaskEntity));
    }

    private Map<String, Integer> execNotifyJob(List<NotifyTaskEntity> notifyTaskEntityList) throws Exception {
        int successCount = 0, errorCount = 0, retryCount = 0;
        for (NotifyTaskEntity notifyTask : notifyTaskEntityList) {
            // 统一走端口调用，由端口层屏蔽 HTTP/MQ 等底层差异。
            String response = port.groupBuyNotify(notifyTask);

            // 根据调用结果更新任务状态：
            // success 表示任务完成；
            // error 且重试次数超限则标记最终失败；
            // 否则进入待重试状态。
            if (NotifyTaskHTTPEnumVO.SUCCESS.getCode().equals(response)) {
                int updateCount = repository.updateNotifyTaskStatusSuccess(notifyTask);
                if (1 == updateCount) {
                    successCount += 1;
                }
            } else if (NotifyTaskHTTPEnumVO.ERROR.getCode().equals(response)) {
                if (notifyTask.getNotifyCount() > 4) {
                    int updateCount = repository.updateNotifyTaskStatusError(notifyTask);
                    if (1 == updateCount) {
                        errorCount += 1;
                    }
                } else {
                    int updateCount = repository.updateNotifyTaskStatusRetry(notifyTask);
                    if (1 == updateCount) {
                        retryCount += 1;
                    }
                }
            }
        }

        // 返回一份执行统计结果，便于日志、监控和手工补偿时快速观察执行效果。
        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("waitCount", notifyTaskEntityList.size());
        resultMap.put("successCount", successCount);
        resultMap.put("errorCount", errorCount);
        resultMap.put("retryCount", retryCount);

        return resultMap;
    }
    
}
