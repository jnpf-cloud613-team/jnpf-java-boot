package jnpf.model;


import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReportManageModel {
    @Schema(description ="主键")
    @JSONField(name = "F_Id")
    private String id;
    @Schema(description ="名称")
    @JSONField(name = "F_FullName")
    private String fullName;
    @Schema(description ="分类")
    @JSONField(name = "F_Category")
    private String category;
    @Schema(description ="地址")
    @JSONField(name = "F_UrlAddress")
    private String urlAddress;
}
