package jnpf.base.model.dataset;

import jnpf.database.model.query.SuperQueryJsonModel;
import lombok.Data;

import java.util.List;

/**
 * 连接属性
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/9 17:22:53
 */
@Data
public class DsRelationConfig {

    /**
     * 关联关系：1-左连接，2-右连接，3-内连接，4-全连接
     */
    private Integer type;
    /**
     * 关联字段列表
     */
    private List<DsRelationModel> relationList;
    /**
     * 条件筛选：且或
     */
    private String matchLogic;
    /**
     * 条件组
     */
    private List<SuperQueryJsonModel> ruleList;
}
