

package jnpf.message.model.messagedatatype;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
public class MessageDataTypeListVO {

    @Schema(description = "id")
    private String id;

    /**
     * 数据类型
     **/
    @Schema(description = "数据类型")
    @JsonProperty("type")
    private String type;

    /**
     * 数据名称
     **/
    @Schema(description = "数据名称")
    @JsonProperty("fullName")
    private String fullName;

    /**
     * 数据编码（为防止与系统后续更新的功能的数据编码冲突，客户自定义添加的功能编码请以ZDY开头。例如：ZDY1）
     **/
    @Schema(description = "数据编码")
    @JsonProperty("enCode")
    private String enCode;

    /**
     * 创建时间
     **/
    @Schema(description = "创建时间")
    @JsonProperty("creatortime")
    private Date creatortime;

    /**
     * 创建人员
     **/
    @Schema(description = "创建人员")
    @JsonProperty("creatorUserId")
    private String creatoruserid;

    /**
     * 修改时间
     **/
    @Schema(description = "修改时间")
    @JsonProperty("lastModifyTime")
    private Date lastmodifytime;

    /**
     * 修改人员
     **/
    @Schema(description = "修改人员")
    @JsonProperty("lastModifyUserId")
    private String lastmodifyuserid;

}