package jnpf.flowable.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.entity.TemplateUseNumEntity;
import jnpf.flowable.model.template.TemplateUseNumVo;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface TemplateUseNumMapper extends SuperMapper<TemplateUseNumEntity> {

    default boolean insertOrUpdateUseNum(String templateId) {
        UserInfo user = UserProvider.getUser();
        LambdaQueryWrapper<TemplateUseNumEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TemplateUseNumEntity::getTemplateId, templateId);
        queryWrapper.eq(TemplateUseNumEntity::getUserId, user.getUserId());
        TemplateUseNumEntity userNumEntity = this.selectOne(queryWrapper);
        if (userNumEntity == null) {
            userNumEntity = new TemplateUseNumEntity();
            userNumEntity.setUserId(user.getUserId());
            userNumEntity.setTemplateId(templateId);
            userNumEntity.setUseNum(1);
            userNumEntity.setLastModifyTime(new Date());
            return this.insert(userNumEntity) > 0;
        }
        userNumEntity.setUseNum(userNumEntity.getUseNum() + 1);
        userNumEntity.setLastModifyTime(null);
        return this.updateById(userNumEntity) > 0;
    }

    default void deleteUseNum(String templateId, String userId) {
        if (StringUtil.isNotEmpty(templateId)) {
            LambdaQueryWrapper<TemplateUseNumEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TemplateUseNumEntity::getTemplateId, templateId);
            if (StringUtil.isNotEmpty(userId)) {
                queryWrapper.eq(TemplateUseNumEntity::getUserId, userId);
            }
            this.delete(queryWrapper);
        }
    }

    default List<TemplateUseNumVo> getMenuUseNum(int i, List<String> authFlowList, String systemId) {
        Pagination pagination = new Pagination();
        pagination.setPageSize(12);
        UserInfo user = UserProvider.getUser();
        MPJLambdaWrapper<TemplateUseNumEntity> wrapper = JoinWrappers.lambda(TemplateUseNumEntity.class)
                .select(TemplateEntity::getId, TemplateEntity::getFullName, TemplateEntity::getEnCode,
                        TemplateEntity::getType, TemplateEntity::getIcon, TemplateEntity::getIconBackground,
                        TemplateEntity::getSystemId
                )
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TemplateUseNumEntity::getTemplateId)
                .eq(TemplateUseNumEntity::getUserId, user.getUserId())
                .eq(TemplateEntity::getEnabledMark, 1);
        if (i == 0) {
            wrapper.orderByDesc(TemplateUseNumEntity::getUseNum);
        } else if (i == 1) {
            wrapper.orderByDesc(TemplateUseNumEntity::getLastModifyTime);
        }
        if (StringUtil.isNotEmpty(systemId)) {
            wrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        //流程有权限（包含通用）
        wrapper.and(t -> t.eq(TemplateEntity::getVisibleType, FlowNature.ALL)
                .or().in(!authFlowList.isEmpty(), TemplateEntity::getId, authFlowList)
        );


        Page<TemplateUseNumVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<TemplateUseNumVo> iPage = this.selectJoinPage(page, TemplateUseNumVo.class, wrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }
}
