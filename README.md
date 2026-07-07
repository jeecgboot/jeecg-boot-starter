# jeecg-boot-starter

当前最新版本： 3.9.3（发布日期：2026-07-07）

### 介绍
> jeecg-boot-starter 是 JeecgBoot 低代码平台的核心启动器模块集合，基于 Spring Boot 4 和 JDK 17 构建。
> 
> 本项目将各类技术组件的starter模块独立出来，采用模块化设计，提供开箱即用的企业级功能组件，包括：
> - 微服务架构支持（Spring Cloud & Spring Cloud Alibaba）
> - 服务治理（Sentinel 限流熔断）
> - 分布式事务（Seata）
> - 分布式锁（Redisson）
> - 定时任务（XXL-Job）
> - 消息队列（RabbitMQ、RocketMQ）
> - 分库分表（ShardingSphere）
> - NoSQL数据库（MongoDB）
> - AI集成（LangChain4j）
> 
> 通过模块化设计，开发者可以按需引入所需的starter，简化项目配置，提高开发效率。

### 软件架构

#### 核心框架
  - Spring Boot：4.0.1
  - Java：17
  - Spring Cloud：2025.1.2
  - Spring Cloud Alibaba：2025.1.0.0

#### 主要依赖
  - Hutool：5.8.25
  - Guava：32.1.3-jre
  - Redisson：3.16.1（分布式锁）
  - XXL-Job：3.4.2（定时任务）
  - Dynamic-Datasource：4.5.0（动态数据源）
  - LangChain4j：1.12.2（AI集成）
  - RocketMQ：2.3.4（消息队列）


### jeecg-boot-starter项目说明

``` 
├── jeecg-boot-starter              -- starter父模块
    ├── jeecg-boot-common              -- 底层共通类（单体和微服务公用）
    ├── jeecg-boot-starter-ai          -- AI集成starter（LangChain4j）
    ├── jeecg-boot-starter-cloud       -- 微服务启动starter
    ├── jeecg-boot-starter-sentinel    -- Sentinel限流熔断starter
    ├── jeecg-boot-starter-job         -- xxl-job定时任务starter
    ├── jeecg-boot-starter-lock        -- 分布式锁starter
    ├── jeecg-boot-starter-mongon      -- MongoDB数据库starter
    ├── jeecg-boot-starter-rabbitmq    -- RabbitMQ消息中间件starter
    ├── jeecg-boot-starter-rocketmq    -- RocketMQ消息中间件starter
    ├── jeecg-boot-starter-seata       -- 分布式事务starter
    ├── jeecg-boot-starter-shardingsphere  -- 分库分表starter
    ├── jeecg-boot-starter-shardingsphere-nacos  -- 分库分表starter(实现ShardingSphere 从Nacos加载分片配置)
```

### 模块功能说明

#### 核心模块
- **jeecg-boot-common**：底层共通工具类和基础配置，为所有starter提供统一的工具支持

#### 微服务模块
- **jeecg-boot-starter-cloud**：基于Spring Cloud Alibaba的微服务支持，提供服务注册、发现、配置中心等功能
- **jeecg-boot-starter-sentinel**：集成Sentinel限流熔断组件，支持Feign降级、流量控制、系统负载保护等功能
- **jeecg-boot-starter-seata**：集成Seata分布式事务解决方案，支持AT、TCC、SAGA等事务模式

#### 任务调度模块
- **jeecg-boot-starter-job**：集成XXL-Job分布式任务调度平台，支持定时任务的统一管理和调度

#### 分布式锁模块
- **jeecg-boot-starter-lock**：基于Redisson实现的分布式锁，支持可重入锁、公平锁、读写锁等多种锁模式

#### 消息队列模块
- **jeecg-boot-starter-rabbitmq**：RabbitMQ消息中间件集成，支持消息的发送、接收和可靠性保证
- **jeecg-boot-starter-rocketmq**：RocketMQ消息中间件集成，支持顺序消息、事务消息、延时消息等

#### 数据库模块
- **jeecg-boot-starter-mongon**：MongoDB NoSQL数据库集成，提供文档数据库操作支持
- **jeecg-boot-starter-shardingsphere**：基于Apache ShardingSphere的分库分表解决方案
- **jeecg-boot-starter-shardingsphere-nacos**：ShardingSphere与Nacos配置中心集成，支持动态配置分片规则

#### AI集成模块
- **jeecg-boot-starter-ai**：基于LangChain4j的AI集成模块，支持OpenAI、Ollama、通义千问、文心一言、智谱AI、Anthropic等多种大模型

### 安装使用

#### Maven依赖引入

在项目的 `pom.xml` 中添加所需的starter依赖：

```xml
<!-- 分布式锁 -->
<dependency>
    <groupId>org.jeecgframework.boot3</groupId>
    <artifactId>jeecg-boot-starter-lock</artifactId>
    <version>3.9.3</version>
</dependency>

<!-- 定时任务 -->
<dependency>
    <groupId>org.jeecgframework.boot3</groupId>
    <artifactId>jeecg-boot-starter-job</artifactId>
    <version>3.9.3</version>
</dependency>

<!-- RabbitMQ消息队列 -->
<dependency>
    <groupId>org.jeecgframework.boot3</groupId>
    <artifactId>jeecg-boot-starter-rabbitmq</artifactId>
    <version>3.9.3</version>
</dependency>

<!-- 更多模块按需引入... -->
```

#### 特性说明

- ✅ **开箱即用**：引入依赖后自动配置，无需复杂配置
- ✅ **模块化设计**：按需引入，避免不必要的依赖
- ✅ **Spring Boot 4**：基于最新的Spring Boot 4.x版本
- ✅ **JDK 17**：支持最新的Java特性
- ✅ **生产级别**：经过大量项目验证，稳定可靠
- ✅ **持续更新**：紧跟技术发展，定期更新维护

### 常见问题

#### Redis配置问题

如果生产环境的Redis不支持订阅（SUBSCRIBE）命令，会导致应用启动失败。解决方案：

```yaml
jeecg:
  redis:
    # 禁用Redis消息监听器（当生产环境Redis不支持SUBSCRIBE命令时）
    listener-enabled: false
```

### 技术支持

- 本项目关闭issue，使用中遇到问题或BUG可以在 [JeecgBoot主项目上提Issues](https://github.com/jeecgboot/jeecg-boot/issues/new)
- 官方支持： https://jeecg.com/doc/help
- 官方文档： https://help.jeecg.com
