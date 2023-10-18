# jeecg-boot-starter
当前最新版本： 3.6.0（发布日期：2023-10-23）

### 介绍
> jeecg-boot的starter启动模块独立出来，简化项目，便于维护。

### 软件架构
  - spring-cloud：2021.0.3
  - spring-cloud-alibaba：2021.0.1.0



### jeecg-boot-starter项目说明

``` 
├── jeecg-boot-starter              -- starter父模块
    ├── jeecg-boot-common              -- 底层共通类（单体和微服务公用）
    ├── jeecg-boot-starter-cloud       -- 微服务启动starter
    ├── jeecg-boot-starter-job           -- xxl-job定时任务starter
    ├── jeecg-boot-starter-lock          -- 分布式锁starter
    ├── jeecg-boot-starter-rabbitmq       -- 消息中间件starter
    ├── jeecg-boot-starter-seata           --分布式事务starter
    ├── jeecg-boot-starter-shardingsphere  -- 分库分表starter
    ├── jeecg-boot-starter-mongon          -- mongostarter
```

### 技术支持

- 本项目关闭issue，使用中遇到问题或BUG可以在 [JeecgBoot主项目上提Issues](https://github.com/jeecgboot/jeecg-boot/issues/new)
- 官方支持： http://jeecg.com/doc/help
- 官方文档： http://doc.jeecg.com
