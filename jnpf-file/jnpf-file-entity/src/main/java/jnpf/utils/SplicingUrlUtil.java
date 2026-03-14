package jnpf.utils;

import cn.hutool.core.util.ObjectUtil;
import jnpf.model.YozoFileParams;
import jnpf.model.YozoParams;
import jnpf.util.XSSEscape;
import jnpf.util.context.SpringContext;
import org.apache.commons.lang3.StringUtils;


/**
 * @author JNPF开发平台组
 */
public class SplicingUrlUtil {

    private static final YozoParams yozoParams = SpringContext.getBean(YozoParams.class);

    private SplicingUrlUtil() {}
    /**
     * 永中预览url拼接
     * @param params
     * @return
     */
    public static String getPreviewUrl(YozoFileParams params) {
        StringBuilder paramsUrl = new StringBuilder();
        if (!ObjectUtil.isEmpty(params.getNoCache())) {
            paramsUrl.append("&noCache=").append(params.getNoCache());
        }
        if (!StringUtils.isEmpty(params.getWatermark())) {
            String watermark = XSSEscape.escape(params.getWatermark());
            paramsUrl.append("&watermark=").append(watermark);
        }
        if (!ObjectUtil.isEmpty(params.getIsCopy())) {
            paramsUrl.append("&isCopy=").append(params.getIsCopy());
        }
        if (!ObjectUtil.isEmpty(params.getPageStart())) {
            paramsUrl.append("&pageStart=").append(params.getPageStart());
        }
        if (!ObjectUtil.isEmpty(params.getPageEnd())) {
            paramsUrl.append("&pageEnd=").append(params.getPageEnd());
        }
        if (!StringUtils.isEmpty(params.getType())) {
            String type = XSSEscape.escape(params.getType());
            paramsUrl.append("&type=").append(type);
        }
        String s = paramsUrl.toString();
        return yozoParams.getDomain()+"?k=" + yozoParams.getDomainKey() + "&url=" + params.getUrl() + s;
    }

}
