# jeecg-boot-starter-shardingsphere 使用说明

## 概述

`jeecg-boot-starter-shardingsphere` 是 JeecgBoot 框架提供的分库分表 Starter，集成了 Apache ShardingSphere 5.5.0 和动态数据源功能，支持数据分片、读写分离等功能。

## 特性

- ✅ **自动集成**: 开箱即用的 ShardingSphere 集成
- ✅ **动态数据源**: 与 dynamic-datasource 完美结合
- ✅ **配置简化**: 支持 YAML 配置文件管理
- ✅ **Spring Boot 3**: 完全支持 Spring Boot 3.x
- ✅ **版本兼容**: 使用最新稳定版本组件

## 依赖版本

```xml
<!-- 核心依赖 -->
<dependency>
    <groupId>org.jeecgframework.boot3</groupId>
    <artifactId>jeecg-boot-starter-shardingsphere</artifactId>
    <version>3.9.2</version>
</dependency>
```

**内置版本**:
- Apache ShardingSphere: `5.5.0`
- Dynamic DataSource: `4.3.1` 
- Spring Boot: `3.x` 兼容

## 快速开始

### 1. 添加依赖

在项目 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>org.jeecgframework.boot3</groupId>
    <artifactId>jeecg-boot-starter-shardingsphere</artifactId>
    <version>3.9.2</version>
</dependency>
```

### 2. 配置数据源

在 `application.yml` 中配置动态数据源：

```yaml
spring:
  datasource:
    dynamic:
      primary: master  # 默认数据源
      strict: false    # 是否严格匹配数据源
      datasource:
        # 主数据源
        master:
          url: jdbc:mysql://localhost:3306/jeecg_boot?useSSL=false&useUnicode=true&characterEncoding=utf-8
          username: root
          password: root
          driver-class-name: com.mysql.cj.jdbc.Driver
          
        # ShardingSphere 分片数据源
        sharding-db:
          driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
          url: jdbc:shardingsphere:classpath:sharding.yaml
```

### 3. 创建分片配置

在 `src/main/resources/` 目录下创建 `sharding.yaml` 配置文件：

> ⚠️ **重要提醒**: `dataSourceClassName` 必须使用 `com.zaxxer.hikari.HikariDataSource`，不能使用其他数据源（如 DruidDataSource），否则会导致分库分表功能异常！

```yaml
# !!!数据源名称要和动态数据源中配置的名称一致
databaseName: sharding-db

# 具体参看官网文档说明
dataSources:
  db_0:
    # ⚠️ 必须使用 HikariDataSource，不能使用 DruidDataSource 等其他数据源
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://jeecg-boot-mysql:3306/jeecg-boot?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    password: root
    username: root

rules:
  - !SHARDING
    tables: # 数据分片规则配置
      sys_log: # 逻辑表名称
        actualDataNodes: db_0.sys_log$->{0..1} # 由数据源名 + 表名组成（参考 Inline 语法规则）
        databaseStrategy: # 分库策略，缺省表示使用默认分库策略，以下的分片策略只能选其一
          none:
        tableStrategy: # 分表策略
          standard: # 用于单分片键的标准分片场景
            shardingColumn: log_type # 分片列名称
            shardingAlgorithmName: user_inline
        keyGenerateStrategy:
          column: id
          keyGeneratorName: snowflake
    keyGenerators:
      snowflake:
        type: SNOWFLAKE
        props:
          worker-id: 123
    # 分片算法配置
    shardingAlgorithms:
      user_inline:
        type: INLINE
        props:
          algorithm-expression: sys_log$->{log_type % 2}

props:
  sql-show: true
```

## 使用方式

### 1. 在 Service 中使用（推荐）

```java
@Service
public class SysLogService {
    
    @Autowired
    private SysLogMapper sysLogMapper;
    
    // 使用分片数据源
    @DS("sharding-db")
    public void saveLog(SysLog sysLog) {
        // 数据会自动根据分片规则路由到对应的表
        sysLogMapper.insert(sysLog);
    }
    
    @DS("sharding-db")
    public List<SysLog> findByLogType(Integer logType) {
        return sysLogMapper.findByLogType(logType);
    }
    
