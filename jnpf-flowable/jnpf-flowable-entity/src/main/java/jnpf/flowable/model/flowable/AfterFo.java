package jnpf.flowable.model.flowable;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/8 20:12
 */
@Data
public class AfterFo {
    /**
     * 部署ID
     */
    private String deploymentId;
    /**
     * 节点Key
     */
    private List<String> taskKeys = new ArrayList<>();
}
