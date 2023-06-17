package org.jeecg.chatgpt.dto.image;

/**
 * 
 * @author liuliu
 *
 */
public class ImageData {
    private String url;
    private String b64_json;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getB64_json() {
        return b64_json;
    }

    public void setB64_json(String b64_json) {
        this.b64_json = b64_json;
    }

    @Override
    public String toString() {
        return "ImageData [url=" + url + ", b64_json=" + b64_json + "]";
    }

}
