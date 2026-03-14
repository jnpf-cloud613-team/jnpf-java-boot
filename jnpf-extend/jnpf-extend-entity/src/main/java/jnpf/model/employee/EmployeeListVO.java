package jnpf.model.employee;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmployeeListVO {
    @Schema(description ="主鍵")
    private String id;
    @Schema(description ="工号")
    private String enCode;
    @Schema(description ="姓名")
    private String fullName;
    @Schema(description ="性别ID")
    private String gender;
    @Schema(description ="部门")
    private String departmentName;
    @Schema(description ="岗位")
    private String positionName;
    @Schema(description ="用工性质")
    private String workingNature;
    @Schema(description ="身份证")
    private String idNumber;
    @Schema(description ="联系电话")
    private String telephone;
    @Schema(description ="生日")
    private long birthday;
    @Schema(description ="参加工作时间")
    private long attendWorkTime;
    @Schema(description ="学历")
    private String education;
    @Schema(description ="所学专业")
    private String major;
    @Schema(description ="毕业院校")
    private String graduationAcademy;
    @Schema(description ="毕业时间")
    private long graduationTime;
    @Schema(description ="创建时间")
    private long creatorTime;

}
