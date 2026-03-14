package jnpf.base.service;

import jnpf.base.entity.BillNumEntity;

/**
 * 单据递增序号
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/4 9:26:33
 */
public interface BillNumService extends SuperService<BillNumEntity> {

    void saveBillNum(BillNumEntity entity);

    BillNumEntity getBillNum(String ruleId, String visualId, String flowId);

    void removeByRuleId(String ruleId, String visualId, String flowId);
}
