package jnpf.base.model.commonword;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jnpf.base.Pagination;
import jnpf.base.entity.CommonWordsEntity;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-07
 */
@Data
public class ComWordsPagination extends Pagination {

    @Schema(description = "状态")
    private Integer enabledMark;

    @Schema(description = "类型：0-系统，1-个人")
    @NotNull(message = "必填")
    private Integer commonWordsType;

    public Page<CommonWordsEntity> getPage(){
        return new Page<>(getCurrentPage(), getPageSize(), getTotal());
    }

}
