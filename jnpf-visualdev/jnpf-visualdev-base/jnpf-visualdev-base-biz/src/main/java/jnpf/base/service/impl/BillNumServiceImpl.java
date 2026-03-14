package jnpf.base.service.impl;


import jnpf.base.entity.BillNumEntity;
import jnpf.base.mapper.BillNumMapper;
import jnpf.base.service.BillNumService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 单据递增序号
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/4 9:28:31
 */
@Service
public class BillNumServiceImpl extends SuperServiceImpl<BillNumMapper, BillNumEntity> implements BillNumService {

    public void saveBillNum(BillNumEntity entity) {
        this.baseMapper.saveBillNum(entity);
    }

    public BillNumEntity getBillNum(String ruleId, String visualId, String flowId) {
        return this.baseMapper.getBillNum(ruleId, visualId, flowId);
    }

    @Override
    public void removeByRuleId(String ruleId, String visualId, String flowId) {
        this.baseMapper.removeByRuleId(ruleId, visualId, flowId);
    }
}
