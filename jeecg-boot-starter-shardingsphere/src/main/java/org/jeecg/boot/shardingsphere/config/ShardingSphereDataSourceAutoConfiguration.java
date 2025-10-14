//package org.jeecg.boot.shardingsphere.config;//package org.jeecg.shardingsphere.config;
//
//import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//
//import javax.sql.DataSource;
//import java.io.IOException;
//import java.sql.SQLException;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * ShardingSphere 数据源自动配置类
// * 自动将 sharding.yaml 配置的数据源注册到动态数据源中
// */
//@Slf4j
//@Configuration
//@ConditionalOnProperty(prefix = "spring.shardingsphere", name = "enabled", havingValue = "true", matchIfMissing = true)
//public class ShardingSphereDataSourceAutoConfiguration {
//    /**
//     * 分库分表数据源名称
//     */
//    public static final String SHARDING_DATA_SOURCE_NAME = "sharding-db";
//    /**
//     * sharding.yaml 配置文件路径常量
//     */
//    private static final String SHARDING_YAML_PATH = "sharding.yaml";
//
//    /**
//     * 创建 ShardingSphere 数据源提供者
//     * 自动将 sharding.yaml 配置的数据源添加到动态数据源映射中
//     */
//    @Bean
//    public DynamicDataSourceProvider shardingSphereDataSourceProvider() {
//        return new DynamicDataSourceProvider() {
//            @Override
//            public Map<String, DataSource> loadDataSources() {
//                Map<String, DataSource> dataSourceMap = new HashMap<>();
//
//                try {
//                    // 从 classpath 加载 sharding.yaml 配置创建 ShardingSphere 数据源
//                    DataSource shardingDataSource = createShardingSphereDataSource();
//
//                    // 添加到数据源映射中，数据源名称为 sharding（与 sharding.yaml 中的 databaseName 一致）
//                    dataSourceMap.put(SHARDING_DATA_SOURCE_NAME, shardingDataSource);
//
//                    log.info("=== ShardingSphere 分库分表 数据源自动配置 ===");
//                    log.info("✅ 数据源 [{}] 注册成功", SHARDING_DATA_SOURCE_NAME);
//                    log.info("📁 配置文件: classpath:sharding.yaml");
//                    log.info("🔧 数据源类型: {}", shardingDataSource.getClass().getSimpleName());
//                    log.info("");
//                    log.info("📋 等效的手动配置:");
//                    log.info("spring:");
//                    log.info("  datasource:");
//                    log.info("    dynamic:");
//                    log.info("      datasource:");
//                    log.info("        {}:", SHARDING_DATA_SOURCE_NAME);
//                    log.info("          driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver");
//                    log.info("          url: jdbc:shardingsphere:classpath:sharding.yaml");
//                    log.info("");
//                    log.info("🚀 使用方式: @DS(\"{}\")", SHARDING_DATA_SOURCE_NAME);
//                    log.info("=============================================");
//
//                } catch (Exception e) {
//                    log.error("创建 ShardingSphere 数据源失败", e);
//                    throw new RuntimeException("ShardingSphere 数据源初始化失败", e);
//                }
//
//                return dataSourceMap;
//            }
//        };
//    }
//
//    /**
//     * 创建 ShardingSphere 数据源
//     */
//    private DataSource createShardingSphereDataSource() throws SQLException, IOException {
//        // 从 classpath 加载 sharding.yaml 配置文件
//        ClassPathResource resource = new ClassPathResource(SHARDING_YAML_PATH);
//
//        if (!resource.exists()) {
//            throw new RuntimeException("sharding.yaml 配置文件不存在，请检查 classpath 下是否有此文件");
//        }
//
//        // 使用 ShardingSphere 工厂类创建数据源
//        return YamlShardingSphereDataSourceFactory.createDataSource(resource.getFile());
//    }
//}
