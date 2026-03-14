package jnpf.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.DataSetEntity;
import jnpf.base.entity.PrintVersionEntity;
import jnpf.base.mapper.DataSetMapper;
import jnpf.base.mapper.PrintVersionMapper;
import jnpf.base.model.dataset.DataSetForm;
import jnpf.base.model.dataset.DataSetPagination;
import jnpf.base.model.print.PrintDevFormDTO;
import jnpf.base.service.PrintVersionService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.emnus.DataSetTypeEnum;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/6 14:07:11
 */
@Service
@RequiredArgsConstructor
public class PrintVersionServiceImpl extends SuperServiceImpl<PrintVersionMapper, PrintVersionEntity> implements PrintVersionService {


    private final DataSetMapper dataSetMapper;

    @Override
    @DSTransactional
    public void create(PrintDevFormDTO dto) {
        PrintVersionEntity entity = JsonUtil.getJsonToBean(dto, PrintVersionEntity.class);
        entity.setTemplateId(dto.getId());
        this.baseMapper.create(entity);
        //数据集创建
        List<DataSetForm> dataSetList = dto.getDataSetList() != null ? dto.getDataSetList() : new ArrayList<>();
        dataSetMapper.create(dataSetList, DataSetTypeEnum.PRINT_VER.getCode(), entity.getId());
    }

    @Override
    public List<PrintVersionEntity> getList(String templateId) {
        return this.baseMapper.getList(templateId);
    }

    @Override
    public String copyVersion(String versionId) {
        PrintVersionEntity entity = this.getById(versionId);
        PrintVersionEntity versionEntity = BeanUtil.copyProperties(entity, PrintVersionEntity.class);
        String newVersionId = RandomUtil.uuId();
        versionEntity.setId(newVersionId);
        List<PrintVersionEntity> verList = getList(entity.getTemplateId());
        int version = verList.stream().map(PrintVersionEntity::getVersion).max(Comparator.naturalOrder()).orElse(0) + 1;
        versionEntity.setVersion(version);
        versionEntity.setState(0);
        versionEntity.setSortCode(0l);
        versionEntity.setCreatorTime(new Date());
        versionEntity.setCreatorUserId(UserProvider.getUser().getUserId());
        versionEntity.setLastModifyTime(null);
        versionEntity.setLastModifyUserId(null);
        List<DataSetEntity> dataSetList = dataSetMapper.getList(new DataSetPagination(DataSetTypeEnum.PRINT_VER.getCode(), versionId));
        for (DataSetEntity item : dataSetList) {
            item.setId(RandomUtil.uuId());
            item.setObjectType(DataSetTypeEnum.PRINT_VER.getCode());
            item.setObjectId(newVersionId);
            item.setCreatorTime(new Date());
            item.setCreatorUserId(UserProvider.getUser().getUserId());
            item.setLastModifyTime(null);
            item.setLastModifyUserId(null);
            dataSetMapper.insert(item);
        }
        this.save(versionEntity);
        return newVersionId;
    }

    @Override
    public void removeByTemplateId(String templateId) {
        List<PrintVersionEntity> list = this.getList(templateId);
        for (PrintVersionEntity item : list) {
            QueryWrapper<DataSetEntity> dataSetWrapper = new QueryWrapper<>();
            dataSetWrapper.lambda().eq(DataSetEntity::getObjectType, DataSetTypeEnum.PRINT_VER.getCode());
            dataSetWrapper.lambda().eq(DataSetEntity::getObjectId, templateId);
            dataSetMapper.deleteByIds(dataSetMapper.selectList(dataSetWrapper));
            this.removeById(item);
        }
    }
}
