# jeecg-boot-starter-sentinel

Sentinel 公共库——限流降级、Feign 集成、自动配置，开箱即用。

## 模块结构

```
jeecg-boot-starter-sentinel/
├── JeecgSentinelAutoConfiguration.java    # 自动配置（注册 Bean）
├── handler/
│   └── JeecgUrlBlockHandler.java          # 限流降级处理器（HTTP 429）
├── parser/
│   └── JeecgHeaderRequestOriginParser.java # 请求来源解析器（IP/参数）
└── feign/
    ├── JeecgSentinelFeign.java            # 自定义 Feign Builder
    └── JeecgSentinelInvocationHandler.java # Feign 调用拦截处理器
```

## 功能

### 1. 自动配置

引入依赖后自动生效（无需任何配置），注册以下 Bean：

| Bean | 说明 |
|------|------|
| `Feign.Builder` | 自定义 Feign Builder，自动注入 fallback/fallbackFactory |
| `BlockExceptionHandler` | Servlet 层限流降级，返回统一 JSON（HTTP 429） |
| `RequestOriginParser` | 基于请求参数 `origin` 或客户端 IP 的授权来源解析 |

### 2. Feign 熔断降级

- 支持 `@FeignClient` 的 `fallback` 和 `fallbackFactory` 自动注入
- Feign 调用异常且返回类型为 `Result` 时，自动返回 `Result.error()`
- 配置了 `fallbackFactory` 时优先走降级逻辑

### 3. 限流降级响应

被 Sentinel 限流时返回 HTTP 429 + JSON：

| BlockException 类型 | 提示信息 |
|-----|------|
| FlowException | 访问频繁，请稍候再试 |
| DegradeException | 系统降级 |
| ParamFlowException | 热点参数限流 |
| SystemBlockException | 系统规则限流或降级 |
| AuthorityException | 授权规则不通过 |

### 4. 请求来源解析

- 优先取请求参数 `origin`
- 参数为空时取 `X-Forwarded-For` 头（支持代理）
- 最后 fallback 到 `RemoteAddr`

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>org.jeecgframework.boot3</groupId>
    <artifactId>jeecg-boot-starter-sentinel</artifactId>
</dependency>
```

### 开启 Feign Sentinel

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: jeecg-boot-sentinel:9000
    openfeign:
      sentinel:
        enabled: true   # 默认 true，可不配
```

### Feign 降级示例

```java
@FeignClient(value = "jeecg-system", fallbackFactory = SysBaseAPIFallbackFactory.class)
public interface ISysBaseAPI {
    // 调用失败时自动走 SysBaseAPIFallbackFactory
}
```

未配置 `fallbackFactory` 但返回类型为 `Result` 时，自动返回 `Result.error("xxx")`。

## 与 jeecg-boot-starter-cloud 的关系

`starter-cloud` 不自动依赖本模块，按需引入：

```
微服务只需要 Nacos + Feign  → 只引 starter-cloud
微服务还需要限流熔断        → 再加 starter-sentinel
```

## 升级说明

本模块从 `jeecg-boot-starter-cloud` 中拆分出来，原有类迁移对照：

| 旧类（starter-cloud，已删除） | 新类（starter-sentinel） | 变化 |
|---|---|---|
| `CustomSentinelExceptionHandler` | `JeecgUrlBlockHandler` | HTTP 429 代替 200，jackson 序列化 |
| `DefaultRequestOriginParser` | `JeecgHeaderRequestOriginParser` | 增加 X-Forwarded-For 代理支持 |

迁移后 starter-cloud 不再内置 sentinel 依赖，需按需引入本模块。

## 版本

| 组件 | 版本 |
|------|------|
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.2 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Sentinel | 1.8.6+（BOM 管理） |
