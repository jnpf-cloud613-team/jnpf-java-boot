package jnpf.flowable.model.outside;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/16 10:07
 */
@Data
public class OutsideModel {
    private String eventId;
    private Map<String, Object> formData = new HashMap<>();
}
