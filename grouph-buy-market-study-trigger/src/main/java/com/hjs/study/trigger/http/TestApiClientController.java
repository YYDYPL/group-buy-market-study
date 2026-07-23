package com.hjs.study.trigger.http;

import com.hjs.study.api.dto.NotifyRequestDTO;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 拼团结果回调的模拟接收端。
 *
 * <p>该控制器用于本地联调：生产侧通过 HTTP 推送拼团通知时，可以将回调地址指向本接口，
 * 从日志中检查通知报文是否符合约定。它不执行业务处理，也不应作为正式回调服务使用。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-31 08:59
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/test/")
public class TestApiClientController {

    /**
     * 接收并记录一条模拟的拼团回调。
     *
     * <p>通知任务会把字符串 {@code "success"} 视为本次 HTTP 回调成功；当前方法固定返回成功，
     * 便于验证正常通知链路。</p>
     *
     * @param notifyRequestDTO 拼团通知报文，包含回调类型及对应的业务数据
     * @return 固定返回 {@code success}，模拟第三方系统已成功受理通知
     */
    @RequestMapping(value = "group_buy_notify", method = RequestMethod.POST)
    public String groupBuyNotify(@RequestBody NotifyRequestDTO notifyRequestDTO) {
        log.info("模拟测试第三方服务接收拼团回调 {}", JSON.toJSONString(notifyRequestDTO));

        return "success";
    }

}
