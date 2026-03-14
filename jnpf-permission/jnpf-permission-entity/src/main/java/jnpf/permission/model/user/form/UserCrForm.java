package jnpf.permission.model.user.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class UserCrForm {

    @Schema(description = "账号")
    @NotNull(message = "账号不能为空")
    private String account;

    @Schema(description = "户名")
    @NotNull(message = "姓名不能为空")
    private String realName;

    @NotNull(message = "性别不能为空")
    @Schema(description = "性别")
    private String gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机")
    private String mobilePhone;

    @Schema(description = "职级")
    private String ranks;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "状态")
    private Integer enabledMark;

    @Schema(description = "说明")
    private String description;

    //个人资料
    @Schema(description = "头像")
    private String headIcon;

    @Schema(description = "民族")
    private String nation;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "证件类型")
    private String certificatesType;

    @Schema(description = "证件号码")
    private String certificatesNumber;

    @Schema(description = "入职日期")
    private Long entryDate;

    @Schema(description = "文化程度")
    private String education;

    @Schema(description = "生日")
    private String birthday;

    @Schema(description = "办公电话")
    private String telePhone;

    @Schema(description = "办公座机")
    private String landline;

    @Schema(description = "紧急联系人")
    private String urgentContacts;

    @Schema(description = "紧急电话")
    private String urgentTelePhone;

    @Schema(description = "通讯地址")
    private String postalAddress;

    //额外参数
    @Schema(description = "分组id")
    private String groupId;
}
