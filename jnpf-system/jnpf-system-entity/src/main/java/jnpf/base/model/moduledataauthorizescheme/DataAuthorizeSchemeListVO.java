package jnpf.base.model.moduledataauthorizescheme;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF����ƽ̨��
 * @version V3.1.0
 * @copyright ������Ϣ�������޹�˾
 * @date 2021/3/12 15:31
 */
@Data
public class DataAuthorizeSchemeListVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "条件文本")
    private String conditionText;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "是否全部数据")
    private Integer allData;

    @Schema(description = "分组匹配逻辑")
    private String matchLogic;
}
