package org.jeecg.starter.sentinel.parser;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Sentinel 请求来源解析器
 * <p>
 * 迁移自 jeecg-boot-starter-cloud 的 DefaultRequestOriginParser：
 * 1. 有参数 origin 时走参数拦截规则
 * 2. 参数为空时走 IP 拦截模式
 *
 * @author zyf
 */
public class JeecgHeaderRequestOriginParser implements RequestOriginParser {

	@Override
	public String parseOrigin(HttpServletRequest request) {
		// 基于请求参数，origin 对应授权规则中的流控应用名称
		String origin = request.getParameter("origin");
		if (StringUtils.isNotEmpty(origin)) {
			return origin;
		}
		// 当参数为空使用 IP 拦截模式
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.isNotEmpty(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

}
