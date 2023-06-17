package org.jeecg.chatgpt.dto.image;

import java.util.Date;
import java.util.List;

/**
 * 
 * @author liuliu
 *
 */
public class ImageResponse {
    private Date created;

    private List<ImageData> data;

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public List<ImageData> getData() {
        return data;
    }

    public void setData(List<ImageData> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ImageResponse [created=" + created + ", data=" + data + "]";
    }

}
