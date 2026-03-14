package jnpf.base.model.online;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 可视化菜单对象
 *
 * @author JNPF开发平台组
 * @version V3.4
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/4/6
 */
@Data
public class VisualMenuModel {
    /**
     * 功能id
     */
    private String id;

    /**
     * pc 按钮配置
     */
    private PerColModels pcPerCols;

    /**
     * app 按钮配置
     */
    private PerColModels appPerCols;

    /**
     * 功能名
     */
    private String fullName;

    /**
     * 功能编码
     */
    private String enCode;

    private Integer pc;

    private Integer app;

    private List<String> pcModuleParentId;

    private List<String> appModuleParentId;

    private String pcSystemId;

    private String appSystemId;

    private Integer type;

    /**
     * 参考 visualdevEntity
     * 页面类型（1、纯表单，2、表单加列表，4、数据视图）
     */
    private Integer webType;
    /**
     * 按钮权限
     */
    private List<Integer> pcAuth = new ArrayList<>();

    /**
     * 列表权限
     */
    private List<Integer> appAuth = new ArrayList<>();


    @Schema(description = "web页面地址")
    private String webAddress;

    @Schema(description = "app页面地址")
    private String appAddress;
}
