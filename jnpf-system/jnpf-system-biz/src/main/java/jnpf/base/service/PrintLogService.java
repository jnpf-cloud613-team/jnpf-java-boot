package jnpf.base.service;

import jnpf.base.PaginationTime;
import jnpf.base.model.vo.PrintLogVO;
import jnpf.base.entity.PrintLogEntity;

import java.util.List;


public interface PrintLogService extends SuperService<PrintLogEntity> {
    /**
     * 列表
     * @param printId
     * @param page
     * @return
     */
    List<PrintLogVO> list(String printId, PaginationTime page);
}