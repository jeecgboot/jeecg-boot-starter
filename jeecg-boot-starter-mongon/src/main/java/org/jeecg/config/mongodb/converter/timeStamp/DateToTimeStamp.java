package org.jeecg.config.mongodb.converter.timeStamp;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 保存时，将Date时间转为时间戳格
 * Direction: Java -> MongoDB
 */
@WritingConverter
public class DateToTimeStamp implements Converter<Date, Long> {
    @Override
    public Long convert(Date source) {
        return source.getTime();
    }
}
