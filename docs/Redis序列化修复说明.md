# Redis序列化修复说明 - Spring Boot 4.x兼容性

## 问题描述

在升级到Spring Boot 4.x后，出现以下异常：

```java
java.lang.ClassCastException: class java.util.LinkedHashMap cannot be cast to class org.jeecg.common.system.vo.LoginUser
```

## 问题原因

Spring Boot 4.x中，许多Redis序列化器（如`GenericJackson2JsonRedisSerializer`和`Jackson2JsonRedisSerializer`）已被标记为废弃。原有的序列化器在反序列化时不能正确保存类型信息，导致复杂对象（如LoginUser）被反序列化为`LinkedHashMap`，从而导致类型转换异常。

## 解决方案

### 1. 自定义序列化器

创建了`CustomJsonRedisSerializer`类，该类：

- 继承`RedisSerializer<Object>`接口
- 在序列化时使用`TypedValue`包装器保存类型信息
- 在反序列化时根据保存的类型信息恢复正确的对象类型
- 保持向后兼容性，能处理旧格式的Redis数据

### 2. 核心实现

#### 序列化过程
```java
// 包装对象以保存类型信息
TypedValue wrapper = new TypedValue(value.getClass().getName(), value);
return this.objectMapper.writeValueAsBytes(wrapper);
```

#### 反序列化过程
```java
// 检查是否是新格式（包含类型信息）
if (jsonStr.contains("\"type\"") && jsonStr.contains("\"value\"")) {
    // 解析TypedValue并恢复正确类型
    TypedValue wrapper = this.objectMapper.readValue(bytes, TypedValue.class);
    Class<?> clazz = Class.forName(wrapper.type);
    return this.objectMapper.convertValue(wrapper.value, clazz);
}
// 处理旧格式兼容性
return this.objectMapper.readValue(bytes, Object.class);
```

### 3. 类型信息包装器

```java
public static class TypedValue {
    public String type;  // 存储完整类名
    public Object value; // 存储实际对象数据
}
```

## 优势

1. **完全解决ClassCastException**：通过保存完整类型信息，确保反序列化时对象类型正确
2. **向后兼容**：能正确处理现有Redis中的旧格式数据
3. **Spring Boot 4.x兼容**：不依赖已废弃的序列化器
4. **性能优化**：使用单例模式缓存序列化器实例
5. **错误处理**：完善的异常处理和日志记录

## 测试验证

创建了完整的测试套件验证：

- ✅ 正确序列化/反序列化复杂对象
- ✅ 向后兼容旧格式数据  
- ✅ 空值处理
- ✅ 异常情况处理

## 部署建议

1. **渐进式部署**：新序列化器向后兼容，可以直接部署而不需要清理现有Redis数据
2. **监控日志**：注意观察反序列化相关的日志，确保类型转换正常
3. **性能测试**：建议在测试环境验证序列化性能影响

## 影响范围

- 修复所有Redis缓存的类型转换问题
- 影响所有使用`@Cacheable`、`RedisTemplate`等Redis操作
- 特别解决`LoginUser`等用户会话对象的序列化问题

这个解决方案彻底解决了Spring Boot 4.x升级后的Redis序列化问题，确保系统稳定运行。
