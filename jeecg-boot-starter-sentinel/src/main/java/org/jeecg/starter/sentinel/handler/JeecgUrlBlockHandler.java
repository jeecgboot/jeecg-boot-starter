package org.jeecg.starter.sentinel.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 降级限流处理器，返回统一 JSON 响应
 * <p>
 * 迁移自 jeecg-boot-starter-cloud 的 CustomSentinelExceptionHandler，
 * 改进：使用 ObjectMapper 序列化，HTTP 状态码改为 429（Too Many Requests）
 *
 * @author zyf
 * @date 2022/02/18
 */
@Slf4j
public class JeecgUrlBlockHandler implements BlockExceptionHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, String resourceName,
			BlockException ex) throws Exception {
		log.error("Sentinel 限流降级，资源名称: {}", resourceName, ex);

		String msg;
		if (ex instanceof FlowException) {
			msg = "访问频繁，请稍候再试";
		} else if (ex instanceof DegradeException) {
			msg = "系统降级";
		} else if (ex instanceof ParamFlowException) {
			msg = "热点参数限流";
		} else if (ex instanceof SystemBlockException) {
			msg = "系统规则限流或降级";
		} else if (ex instanceof AuthorityException) {
			msg = "授权规则不通过";
		} else {
			msg = "未知限流降级";
		}

		response.setStatus(429);
		response.setCharacterEncoding("utf-8");
		response.setContentType("application/json;charset=utf-8");
		response.getWriter().write(objectMapper.writeValueAsString(
				java.util.Map.of("code", 500, "message", msg)));
	}

}
