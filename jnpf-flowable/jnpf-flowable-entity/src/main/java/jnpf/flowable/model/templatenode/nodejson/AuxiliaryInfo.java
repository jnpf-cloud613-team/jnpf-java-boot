package jnpf.flowable.model.templatenode.nodejson;

import lombok.Data;

/**
 * 辅助信息
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/4 14:21
 */
@Data
public class AuxiliaryInfo {
    /**
     * 3、附件信息
     */
    private Integer id;
    private String fullName;
    private AuxiliaryInfoConfig config = new AuxiliaryInfoConfig();
}
