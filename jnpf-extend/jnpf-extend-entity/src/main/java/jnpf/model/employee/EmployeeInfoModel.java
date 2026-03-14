package jnpf.model.employee;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 *
 */
@Data
public class EmployeeInfoModel {
    @Schema(description ="出生年月")
    private Date birthday;
    @Schema(description ="参加工作")
    private String attendWorkTime;
    @Schema(description ="创建时间")
    private Date creatorTime;
    @Schema(description ="创建用户")
    private String creatorUserId;
    @Schema(description ="删除标志")
    private String deleteMark;
    @Schema(description ="删除时间")
    private Date deleteTime;
    @Schema(description ="删除用户")
    private String deleteUserId;
    @Schema(description ="部门")
    private String departmentName;
    @Schema(description ="描述")
    private String description;
    @Schema(description ="最高学历")
    private String education;
    @Schema(description ="工号")
    private String enCode;
    @Schema(description ="有效标志")
    private Integer enabledMark;
    @Schema(description ="姓名")
    private String fullName;
    @Schema(description ="性别")
    private String gender;
    @Schema(description ="毕业院校")
    private String graduationAcademy  ;
    @Schema(description ="毕业时间")
    private Date graduationTime;
    @Schema(description ="自然主键")
    private String id;
    @Schema(description ="身份证号")
    private String idNumber;
    @Schema(description ="修改时间")
    private Date lastModifyTime;
    @Schema(description ="修改用户")
    private String lastModifyUserId;
    @Schema(description ="所学专业")
    private String major;
    @Schema(description ="岗位")
    private String positionName;
    @Schema(description ="排序")
    private String sortCode;
    @Schema(description ="联系电话")
    private String telephone;
    @Schema(description ="用工性质")
    private String workingNature;
}
