# xxl-job-core 2.4.1 → 3.4.2 升级修改说明

> 模块：`jeecg-boot-starter-job`  
> 日期：2026-07-06

---

## 一、版本变更

| 项 | 旧版本 | 新版本 |
|----|--------|--------|
| xxl-job-core | 2.4.1 | **3.4.2** |

---

## 二、变更文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | `xxl-job-core.version` 属性升级 |
| `jeecg-boot-starter-job/pom.xml` | 修改 | 新增 `spring-data-commons` 依赖 |
| `XxlJobExecutor.java` | **删除** | 2.x shadow 类，3.4.2 API 不兼容 |
| `JeecgXxlJobSpringExecutor.java` | **新增** | 扩展 XxlJobSpringExecutor，修复 3.4.2 Bean 扫描缺陷 |
| `XxlJobConfiguration.java` | 修改 | 使用新 Executor + 端口逻辑 + 排除包配置 |

---

## 三、修改详情

### 3.1 删除自定义 XxlJobExecutor

**位置**：`com.xxl.job.core.executor.XxlJobExecutor`（shadow 框架同名类）

**原因**：该类通过 shadow 方式将默认端口从 9999 改为 10000（避开网关冲突）。xxl-job 3.4.2 内部 API 大量重构，shadow 类无法兼容：

| 2.x 类 | 3.4.2 变化 |
|--------|-----------|
| `biz.AdminBiz` | → `openapi.admin.AdminBiz` |
| `biz.client.AdminBizClient` | → 移除，改用 `HttpTool.createClient().proxy()` |
| `thread.JobLogFileCleanThread` | → `JobLogFileCleanThreadHelper` |
| `thread.TriggerCallbackThread` | → `TriggerCallbackThreadHelper` |
| `util.IpUtil` | → `com.xxl.tool.http.IPTool` |
| `util.NetUtil` | → `com.xxl.tool.http.IPTool` |

端口逻辑移至 `XxlJobConfiguration`，通过 `IPTool.getAvailablePort(10000)` 实现同等效果。

### 3.2 新增 JeecgXxlJobSpringExecutor

**位置**：`org.jeecg.boot.starter.job.config.JeecgXxlJobSpringExecutor`

**原因**：3.4.2 的 `XxlJobSpringExecutor.scanJobHandlerMethod()` 存在 Bean 扫描缺陷——对 `@Bean` 方法注册的 Bean（`BeanDefinition.getBeanClassName() = null`），无法通过 `excludedPackage` 排除，导致反射加载 SpringDoc 的 `QuerydslPredicateOperationCustomizer` 时触发：

```
NoClassDefFoundError: org/springframework/data/util/TypeInformation
```

**修复要点**：

1. **双重排除检查**：`BeanDefinition.getBeanClassName()` 为 null 时（`@Bean` 方法注册的 Bean），用 `applicationContext.getType(beanName, false)` 获取实际类名做二次排除
2. **延迟实例化**：`getBean()` 移到 `@XxlJob` 注解检测之后，只有真正需要注册的 Bean 才实例化
3. **异常隔离**：`MethodIntrospector.selectMethods()` 包装 try-catch，单个 Bean 解析失败不影响整体扫描

### 3.3 更新 XxlJobConfiguration

- 使用 `JeecgXxlJobSpringExecutor` 替代原 `XxlJobSpringExecutor`
- 未配置端口时通过 `IPTool.getAvailablePort(10000)` 自动分配，保持"避开网关 9999"的行为
- 添加 `setExcludedPackage("org.springframework.,spring.,org.springdoc.")` 排除 SpringDoc 包扫描

### 3.4 补充 spring-data-commons 依赖

在 `jeecg-boot-starter-job/pom.xml` 中新增 `spring-data-commons`，解决部分场景下 SpringDoc Bean 反射加载时 `TypeInformation` 类缺失的问题。

---

## 四、向后兼容说明

- `@XxlJob` 注解用法完全不受影响，3.4.2 兼容 2.x 的任务写法
- 用户在 `application.yml` 中配置的 `jeecg.xxljob.port` 仍然优先生效
- 未配置端口时默认使用 `IPTool.getAvailablePort(10000)` 自动检测可用端口

---

## 五、验证结果

- 全模块 `mvn clean compile` 通过（12/12 模块）
- 下游项目（jeecg-system-cloud-start）启动正常，xxl-job executor 注册成功
