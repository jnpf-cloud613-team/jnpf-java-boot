package jnpf.base.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.entity.PrintDevEntity;
import jnpf.base.model.print.PaginationPrint;
import jnpf.base.model.print.PrintOption;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 打印模板-mapper
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
public interface PrintDevMapper extends SuperMapper<PrintDevEntity> {

    default List<PrintDevEntity> getList(PaginationPrint paginationPrint) {
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(paginationPrint.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(PrintDevEntity::getFullName, paginationPrint.getKeyword())
                            .or().like(PrintDevEntity::getEnCode, paginationPrint.getKeyword())
            );
        }
        if (StringUtil.isNotEmpty(paginationPrint.getCategory())) {
            queryWrapper.lambda().eq(PrintDevEntity::getCategory, paginationPrint.getCategory());
        }
        if (paginationPrint.getState() != null) {
            queryWrapper.lambda().eq(PrintDevEntity::getState, paginationPrint.getState());
        }
        if (StringUtil.isNotEmpty(paginationPrint.getSystemId())) {
            queryWrapper.lambda().eq(PrintDevEntity::getSystemId, paginationPrint.getSystemId());
        }
        queryWrapper.lambda().orderByAsc(PrintDevEntity::getSortCode).orderByDesc(PrintDevEntity::getCreatorTime);
        Page<PrintDevEntity> page = new Page<>(paginationPrint.getCurrentPage(), paginationPrint.getPageSize());
        IPage<PrintDevEntity> iPage = this.selectPage(page, queryWrapper);
        return paginationPrint.setData(iPage.getRecords(), page.getTotal());
    }

    default List<PrintDevEntity> getListByIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(idList, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().in(PrintDevEntity::getId, list).or();
        }
        return this.selectList(queryWrapper);
    }

    default List<PrintDevEntity> getListByCreUser(String creUser) {
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintDevEntity::getVisibleType, 2);
        queryWrapper.lambda().eq(PrintDevEntity::getCreatorUserId, creUser);
        return this.selectList(queryWrapper);
    }

    default void create(PrintDevEntity entity) {
        String id = StringUtil.isNotEmpty(entity.getId()) ? entity.getId() : RandomUtil.uuId();
        entity.setId(id);
        entity.setState(0);
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(new Date());
        entity.setLastModifyUserId(null);
        entity.setLastModifyTime(null);
        this.insert(entity);
    }

    default void creUpdateCheck(PrintDevEntity printDevEntity, Boolean fullNameCheck, Boolean encodeCheck) {
        String fullName = printDevEntity.getFullName();
        String encode = printDevEntity.getEnCode();
        // 名称长度验证
        if (fullName.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }
        QueryWrapper<PrintDevEntity> query = new QueryWrapper<>();
        //重名验证
        if (Boolean.TRUE.equals(fullNameCheck)) {
            query.lambda().eq(PrintDevEntity::getFullName, fullName);
            query.lambda().eq(PrintDevEntity::getSystemId, printDevEntity.getSystemId());
            if (!this.selectList(query).isEmpty()) {
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
        //编码验证
        if (Boolean.TRUE.equals(encodeCheck)) {
            query.clear();
            query.lambda().eq(PrintDevEntity::getEnCode, encode);
            if (!this.selectList(query).isEmpty()) {
                throw new DataException(MsgCode.EXIST002.get());
            }
        }
    }

    default Boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintDevEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(PrintDevEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<PrintOption> getPrintTemplateOptions(List<String> ids) {
        QueryWrapper<PrintDevEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(PrintDevEntity::getId, ids);
        List<PrintDevEntity> list = this.selectList(wrapper);
        return JsonUtil.getJsonToList(list, PrintOption.class);
    }

    default List<PrintDevEntity> getWorkSelector(List<String> id) {
        List<PrintDevEntity> list = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(id)) {
            QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(PrintDevEntity::getId, id);
            queryWrapper.lambda().orderByAsc(PrintDevEntity::getSortCode).orderByDesc(PrintDevEntity::getCreatorTime);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default List<PrintDevEntity> getListBySystemId(String systemId) {
        QueryWrapper<PrintDevEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrintDevEntity::getSystemId, systemId);
        return this.selectList(queryWrapper);
    }
}
