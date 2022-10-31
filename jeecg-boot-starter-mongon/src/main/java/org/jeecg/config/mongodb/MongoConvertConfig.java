package org.jeecg.config.mongodb;

import org.jeecg.config.mongodb.converter.BigDecimalToDecimal128Converter;
import org.jeecg.config.mongodb.converter.Decimal128ToBigDecimalConverter;
import org.jeecg.config.mongodb.converter.timeStamp.DateToTimeStamp;
import org.jeecg.config.mongodb.converter.timeStamp.TimeStampToDate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.List;

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
        List<Converter<?, ?>> converterList = new ArrayList<>();
        converterList.add(new BigDecimalToDecimal128Converter());
        converterList.add(new Decimal128ToBigDecimalConverter());

        //日期方案——字符串格式互转方案
//        converterList.add(new DateToString());
//        converterList.add(new StringToDate());

        //日期方案——时间戳格式互转方案
        converterList.add(new DateToTimeStamp());
        converterList.add(new TimeStampToDate());

        return new MongoCustomConversions(converterList);
    }

}
