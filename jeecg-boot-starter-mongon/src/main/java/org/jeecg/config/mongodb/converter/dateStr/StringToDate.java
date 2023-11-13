package org.jeecg.config.mongodb.converter.dateStr;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 从mongo查询数据，将字符串时间转为Date类型
 *
 * Direction: MongoDB -> Java
 */
@ReadingConverter
public class StringToDate implements Converter<String, Date> {
    @Override
    public Date convert(String source) {
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm");
        try {
            return sf.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }
}
