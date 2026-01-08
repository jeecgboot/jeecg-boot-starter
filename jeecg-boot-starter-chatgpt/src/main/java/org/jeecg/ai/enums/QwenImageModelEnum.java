package org.jeecg.ai.enums;

import lombok.Getter;

/**
 * 千问图片模型枚举
 *
 * @Author: chenrui
 * @Date: 2026/1/7 20:12
 */
@Getter
public enum QwenImageModelEnum {

    /**
     * Wanx v1
     */
    WANX_V1("wanx-v1"),

    /**
     * Wanx 2.1 图像编辑
     */
    WANX_2_1_IMAGE_EDIT("wanx2.1-imageedit"),

    /**
     * Wan 2.5 I2I 预览
     */
    WAN_2_5_I2I_PREVIEW("wan2.5-i2i-preview");

    private final String modelName;

    QwenImageModelEnum(String modelName) {
        this.modelName = modelName;
    }
}
