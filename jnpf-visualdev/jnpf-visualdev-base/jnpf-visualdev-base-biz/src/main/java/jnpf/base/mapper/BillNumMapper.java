package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.BillNumEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.List;

/**
 * 单据递增序号
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/4 9:26:52
 */
public interface BillNumMapper extends SuperMapper<BillNumEntity> {

    default void saveBillNum(BillNumEntity entity) {
        if (entity.getId() == null) {
            entity.setId(RandomUtil.uuId());
        }
        this.insertOrUpdate(entity);
    }

    default BillNumEntity getBillNum(String ruleId, String visualId, String flowId) {
        QueryWrapper<BillNumEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillNumEntity::getRuleId, ruleId);
        queryWrapper.lambda().eq(BillNumEntity::getVisualId, visualId);
        if (StringUtil.isNotEmpty(flowId)) {
            queryWrapper.lambda().eq(BillNumEntity::getFlowId, flowId);
        } else {
            queryWrapper.lambda().isNull(BillNumEntity::getFlowId);
        }
        List<BillNumEntity> list = this.selectList(queryWrapper);
        if (!list.isEmpty()) {
            return list.stream().findFirst().orElse(null);
        }
        return null;
    }

    default void removeByRuleId(String ruleId, String visualId, String flowId) {
        QueryWrapper<BillNumEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BillNumEntity::getRuleId, ruleId);
        queryWrapper.lambda().eq(BillNumEntity::getVisualId, visualId);
        if (StringUtil.isNotEmpty(flowId)) {
            queryWrapper.lambda().eq(BillNumEntity::getFlowId, flowId);
        } else {
            queryWrapper.lambda().isNull(BillNumEntity::getFlowId);
        }
        this.deleteByIds(this.selectList(queryWrapper));
    }
}
