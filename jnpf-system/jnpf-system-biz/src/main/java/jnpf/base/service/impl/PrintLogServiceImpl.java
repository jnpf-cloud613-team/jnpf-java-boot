package jnpf.base.service.impl;

import jnpf.base.PaginationTime;
import jnpf.base.entity.PrintLogEntity;
import jnpf.base.mapper.PrintLogMapper;
import jnpf.base.model.vo.PrintLogVO;
import jnpf.base.service.PrintLogService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrintLogServiceImpl extends SuperServiceImpl<PrintLogMapper, PrintLogEntity> implements PrintLogService {

    @Override
    public List<PrintLogVO> list(String printId, PaginationTime paginationTime) {
        return this.baseMapper.list(printId, paginationTime);
    }
}
