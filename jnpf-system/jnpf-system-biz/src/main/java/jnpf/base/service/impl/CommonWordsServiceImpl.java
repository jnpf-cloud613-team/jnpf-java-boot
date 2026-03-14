package jnpf.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.entity.CommonWordsEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.mapper.CommonWordsMapper;
import jnpf.base.mapper.SystemMapper;
import jnpf.base.model.commonword.ComWordsPagination;
import jnpf.base.service.CommonWordsService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 审批常用语 ServiceImpl
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-06
 */
@Service
@RequiredArgsConstructor
public class CommonWordsServiceImpl extends SuperServiceImpl<CommonWordsMapper, CommonWordsEntity> implements CommonWordsService {


    private final SystemMapper systemMapper;

    @Override
    public List<CommonWordsEntity> getSysList(ComWordsPagination comWordsPagination, Boolean currentSysFlag) {
        QueryWrapper<SystemEntity> sysQuery = new QueryWrapper<>();
        // 匹配
        QueryWrapper<CommonWordsEntity> query = new QueryWrapper<>();
        if (Objects.nonNull(comWordsPagination.getCommonWordsType())) {
            query.lambda().eq(CommonWordsEntity::getCommonWordsType, comWordsPagination.getCommonWordsType());
            if (Objects.equals(comWordsPagination.getCommonWordsType(), 1)) {
                query.lambda().eq(CommonWordsEntity::getCreatorUserId, UserProvider.getUser().getUserId());
            }
        }

        if (StringUtil.isNotEmpty(comWordsPagination.getKeyword())) {
            sysQuery.lambda().like(SystemEntity::getFullName, comWordsPagination.getKeyword());
            List<String> ids = systemMapper.selectList(sysQuery).stream().map(SystemEntity::getId).collect(Collectors.toList());
            query.lambda().and(t -> {
                // 应用名称
                for (String id : ids) {
                    t.like(CommonWordsEntity::getSystemIds, id).or();
                }
                t.like(CommonWordsEntity::getCommonWordsText, comWordsPagination.getKeyword()); // 常用语
            });
        }
        if (comWordsPagination.getEnabledMark() != null) {
            query.lambda().eq(CommonWordsEntity::getEnabledMark, comWordsPagination.getEnabledMark());
        }
        // 排序
        if (Objects.equals(comWordsPagination.getCommonWordsType(), 1)) {
            query.lambda().orderByDesc(CommonWordsEntity::getUsesNum);
        }
        query.lambda().orderByAsc(CommonWordsEntity::getSortCode).orderByDesc(CommonWordsEntity::getCreatorTime);
        Page<CommonWordsEntity> page = this.page(comWordsPagination.getPage(), query);
        comWordsPagination.setTotal(page.getTotal());
        return page.getRecords();
    }

    @Override
    public List<CommonWordsEntity> getListModel(String type) {
        return this.baseMapper.getListModel(type);
    }

    @Override
    public Boolean existSystem(String systemId) {
        return this.baseMapper.existSystem(systemId);
    }

    @Override
    public Boolean existCommonWord(String id, String commonWordsText, Integer commonWordsType) {
        return this.baseMapper.existCommonWord(id, commonWordsText, commonWordsType);
    }

    @Override
    public void addCommonWordsNum(String commonWordsText) {
        this.baseMapper.addCommonWordsNum(commonWordsText);
    }

}
