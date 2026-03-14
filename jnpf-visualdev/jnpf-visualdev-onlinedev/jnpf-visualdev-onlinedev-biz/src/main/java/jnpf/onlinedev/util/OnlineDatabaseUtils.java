package jnpf.onlinedev.util;


import jnpf.base.util.FormPublicUtils;

/**
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/7/28
 */
public class OnlineDatabaseUtils {
    OnlineDatabaseUtils() {
    }

    /**
     * 转换时间格式
     *
     * @param time
     * @return
     */
    public static String getTimeFormat(String time) {
        return FormPublicUtils.getTimeFormat(time);
    }

    public static String getLastTimeFormat(String time) {
        return FormPublicUtils.getLastTimeFormat(time);
    }

}
