package jnpf.base.model.dbsync;

import jnpf.database.model.dbtable.DbTableFieldModel;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.3
 * @copyright 引迈信息技术有限公司
 * @date 2022-06-01
 */
@Data
public class DbSyncVo {

    /**
     * 验证结果
     */
    private Boolean checkDbFlag;

    /**
     * 表集合
     */
    private List<DbTableFieldModel> tableList;

    /**
     * 转换规则
     */
    private Map<String, List<String>> convertRuleMap;

}
