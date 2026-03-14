package jnpf.service.impl;

import jnpf.base.Pagination;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.DbTypeUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.entity.BigDataEntity;
import jnpf.exception.WorkFlowException;
import jnpf.mapper.BigDataMapper;
import jnpf.service.BigDataService;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

/**
 * 大数据测试
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BigDataServiceImpl extends SuperServiceImpl<BigDataMapper, BigDataEntity> implements BigDataService {


    private final DataSourceUtil dataSourceUtils;

    private final ConfigValueUtil configValueUtil;

    private final DbLinkService dbLinkService;



    @Override
    public List<BigDataEntity> getList(Pagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public void create(int insertCount) throws WorkFlowException {
        Integer code = this.baseMapper.maxCode();
        if (code == null) {
            code = 0;
        }
        int index = code == 0 ? 10000001 : code;
        if (index > 11500001) {
            throw new WorkFlowException(MsgCode.ETD113.get());
        }
        try (Connection conn = ConnUtil.getConnOrDefault(dataSourceUtils)){
            String sql;
            String tenantColumn = TenantDataSourceUtil.getTenantColumn();
            DbLinkEntity dbLinkEntity = dbLinkService.getResource("0");
            if (DbTypeUtil.checkOracle(dbLinkEntity) || DbTypeUtil.checkDM(dbLinkEntity)) {
                sql = "INSERT INTO ext_big_data(F_ID,F_EN_CODE,F_FULL_NAME,F_CREATOR_TIME{column})  VALUES (?,?,?,to_date(?,'yyyy-mm-dd hh24:mi:ss'){value})";
            } else {
                sql = "INSERT INTO ext_big_data(F_ID,F_EN_CODE,F_FULL_NAME,F_CREATOR_TIME{column})  VALUES (?,?,?,?{value})";
            }
            sql = sql.replace("{column}", "," + configValueUtil.getMultiTenantColumn());
            sql = sql.replace("{value}", ",?");
            @Cleanup PreparedStatement pstm = conn.prepareStatement(sql);
            conn.setAutoCommit(false);
            Boolean b = DbTypeUtil.checkPostgre(dbLinkEntity);
            if (null!=b&&b.equals(true)) {
                for (int i = 0; i < insertCount; i++) {
                    pstm.setString(1, RandomUtil.uuId());
                    pstm.setInt(2, index);
                    pstm.setString(3, "测试大数据" + index);
                    pstm.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    if (StringUtil.isNotEmpty(tenantColumn)) {
                        pstm.setString(5, tenantColumn);
                    }
                    pstm.addBatch();
                    index++;
                }
            } else {
                for (int i = 0; i < insertCount; i++) {
                    pstm.setString(1, RandomUtil.uuId());
                    pstm.setInt(2, index);
                    pstm.setString(3, "测试大数据" + index);
                    pstm.setString(4, DateUtil.getNow());
                    if (StringUtil.isNotEmpty(tenantColumn)) {
                        pstm.setString(5, tenantColumn);
                    }
                    pstm.addBatch();
                    index++;
                }
            }
            pstm.executeBatch();
            conn.commit();
            pstm.close();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
