package jnpf.flowable.model.templatenode;

import lombok.Data;

import java.io.Serializable;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/5 18:01
 */
@Data
public class FlowErrorModel implements Serializable {
    private String nodeCode;
    private String nodeName;
}
