

package jnpf.message.model.accountconfig;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;
import com.alibaba.fastjson.annotation.JSONField;
/**
 *
 * 
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
public class AccountConfigListVO{

        @Schema(description = "id")
        private String id;

        /** 名称 **/
        @Schema(description = "名称")
        @JSONField(name = "fullName")
        private String fullName;

        /** 配置类型 **/
        @Schema(description = "配置类型")
        @JSONField(name = "type")
        private String type;

        /** 编码 **/
        @Schema(description = "编码")
        @JSONField(name = "enCode")
        private String enCode;

        /** 发件人昵称 **/
        @Schema(description = "发件人昵称")
        @JSONField(name = "addressorName")
        private String addressorName;

        /** SMTP用户名 **/
        @Schema(description = "SMTP用户名")
        @JSONField(name = "smtpUser")
        private String smtpUser;

        /** 渠道 **/
        @Schema(description = "渠道")
        @JSONField(name = "channel")
        private String channel;

        /** 短信签名 **/
        @Schema(description = "短信签名")
        @JSONField(name = "smsSignature")
        private String smsSignature;


        /** WebHook类型 **/
        @Schema(description = "WebHook类型")
        @JSONField(name = "webhookType")
        private String webhookType;

        /** 排序 **/
        @Schema(description = "排序")
        @JSONField(name = "sortCode")
        private Integer sortCode;
        /** 状态 **/
        @Schema(description = "状态")
        @JSONField(name = "enabledMark")
        private String enabledMark;


        /** 创建时间 **/
        @Schema(description = "创建时间")
        @JSONField(name = "creatorTime")
        private Date creatorTime;

        /** 创建用户 **/
        @Schema(description = "创建用户")
        @JSONField(name = "creatorUserId")
        private String creatorUserId;

        /** 修改时间 **/
        @Schema(description = "修改时间")
        @JSONField(name = "lastModifyTime")
        private Date lastModifyTime;

        @Schema(description = "创建人")
        private String creatorUser;

}
