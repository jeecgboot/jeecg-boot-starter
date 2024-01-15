package org.jeecg.chatgpt.dto.image;

/**
 * 图片尺寸
 * @author chenrui
 * @date 2024/1/12 19:10
 */
public enum ImageSize {

    /**
     * 图片尺寸:1024x1792
     */
    size_1024_1792("1024x1792"),
    /**
     * 图片尺寸:1792x1024
     */
    size_1792_1024("1792x1024"),
    /**
     * 图片尺寸:1024x1024
     */
    size_1024("1024x1024");

    private final String size;

    ImageSize(String size) {
        this.size = size;
    }

    public String getSize() {
        return size;
    }

}
