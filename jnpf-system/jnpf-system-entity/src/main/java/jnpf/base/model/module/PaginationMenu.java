package jnpf.base.model.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Page;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class PaginationMenu extends Page {
   @Schema(description = "分类")
   private String category;
   @Schema(description = "状态")
   private Integer enabledMark;
   @Schema(description = "类型")
   private Integer type;
}
