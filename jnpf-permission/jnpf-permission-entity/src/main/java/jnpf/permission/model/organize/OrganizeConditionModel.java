package jnpf.permission.model.organize;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/8 14:05
 */
@Data
public class OrganizeConditionModel extends Page implements Serializable {

    @Schema(description = "部门id集合")
    private List<String> departIds;

    private Map<String, String> orgIdNameMaps;

}
