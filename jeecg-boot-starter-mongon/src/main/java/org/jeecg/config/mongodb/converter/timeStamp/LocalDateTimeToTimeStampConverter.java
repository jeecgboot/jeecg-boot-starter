package org.jeecg.config.mongodb.converter.timeStamp;

import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 *  保存时，将LocalDateTime时间转为时间戳格
 */
public class LocalDateTimeToTimeStampConverter implements Converter<LocalDateTime, Long> {

    @Override
    public Long convert(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }
}