package jnpf.onlinedev.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.visualjson.analysis.FormAllModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnlineInfoModel {
    /**
     * 是否需要关联表单字段
     */
    private boolean needRlationFiled = false;
    /**
     * 是否转换
     */
    private boolean needSwap = false;
    /**
     * 存储字段
     */
    private String propsValue;

    @Schema(description = "解析后字段")
    private List<FormAllModel> formAllModel;

    @Schema(description = "菜单id")
    private String menuId;
}
