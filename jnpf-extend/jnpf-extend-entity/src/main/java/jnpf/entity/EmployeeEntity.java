package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.util.Date;

/**
 * 职员信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 *
 */
@Data
@TableName("ext_employee")
public class EmployeeEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 工号
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 姓名
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 性别
     */
    @TableField("F_GENDER")
    private String gender;

    /**
     * 部门
     */
    @TableField("F_DEPARTMENT_NAME")
    private String departmentName;

    /**
     * 岗位
     */
    @TableField("F_POSITION_NAME")
    private String positionName;

    /**
     * 用工性质
     */
    @TableField("F_WORKING_NATURE")
    private String workingNature;

    /**
     * 身份证号
     */
    @TableField("F_ID_NUMBER")
    private String idNumber;

    /**
     * 联系电话
     */
    @TableField("F_TELEPHONE")
    private String telephone;

    /**
     * 参加工作
     */
    @TableField("F_ATTEND_WORK_TIME")
    private Date attendWorkTime;

    /**
     * 出生年月
     */
    @TableField("F_BIRTHDAY")
    private Date birthday;

    /**
     * 最高学历
     */
    @TableField("F_EDUCATION")
    private String education;

    /**
     * 所学专业
     */
    @TableField("F_MAJOR")
    private String major;

    /**
     * 毕业院校
     */
    @TableField("F_GRADUATION_ACADEMY")
    private String graduationAcademy;

    /**
     * 毕业时间
     */
    @TableField("F_GRADUATION_TIME")
    private Date graduationTime;

}
