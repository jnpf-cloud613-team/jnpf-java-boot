package jnpf.permission.model.position;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.constant.MsgCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 约束模型
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/4/15 11:42:06
 */
@Data
@Schema(description = "约束模型")
@NoArgsConstructor
public class PosConModel {
    @Schema(description = "约束类型:0-互斥,1-基数,2-先决")
    private List<Integer> constraintType;
    @Schema(description = "互斥")
    private List<String> mutualExclusion;
    @Schema(description = "用户基数")
    private Integer userNum;
    @Schema(description = "权限基数")
    private Integer permissionNum;
    @Schema(description = "先决")
    private List<String> prerequisite;
    @JsonIgnore
    private boolean numFlag = false;
    @JsonIgnore
    private boolean mutualExclusionFlag = false;
    @JsonIgnore
    private boolean prerequisiteFlag = false;

    public void init() {
        if (constraintType != null && !constraintType.isEmpty()) {
            for (Integer type : constraintType) {
                if (Objects.equals(type, 0)) {
                    mutualExclusionFlag = true;
                }
                if (Objects.equals(type, 1)) {
                    numFlag = true;
                }
                if (Objects.equals(type, 2)) {
                    prerequisiteFlag = true;
                }
            }
        }
    }

    public String checkCondition(String id) {
        if (mutualExclusionFlag && mutualExclusion.contains(id)) {
            return MsgCode.SYS140.get();
        }
        if (prerequisiteFlag && prerequisite.contains(id)) {
            return MsgCode.SYS141.get();
        }
        if (mutualExclusionFlag && prerequisiteFlag) {
            List<String> collect = mutualExclusion.stream().filter(t -> prerequisite.contains(t)).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(collect)) {
                return MsgCode.SYS142.get();
            }
        }
        return "";
    }

    public boolean getNumFlag() {
        return numFlag;
    }

    public boolean getMutualExclusionFlag() {
        return mutualExclusionFlag;
    }

    public boolean getPrerequisiteFlag() {
        return prerequisiteFlag;
    }
}
