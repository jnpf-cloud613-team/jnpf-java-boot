package jnpf.onlinedev.model.online;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果类
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/11/12 10:10:56
 */
@Data
public class ValidationRes {
    private boolean success;
    private String msg;
    private Object value;
    private List<String> list = new ArrayList<>();

    public ValidationRes(boolean success, String msg, Object value, List<String> list) {
        this.success = success;
        this.msg = msg;
        this.value = value;
        this.list = list;
    }

    public static ValidationRes success() {
        return new ValidationRes(true, null, null, null);
    }

    public static ValidationRes success(Object value) {
        return new ValidationRes(true, null, value, null);
    }

    public static ValidationRes success(Object value, List<String> list) {
        return new ValidationRes(true, null, value, list);
    }

    public static ValidationRes failure(String msg) {
        return new ValidationRes(false, msg, null, null);
    }

}
