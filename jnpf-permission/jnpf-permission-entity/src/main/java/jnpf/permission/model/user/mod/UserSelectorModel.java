package jnpf.permission.model.user.mod;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.util.treeutil.SumTree;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class UserSelectorModel extends SumTree<UserSelectorModel> {
    @JSONField(name="category")
    private String type;
    private String fullName;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "图标")
    private String icon;
}
