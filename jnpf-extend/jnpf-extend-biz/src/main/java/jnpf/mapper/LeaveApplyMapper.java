package jnpf.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.LeaveApplyEntity;
import jnpf.model.leaveapply.LeaveApplyForm;
import jnpf.util.JsonUtil;

/**
 * 流程表单【请假申请】
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月29日 上午9:18
 */
public interface LeaveApplyMapper extends SuperMapper<LeaveApplyEntity> {

    default LeaveApplyEntity getInfo(String id) {
        QueryWrapper<LeaveApplyEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LeaveApplyEntity::getId, id);
        return selectOne(queryWrapper);
    }


    default void data(String id, String data) {
        LeaveApplyForm leaveApplyForm = JsonUtil.getJsonToBean(data, LeaveApplyForm.class);
        LeaveApplyEntity entity = JsonUtil.getJsonToBean(leaveApplyForm, LeaveApplyEntity.class);
        entity.setId(id);
        insertOrUpdate(entity);
    }

    default void delete(LeaveApplyEntity entity) {
        this.deleteById(entity);
    }
}
