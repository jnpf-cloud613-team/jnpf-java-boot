package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.TableExampleEntity;
import jnpf.mapper.TableExampleMapper;
import jnpf.model.tableexample.PaginationTableExample;
import jnpf.service.TableExampleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 表格示例数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Service
public class TableExampleServiceImpl extends SuperServiceImpl<TableExampleMapper, TableExampleEntity> implements TableExampleService {


    @Override
    public List<TableExampleEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<TableExampleEntity> getList(String keyword) {
        return this.baseMapper.getList(keyword);
    }

    @Override
    public List<TableExampleEntity> getList(String typeId, PaginationTableExample paginationTableExample) {
        return this.baseMapper.getList(typeId, paginationTableExample);
    }

    @Override
    public List<TableExampleEntity> getList(PaginationTableExample paginationTableExample) {
        return this.baseMapper.getList(paginationTableExample);
    }

    @Override
    public TableExampleEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void delete(TableExampleEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public void create(TableExampleEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, TableExampleEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public boolean rowEditing(TableExampleEntity entity) {
        return this.baseMapper.rowEditing(entity);
    }

}
