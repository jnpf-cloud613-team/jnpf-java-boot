package jnpf.permission.model.user.mod;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 导入模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-20
 */
@Data
public class UserImportModel implements Serializable {
    private String account;

    private String realName;

    private String organizeId;

    private String managerId;

    private String positionId;

    private String roleId;

    private String description;

    private String gender;

    private String nation;

    private String nativePlace;

    private String certificatesType;

    private String certificatesNumber;

    private String education;

    private Date birthday;

    private String telePhone;

    private String landline;

    private String mobilePhone;

    private String email;

    private String urgentContacts;

    private String urgentTelePhone;

    private String postalAddress;

    private Long sortCode;

    private Date entryDate;

    private Integer enabledMark;
    private String ranks;
}
