package org.jeecg.config.mongodb.converter.timeStamp;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 从mongo查询数据，将时间戳转为Date类型
 * <p>
 * Direction: MongoDB -> Java
 */
@ReadingConverter
public class TimeStampToDate implements Converter<Long, Date> {
    @Override
    public Date convert(Long source) {
        return new Date(source);
    }
}