    // 使用主数据源
    @DS("master")
    public List<SysLog> getAllLogs() {
        return sysLogMapper.selectList(null);
    }
    
    // 不指定数据源，使用默认数据源（master）
    public SysLog getById(String id) {
        return sysLogMapper.selectById(id);
    }
}
```

### 2. Mapper 接口定义

```java
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
    
    // 根据日志类型查询（数据源由 Service 层决定）
    @Select("SELECT * FROM sys_log WHERE log_type = #{logType}")
    List<SysLog> findByLogType(@Param("logType") Integer logType);
}
```

### 3. 在 Controller 中使用

```java
@RestController
@RequestMapping("/log")
public class LogController {
    
    @Autowired
    private SysLogService sysLogService;
    
    @PostMapping("/save")
    public Result<?> saveLog(@RequestBody SysLog sysLog) {
        // 自动分片存储
        sysLogService.saveLog(sysLog);
        return Result.ok("保存成功");
    }
    
    @GetMapping("/type/{logType}")
    public Result<List<SysLog>> getByType(@PathVariable Integer logType) {
        List<SysLog> logs = sysLogService.findByLogType(logType);
        return Result.ok(logs);
    }
}
```

## 最佳实践说明

### 💡 `@DS` 注解使用建议

1. **推荐在 Service 层使用**: 
   - 便于事务管理和业务逻辑控制
   - 同一个 Mapper 可在不同业务场景下使用不同数据源
   - 职责分离，Mapper 专注数据访问，Service 负责数据源选择

2. **特殊情况下可在 Mapper 层使用**:
   ```java
   @Mapper
   @DS("sharding-db")  // 仅当整个 Mapper 固定使用分片数据源时
   public interface ShardingOnlyMapper extends BaseMapper<SysLog> {
       // 所有方法都会使用 sharding-db
   }
   ```

3. **避免混用**: 不要在同一个调用链路中的 Service 和 Mapper 都加 `@DS` 注解，可能导致数据源切换混乱

### ⚠️ 注意事项

- 分片表需要预先在数据库中创建（如 `sys_log0`, `sys_log1`）
- 确保分片键的值能够正确路由到对应的分片表
- 在生产环境中建议关闭 `sql-show` 以提高性能

## 配置说明

### 重要提示

1. **数据源名称一致性**: `sharding.yaml` 中的 `databaseName` 必须与 `application.yml` 中动态数据源的名称保持一致
2. **数据源类型限制**: ⚠️ **必须使用 `com.zaxxer.hikari.HikariDataSource`**，不能使用 `com.alibaba.druid.pool.DruidDataSource` 等其他数据源，否则会导致分库分表功能异常
3. **分片键选择**: 选择合适的分片键（如 `log_type`）以确保数据均匀分布
4. **表结构**: 需要在数据库中预先创建分片表（如 `sys_log0`, `sys_log1`）

### 错误示例 ❌

```yaml
# 错误：使用 DruidDataSource 会导致分库分表功能异常
dataSources:
  db_0:
    dataSourceClassName: com.alibaba.druid.pool.DruidDataSource  # ❌ 错误！
    # ...其他配置
```

### 正确示例 ✅

```yaml
# 正确：必须使用 HikariDataSource
dataSources:
  db_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource  # ✅ 正确！
    # ...其他配置
```

## 故障排除

1. **启动失败**: 检查 `sharding.yaml` 文件路径和格式
2. **分片不生效**: 确认 `@DS("sharding-db")` 注解使用正确
3. **SQL 执行异常**: 开启 `sql-show: true` 查看实际执行的 SQL
4. **数据源异常**: 确认 `dataSourceClassName` 使用的是 `com.zaxxer.hikari.HikariDataSource`，而不是其他数据源类型

## 参考文献

- https://blog.csdn.net/weixin_46688677/article/details/140139785
- https://github.com/DRAGON-Yeah/shardingsphere-5.5.0-example
- https://blog.csdn.net/qq_39203889/article/details/148902630
- https://blog.csdn.net/qq_22855851/article/details/149332597
- https://juejin.cn/post/7413671424127664138