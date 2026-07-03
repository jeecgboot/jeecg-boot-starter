package org.jeecg.config.mongodb;

import org.jeecg.config.mongodb.converter.Decimal128ToBigDecimalConverter;
import org.jeecg.config.mongodb.converter.timeStamp.DateToTimeStamp;
import org.jeecg.config.mongodb.converter.timeStamp.LocalDateTimeToTimeStampConverter;
import org.jeecg.config.mongodb.converter.timeStamp.TimeStampToDate;
import org.jeecg.config.mongodb.converter.timeStamp.TimeStampToLocalDateTimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConvertConfig {

    /**
     * mongoCustomConversions会由spring进行管理,
     * 按照加入的转换器,在数据库读写时对数据类型进行转换
     *
     * @return
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(config -> {
            config.registerConverter(new Decimal128ToBigDecimalConverter());
            // java --> mongo  即 BigDecimal 变为 Decimal128 的转换器
            config.bigDecimal(MongoCustomConversions.BigDecimalRepresentation.DECIMAL128);

        //日期方案——字符串格式互转方案
//        converterList.add(new DateToString());
//        converterList.add(new StringToDate());

            //日期方案——时间戳格式互转方案
            config.registerConverter(new DateToTimeStamp());
            config.registerConverter(new TimeStampToDate());

            //日期方案——时间戳格式互转方案
            config.registerConverter(new LocalDateTimeToTimeStampConverter());
            config.registerConverter(new TimeStampToLocalDateTimeConverter());
        });
    }

}
