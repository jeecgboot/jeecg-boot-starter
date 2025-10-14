# ShardingSphere + Nacos 集成配置说明

## 配置概述

本项目实现了 ShardingSphere 从 Nacos 配置中心动态加载分片配置的功能。

## 推荐配置（最佳实践）

### 在 Nacos 配置中心配置数据源

**在 Nacos 的 `jeecg-dev.yaml` 配置文件中添加:**

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        # 主数据源（原有配置保持不变）
        master:
          url: jdbc:mysql://jeecg-boot-mysql:3306/jeecg-boot?characterEncoding=UTF-8&useUnicode=true&useSSL=false
          username: root
          password: root
          driver-class-name: com.mysql.cj.jdbc.Driver
        
        # ShardingSphere 分片数据源 - 推荐配置
        sharding-db:
          driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
          url: jdbc:shardingsphere:nacos:sharding.yaml?serverAddr=${spring.cloud.nacos.config.server-addr}&namespace=${spring.cloud.nacos.config.namespace}&group=${spring.cloud.nacos.config.group}
```

> **优势**: 
> - 自动复用现有的 Spring Cloud Nacos 配置参数
> - 无需额外配置 Nacos 连接信息
> - 配置完全中心化管理

## Nacos 分片配置

### 配置项信息
- **Data ID**: `sharding.yaml`
- **Group**: 与应用配置组一致
- **命名空间**: 与应用命名空间一致

### 分片配置示例

在 Nacos 中创建 `sharding.yaml` 配置:

```yaml
# 数据源配置
dataSources:
  db_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/jeecg_boot_0?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root

# 分片规则配置
rules:
  - !SHARDING
    tables:
      sys_log:
        actualDataNodes: db_0.sys_log$->{0..1}
        tableStrategy:
          standard:
            shardingColumn: log_type
            shardingAlgorithmName: user_inline
        keyGenerateStrategy:
          column: id
          keyGeneratorName: snowflake
    keyGenerators:
      snowflake:
        type: SNOWFLAKE
        props:
          worker-id: 123
    shardingAlgorithms:
      user_inline:
        type: INLINE
        props:
          algorithm-expression: sys_log$->{log_type % 2}

# 日志配置
props:
  sql-show: true
```

## 使用方式

在 Service 中指定分片数据源:

```java
@Service
public class SysLogService {
    
    @DS("sharding-db")  // 指定使用分片数据源
    public void save(SysLog sysLog) {
        // 数据会自动根据分片规则路由到对应的表
        sysLogMapper.insert(sysLog);
    }
}
```

## 核心实现

### SPI 实现类
**文件**: `src/main/java/org/jeecg/boot/shardingsphere/config/ShardingSphereSpringNacosURLLoader.java`

```java
package org.jeecg.boot.shardingsphere.config;

/**
 * @title: ShardingSphereSpringNacosURLLoader
 * @description: 实现SPI，读取远程的nacos配置
 * @author: arron
 * @date: 2024/8/28 22:09
 */
public class ShardingSphereSpringNacosURLLoader implements ShardingSphereURLLoader {

    /**
     * 定义jdbc:shardingsphere:后的类型为nacos:
     */
    private static final String NACOS_TYPE = "nacos:";

    /**
     * 接收nacos:后的参数sharding.yaml?serverAddr=${nacos.service-address}&namespace=${nacos.namespace}&group=${nacos.group}&username=${nacos.username}&password=${nacos.password}
     * @param configurationSubject configuration dataId
     * @param queryProps url参数，已经解析成为Properties
     * @return
     */
    @Override
    @SneakyThrows
    public String load(String configurationSubject, Properties queryProps) {
        ConfigService configService = NacosFactory.createConfigService(queryProps);
        String dataId = configurationSubject;
        //获取nacos配置
        String config = configService.getConfig(dataId, queryProps.getProperty(Constants.GROUP, Constants.DEFAULT_GROUP), 500);
        Preconditions.checkArgument(config != null, "Nacos config [" + dataId + "] is Empty.");
        return config;
    }

    @Override
    public Object getType() {
        return NACOS_TYPE;
    }
}
```

### SPI 注册文件
**文件**: `src/main/resources/META-INF/services/org.apache.shardingsphere.infra.url.spi.ShardingSphereURLLoader`
```
org.jeecg.boot.shardingsphere.config.ShardingSphereSpringNacosURLLoader
```

### 依赖配置
**文件**: `pom.xml`
```xml
<dependencies>
    <dependency>
        <groupId>org.jeecgframework.boot3</groupId>
        <artifactId>jeecg-boot-starter-shardingsphere</artifactId>
        <version>3.8.3.1</version>
    </dependency>
    <!-- Nacos配置中心 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
</dependencies>
```

## 优势特点

1. **配置复用**: 自动使用现有 Spring Cloud Nacos 配置
2. **中心化管理**: 所有分片配置在 Nacos 中统一管理
3. **环境隔离**: 通过命名空间和分组实现不同环境配置隔离
4. **安全性**: 数据库连接信息不暴露在应用代码中
5. **简化配置**: 无需额外的 Nacos 连接参数配置

## 故障排查

1. 检查 Spring Cloud Nacos 配置是否正常
2. 确认 `sharding.yaml` 已上传到对应命名空间和分组
3. 查看应用启动日志中的错误信息
4. 验证 SPI 注册文件配置正确
5. 确认 `ShardingSphereSpringNacosURLLoader` 类路径正确

## 其他配置方案

<details>
<summary>点击查看本地 application.yml 配置方案（不推荐）</summary>

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        sharding-db:
          driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
          url: jdbc:shardingsphere:nacos:sharding.yaml?serverAddr=@config.server-addr@&namespace=@config.namespace@&group=@config.group@
```

> 注意: 需要通过 Maven profile 在构建时替换占位符，配置管理复杂度较高。

</details>

---
*配置更新日期: 2025-10-14*  
*状态: ✅ 已验证可用*
