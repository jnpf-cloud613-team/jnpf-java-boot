package jnpf.model.employee;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class EmployeeCrForm {
    @NotBlank(message = "必填")
    @Schema(description ="姓名")
    private String fullName;
    @NotBlank(message = "必填")
    @Schema(description ="工号")
    private String enCode;
    @NotBlank(message = "必填")
    @Schema(description ="性别")
    private String gender;
    @NotBlank(message = "必填")
    @Schema(description ="部门")
    private String departmentName;
    @NotBlank(message = "必填")
    @Schema(description ="岗位")
    private String positionName;
    @Schema(description ="用工性质")
    private String workingNature;
    @NotBlank(message = "必填")
    @Schema(description ="身份证号")
    private String idNumber;
    @NotBlank(message = "必填")
    @Schema(description ="联系电话")
    private String telephone;
    @Schema(description ="参加工作")
    private long attendWorkTime;
    @Schema(description ="出生年月")
    private long birthday;
    @Schema(description ="最高学历")
    private String education;
    @Schema(description ="所学专业")
    private String major;
    @Schema(description ="毕业院校")
    private String graduationAcademy;
    @Schema(description ="毕业时间")
    private long graduationTime;
}
