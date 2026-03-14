package jnpf.base.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.PaginationTime;
import jnpf.base.entity.PrintLogEntity;
import jnpf.base.model.vo.PrintLogVO;
import jnpf.permission.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Mapper
@Repository
public interface PrintLogMapper extends SuperMapper<PrintLogEntity> {

    default List<PrintLogVO> list(String printId, PaginationTime paginationTime) {
        MPJLambdaWrapper<PrintLogEntity> wrapper = new MPJLambdaWrapper<>(PrintLogEntity.class)
                .leftJoin(UserEntity.class, UserEntity::getId, PrintLogEntity::getCreatorUserId)
                .selectAll(PrintLogEntity.class)
                .select(UserEntity::getAccount, UserEntity::getRealName)
                .selectAs(PrintLogEntity::getCreatorTime, PrintLogVO::getCreatorTime);
        if (!ObjectUtil.isEmpty(paginationTime.getStartTime()) && !ObjectUtil.isEmpty(paginationTime.getEndTime())) {
            wrapper.between(PrintLogEntity::getCreatorTime, new Date(paginationTime.getStartTime()), new Date(paginationTime.getEndTime()));
        }
        if (!ObjectUtil.isEmpty(printId)) {
            wrapper.eq(PrintLogEntity::getPrintId, printId);
        }
        if (!ObjectUtil.isEmpty(paginationTime.getKeyword())) {
            wrapper.and(
                    t -> t.like(UserEntity::getRealName, paginationTime.getKeyword())
                            .or().like(UserEntity::getAccount, paginationTime.getKeyword())
                            .or().like(PrintLogEntity::getPrintTitle, paginationTime.getKeyword())
            );
        }
        Page<PrintLogVO> page = new Page<>(paginationTime.getCurrentPage(), paginationTime.getPageSize());
        IPage<PrintLogVO> userPage = this.selectJoinPage(page, PrintLogVO.class, wrapper);
        return paginationTime.setData(userPage.getRecords(), page.getTotal());
    }
}
