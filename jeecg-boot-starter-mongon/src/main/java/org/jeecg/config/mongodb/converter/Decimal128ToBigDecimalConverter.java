package org.jeecg.config.mongodb.converter;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import java.math.BigDecimal;

/**
 * mongo--->java  即Decimal128变为BigDecimal的转换器
 */
@ReadingConverter //告诉spring从数据库读的时候用这个转换
public class Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
    @Override
    public BigDecimal convert(Decimal128 decimal128) {
        return decimal128.bigDecimalValue();
    }
}
