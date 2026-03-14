package jnpf.base.model.flow;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.TransferModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "自定义按钮发起审批参数对象")
public class FlowLaunchModel {
    @Schema(description = "流程大id")
    private String template;

    @Schema(description = "按钮编码")
    private String btnCode;

    @Schema(description = "是否当前用户")
    private Integer currentUser;

    @Schema(description = "是否自定义用户")
    private Integer customUser;

    @Schema(description = "是否校验发起杈限")
    private Boolean hasPermission = false;

    @Schema(description = "自定义列表")
    private List<String> initiator;

    @Schema(description = "数据列表")
    private List<List<TransferModel>> dataList;
}
