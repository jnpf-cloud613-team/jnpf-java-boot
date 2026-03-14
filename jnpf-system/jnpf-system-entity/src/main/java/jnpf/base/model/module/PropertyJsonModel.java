package jnpf.base.model.module;

import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class PropertyJsonModel {
    private String iconBackgroundColor;
    private String moduleId;
    private Integer isTree;
    /**
     * 参考 visualdevEntity
     * 页面类型（1、纯表单，2、表单加列表，4、数据视图）
     */
    private Integer webType;
}
