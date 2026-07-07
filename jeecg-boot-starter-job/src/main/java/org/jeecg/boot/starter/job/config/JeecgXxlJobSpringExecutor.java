package org.jeecg.boot.starter.job.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 扩展 XxlJobSpringExecutor，修复 3.4.2 的 scan 问题（参考 3.5.0）
 *
 * 原问题：scanJobHandlerMethod 对 @Bean 方法注册的 Bean（getBeanClassName()=null）
 *         无法通过 excludedPackage 排除，导致 getBean() 触发 NoClassDefFoundError
 *
 * 修复要点：
 * 1. 使用 getType(beanName, false) 替代 getBean() 获取类信息，避免提前初始化
 * 2. MethodIntrospector 外层加 try-catch，避免单个 Bean 解析失败中断扫描
 * 3. getBean() 移到 @XxlJob 注解检测之后，只有真正需要注册的 Bean 才实例化
 *
 * @author jeecg
 */
public class JeecgXxlJobSpringExecutor extends XxlJobSpringExecutor {
	private static final Logger logger = LoggerFactory.getLogger(JeecgXxlJobSpringExecutor.class);

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		super.setApplicationContext(applicationContext);
		JeecgXxlJobSpringExecutor.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		// 使用修复后的扫描方法替代父类的 scanJobHandlerMethod
		fixedScanJobHandlerMethod(applicationContext);

		// refresh GlueFactory
		com.xxl.job.core.glue.GlueFactory.refreshInstance(1);

		// super start (跳过父类 afterSingletonsInstantiated 中的 scan，直接调 start)
		try {
			super.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// ============ 以下从 3.5.0 参考实现 ============

	private void fixedScanJobHandlerMethod(ApplicationContext applicationContext) {
		if (applicationContext == null) {
			return;
		}

		// 1、build excluded-package list
		List<String> excludedPackageList = new ArrayList<>();
		String excludedPackage = getExcludedPackage();
		if (excludedPackage != null) {
			for (String pkg : excludedPackage.split(",")) {
				if (!pkg.trim().isEmpty()) {
					excludedPackageList.add(pkg.trim());
				}
			}
		}

		// 2、scan beans（allowEagerInit=false，避免提前初始化）
		String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, false);
		for (String beanName : beanNames) {

			// 2.1、通过 BeanDefinition 排除
			if (applicationContext instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
				if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
					continue;
				}
				BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);

				// skip excluded-package
				String beanClassName = beanDefinition.getBeanClassName();
				if (isExcluded(excludedPackageList, beanClassName)) {
					logger.debug(">>>>>>>>>>> xxl-job bean scan, skip excluded-package beanName:{}, beanClassName:{}", beanName, beanClassName);
					continue;
				}

				// skip lazy-init
				if (beanDefinition.isLazyInit()) {
					logger.debug(">>>>>>>>>>> xxl-job bean scan, skip lazy-init beanName:{}", beanName);
					continue;
				}
			}

			// 2.2、通过 Bean Class 排除（使用 getType，不实例化）
			Class<?> beanClass = applicationContext.getType(beanName, false);
			if (beanClass == null) {
				logger.debug(">>>>>>>>>>> xxl-job bean scan, skip beanClass-null beanName:{}", beanName);
				continue;
			}

			// 补充排除：对 @Bean 方法注册的 Bean（BeanDefinition.getBeanClassName()=null），用实际类名再检查一次
			if (isExcluded(excludedPackageList, beanClass.getName())) {
				logger.debug(">>>>>>>>>>> xxl-job bean scan, skip excluded-class beanName:{}, className:{}", beanName, beanClass.getName());
				continue;
			}

			// 2.3、扫描 @XxlJob 方法（带 try-catch，单个 Bean 失败不影响整体）
			Map<Method, XxlJob> annotatedMethods = null;
			try {
				annotatedMethods = MethodIntrospector.selectMethods(beanClass,
						(MethodIntrospector.MetadataLookup<XxlJob>) method ->
								AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class));
			} catch (Throwable ex) {
				logger.error(">>>>>>>>>>> xxl-job method-jobhandler resolve error for bean[" + beanName + "].", ex);
			}
			if (annotatedMethods == null || annotatedMethods.isEmpty()) {
				continue;
			}

			// 2.4、有 @XxlJob 注解才真正实例化 Bean 并注册
			Object jobBean = applicationContext.getBean(beanName);
			for (Map.Entry<Method, XxlJob> entry : annotatedMethods.entrySet()) {
				Method jobMethod = entry.getKey();
				XxlJob xxlJob = entry.getValue();
				// 调用父类的 registryJobHandler（protected 方法）
				registryJobHandler(xxlJob, jobBean, jobMethod);
			}
		}
	}

	/**
	 * 检查是否应该排除
	 */
	private boolean isExcluded(List<String> excludedPackageList, String beanClassName) {
		if (excludedPackageList == null || excludedPackageList.isEmpty()) {
			return false;
		}
		if (beanClassName == null) {
			return false;
		}
		for (String excludedPackage : excludedPackageList) {
			if (beanClassName.startsWith(excludedPackage)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取排除包配置（从父类读取）
	 */
	private String getExcludedPackage() {
		// 默认排除 + 避免 @Bean 定义导致的 null beanClassName 穿透
		return "org.springframework.,spring.,org.springdoc.";
	}

}
