package jnpf.flowable.model.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/10/8 18:05
 */
public class SystemAuditHolder {

    private SystemAuditHolder() {}

    private static final ThreadLocal<List<SystemAuditModel>> MODEL_LIST = new ThreadLocal<>();

    public static void add(SystemAuditModel model) {
        if (null != model) {
            List<SystemAuditModel> list = MODEL_LIST.get() != null ? MODEL_LIST.get() : new ArrayList<>();
            list.add(model);
            MODEL_LIST.set(list);
        }
    }

    public static List<SystemAuditModel> getAll() {
        return MODEL_LIST.get() != null ? MODEL_LIST.get() : new ArrayList<>();
    }

    public static void clear() {
        MODEL_LIST.remove();
    }
}
