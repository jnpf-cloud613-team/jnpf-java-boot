package jnpf.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.service.BillRuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.LeaveApplyEntity;
import jnpf.mapper.LeaveApplyMapper;
import jnpf.model.leaveapply.LeaveApplyForm;
import jnpf.service.LeaveApplyService;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 流程表单【请假申请】
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月29日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class LeaveApplyServiceImpl extends SuperServiceImpl<LeaveApplyMapper, LeaveApplyEntity> implements LeaveApplyService {


    private final BillRuleService billRuleService;



    @Override
    public LeaveApplyEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public void save(String id, LeaveApplyEntity entity, LeaveApplyForm form) {
        //表单信息
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(id);
            save(entity);
            billRuleService.useBillNumber("WF_LeaveApplyNo");
        } else {
            entity.setId(id);
            updateById(entity);
        }
    }

    @Override
    @DSTransactional
    public void submit(String id, LeaveApplyEntity entity, LeaveApplyForm form) {
        //表单信息
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(id);
            save(entity);
            billRuleService.useBillNumber("WF_LeaveApplyNo");
        } else {
            entity.setId(id);
            updateById(entity);
        }
    }

    @Override
    public void data(String id, String data) {
        this.baseMapper.data(id, data);
    }

    @Override
    public void delete(LeaveApplyEntity entity) {
        this.baseMapper.delete(entity);
    }

}
