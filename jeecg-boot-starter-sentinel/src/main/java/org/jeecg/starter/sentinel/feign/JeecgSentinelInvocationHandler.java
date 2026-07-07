package org.jeecg.starter.sentinel.feign;

import com.alibaba.cloud.sentinel.feign.SentinelContractHolder;
import com.alibaba.cloud.sentinel.feign.SentinelInvocationHandler;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.MethodMetadata;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import static feign.Util.checkNotNull;

/**
 * 重写 SentinelInvocationHandler，支持自动降级注入
 * <p>
 * Feign 调用异常时：
 * 1. 如配置了 fallbackFactory，走 fallbackFactory 降级
 * 2. 如返回类型为 org.jeecg.common.api.vo.Result，通过反射创建 Result.error() 返回
 * 3. 否则抛出原始异常
 *
 * @author jeecg
 * @date 2026-07-07
 */
@Slf4j
public class JeecgSentinelInvocationHandler implements InvocationHandler {

	public static final String EQUALS = "equals";
	public static final String HASH_CODE = "hashCode";
	public static final String TO_STRING = "toString";

	/** Result 类的全限定名 */
	private static final String RESULT_CLASS = "org.jeecg.common.api.vo.Result";

	private final Target<?> target;
	private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;
	private FallbackFactory<?> fallbackFactory;
	private Map<Method, Method> fallbackMethodMap;

	JeecgSentinelInvocationHandler(Target<?> target, Map<Method, InvocationHandlerFactory.MethodHandler> dispatch,
			FallbackFactory<?> fallbackFactory) {
		this.target = checkNotNull(target, "target");
		this.dispatch = checkNotNull(dispatch, "dispatch");
		this.fallbackFactory = fallbackFactory;
		this.fallbackMethodMap = toFallbackMethod(dispatch);
	}

	JeecgSentinelInvocationHandler(Target<?> target, Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
		this.target = checkNotNull(target, "target");
		this.dispatch = checkNotNull(dispatch, "dispatch");
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		if (EQUALS.equals(method.getName())) {
			try {
				Object otherHandler = args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
				return equals(otherHandler);
			} catch (IllegalArgumentException e) {
				return false;
			}
		} else if (HASH_CODE.equals(method.getName())) {
			return hashCode();
		} else if (TO_STRING.equals(method.getName())) {
			return toString();
		}

		Object result;
		InvocationHandlerFactory.MethodHandler methodHandler = this.dispatch.get(method);
		if (target instanceof Target.HardCodedTarget) {
			Target.HardCodedTarget<?> hardCodedTarget = (Target.HardCodedTarget<?>) target;
			MethodMetadata methodMetadata = SentinelContractHolder.METADATA_MAP
					.get(hardCodedTarget.type().getName() + Feign.configKey(hardCodedTarget.type(), method));
			if (methodMetadata == null) {
				result = methodHandler.invoke(args);
			} else {
				String resourceName = methodMetadata.template().method().toUpperCase() + ":"
						+ hardCodedTarget.url() + methodMetadata.template().path();
				Entry entry = null;
				try {
					ContextUtil.enter(resourceName);
					entry = SphU.entry(resourceName, EntryType.OUT, 1, args);
					result = methodHandler.invoke(args);
				} catch (Throwable ex) {
					if (!BlockException.isBlockException(ex)) {
						Tracer.trace(ex);
					}
					if (fallbackFactory != null) {
						try {
							Object fallbackResult = fallbackMethodMap.get(method)
									.invoke(fallbackFactory.create(ex), args);
							return fallbackResult;
						} catch (IllegalAccessException e) {
							throw new AssertionError(e);
						} catch (InvocationTargetException e) {
							throw new AssertionError(e.getCause());
						}
					} else if (isResultType(method.getReturnType())) {
						log.error("Feign 服务间调用异常", ex);
						return createResultError(ex.getLocalizedMessage());
					} else {
						throw ex;
					}
				} finally {
					if (entry != null) {
						entry.exit(1, args);
					}
					ContextUtil.exit();
				}
			}
		} else {
			result = methodHandler.invoke(args);
		}
		return result;
	}

	/**
	 * 判断返回类型是否为 Result
	 */
	private boolean isResultType(Class<?> returnType) {
		try {
			Class<?> resultClass = Class.forName(RESULT_CLASS);
			return resultClass.isAssignableFrom(returnType);
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * 通过反射创建 Result.error(msg)
	 */
	private Object createResultError(String msg) {
		try {
			Class<?> resultClass = Class.forName(RESULT_CLASS);
			Method errorMethod = resultClass.getMethod("error", String.class);
			return errorMethod.invoke(null, msg);
		} catch (Exception e) {
			log.warn("无法通过反射创建 Result.error()", e);
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SentinelInvocationHandler) {
			JeecgSentinelInvocationHandler other = (JeecgSentinelInvocationHandler) obj;
			return target.equals(other.target);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return target.hashCode();
	}

	@Override
	public String toString() {
		return target.toString();
	}

	static Map<Method, Method> toFallbackMethod(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
		Map<Method, Method> result = new LinkedHashMap<>();
		for (Method method : dispatch.keySet()) {
			method.setAccessible(true);
			result.put(method, method);
		}
		return result;
	}

}
