package org.jeecg.chatgpt.dto.image;

/**
 * 
 * @author liuliu
 *
 */
public enum ImageFormat {

    URL("url"),
    BASE64("b64_json");

    private final String format;

    ImageFormat(String format){
        this.format = format;
    }

    public String getFormat(){
        return format;
    }

}
