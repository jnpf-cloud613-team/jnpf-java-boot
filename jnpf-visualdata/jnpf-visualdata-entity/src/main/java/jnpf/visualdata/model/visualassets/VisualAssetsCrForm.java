package jnpf.visualdata.model.visualassets;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;


/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Data
public class VisualAssetsCrForm {


    @Schema(description = "主键")
    private String id;

    @Schema(description = "资源名称")
    private String assetsName;

    @Schema(description = "资源大小")
    private String assetsSize;

    @Schema(description = "资源上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date assetsTime;

    @Schema(description = "资源后缀名")
    private String assetsType;

    @Schema(description = "资源地址")
    private String assetsUrl;
}
