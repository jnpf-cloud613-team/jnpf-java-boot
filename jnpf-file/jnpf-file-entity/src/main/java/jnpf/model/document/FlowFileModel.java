package jnpf.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/4 16:29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowFileModel {
    private String userId;
    private String templateId;
    /**
     * 数据读取范围，近七天、近一个月、近半年、近一年、全部
     */
    private Integer dataRange = 0;
}
