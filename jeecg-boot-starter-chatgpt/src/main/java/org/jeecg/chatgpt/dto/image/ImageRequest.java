package org.jeecg.chatgpt.dto.image;

/**
 * 
 * @author liuliu
 *
 */
public class ImageRequest {

    private String prompt;
    private Integer n;
    private String size;
    private String response_format;
    private String user;

    public ImageRequest(String prompt, Integer n, String size, String response_format, String user) {
        super();
        this.prompt = prompt;
        this.n = n;
        this.size = size;
        this.response_format = response_format;
        this.user = user;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getResponse_format() {
        return response_format;
    }

    public void setResponse_format(String response_format) {
        this.response_format = response_format;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "ImageRequest [prompt=" + prompt + ", n=" + n + ", size=" + size + ", response_format=" + response_format
                + ", user=" + user + "]";
    }

}
