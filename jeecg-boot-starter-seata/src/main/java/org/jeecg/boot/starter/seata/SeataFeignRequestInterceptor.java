package org.jeecg.boot.starter.seata;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author eightmonth
 * @date 2023/10/16 17:32
 */
public class SeataFeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String xid = RootContext.getXID();
        if (!StringUtils.hasLength(xid)) {
            return;
        }

        List<String> seataXid = new ArrayList<>();
        seataXid.add(xid);
        template.header(RootContext.KEY_XID, xid);
    }
}
