package org.jeecg.config.mongodb.converter.dateStr;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 保存时，将Date时间转为字符串时间，格式为yyyy-MM-dd HH:ss:mm
 * Direction: Java -> MongoDB
 */
@WritingConverter
public class DateToString implements Converter<Date, String> {
    @Override
    public String convert(Date source) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm");
        return sf.format(source);
    }
}
