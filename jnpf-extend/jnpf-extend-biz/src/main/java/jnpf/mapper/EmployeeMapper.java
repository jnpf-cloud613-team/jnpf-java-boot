package jnpf.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.entity.EmployeeEntity;
import jnpf.model.employee.PaginationEmployee;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 职员信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface EmployeeMapper extends SuperMapper<EmployeeEntity> {

    default List<EmployeeEntity> getList() {
        QueryWrapper<EmployeeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByDesc(EmployeeEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<EmployeeEntity> getList(PaginationEmployee paginationEmployee) {
        QueryWrapper<EmployeeEntity> queryWrapper = new QueryWrapper<>();
        //查询条件
        String propertyName = paginationEmployee.getCondition() != null ? paginationEmployee.getCondition() : null;

        String propertyValue = paginationEmployee.getKeyword() != null ? paginationEmployee.getKeyword() : null;
        if (!StringUtils.isEmpty(propertyName) && !StringUtils.isEmpty(propertyValue)) {
            switch (propertyName) {
                //工号
                case "EnCode":
                    queryWrapper.lambda().like(EmployeeEntity::getEnCode, propertyValue);
                    break;
                //姓名
                case "FullName":
                    queryWrapper.lambda().like(EmployeeEntity::getFullName, propertyValue);
                    break;
                //电话
                case "Telephone":
                    queryWrapper.lambda().like(EmployeeEntity::getTelephone, propertyValue);
                    break;
                //部门
                case "DepartmentName":
                    queryWrapper.lambda().like(EmployeeEntity::getDepartmentName, propertyValue);
                    break;
                //岗位
                case "PositionName":
                    queryWrapper.lambda().like(EmployeeEntity::getPositionName, propertyValue);
                    break;
                default:
                    break;
            }
        }
        //排序
        if (StringUtils.isEmpty(paginationEmployee.getSidx())) {
            queryWrapper.lambda().orderByDesc(EmployeeEntity::getCreatorTime);
        } else {
            queryWrapper = "ASC".equalsIgnoreCase(paginationEmployee.getSort()) ? queryWrapper.orderByAsc(paginationEmployee.getSidx()) : queryWrapper.orderByDesc(paginationEmployee.getSidx());
        }
        Page<EmployeeEntity> page = new Page<>(paginationEmployee.getCurrentPage(), paginationEmployee.getPageSize());
        IPage<EmployeeEntity> userIPage = this.selectPage(page, queryWrapper);
        return paginationEmployee.setData(userIPage.getRecords(), page.getTotal());
    }

    default EmployeeEntity getInfo(String id) {
        QueryWrapper<EmployeeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EmployeeEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void delete(EmployeeEntity entity) {
        this.deleteById(entity.getId());
    }

    default void create(EmployeeEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setSortCode(RandomUtil.parses());
        entity.setCreatorUserId(UserProvider.getLoginUserId());
        this.insert(entity);
    }

    default void update(String id, EmployeeEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getLoginUserId());
        this.updateById(entity);
    }

}
