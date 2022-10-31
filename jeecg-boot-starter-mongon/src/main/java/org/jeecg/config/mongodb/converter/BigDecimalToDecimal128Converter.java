package org.jeecg.config.mongodb.converter;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import java.math.BigDecimal;

/**
 * java-->mongo  即BigDecimal变为Decimal128的转换器
 */
@WritingConverter//告诉spring往数据库写的时候用这个转换
public class BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
    @Override
    public Decimal128 convert(BigDecimal bigDecimal) {
        return new Decimal128(bigDecimal);
    }
}

