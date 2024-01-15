package org.jeecg.chatgpt.dto.image;

/**
 * 图像格式化类型
 * @author chenrui
 * @date 2024/1/12 18:20
 */
public enum ImageFormat {

    /**
     * URL
     */
    URL("url"),
    /**
     * Base64类型
     */
    BASE64("b64_json");

    /**
     * 格式化类型
     */
    private final String format;

    ImageFormat(String format){
        this.format = format;
    }

    public String getFormat(){
        return format;
    }

}
