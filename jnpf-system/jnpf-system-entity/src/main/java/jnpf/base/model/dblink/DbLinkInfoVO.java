package jnpf.base.model.dblink;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.database.constant.DbConst;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.source.impl.DbOracle;
import jnpf.exception.DataException;
import jnpf.util.*;
import lombok.Data;

import java.util.Map;

/**
 * 页面显示对象
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DbLinkInfoVO extends DbLinkBaseForm {

    /**
     * 获取连接页面显示对象
     * @param entity 连接实体对象
     * @return 返回显示对象
     * @throws DataException ignore
     */
    public DbLinkInfoVO getDbLinkInfoVO(DbLinkEntity entity) throws DataException {
        DbLinkInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, DbLinkInfoVO.class);
        vo.setPassword(DesUtil.aesOrDecode(vo.getPassword(), true, true));
        vo.setServiceName(XSSEscape.escape(entity.getDbName()));
        vo.setTableSpace(XSSEscape.escape(entity.getDbTableSpace()));
        vo.setOracleExtend(entity.getOracleExtend() != null && entity.getOracleExtend() == 1);
        if(StringUtil.isNotEmpty(entity.getOracleParam())){
            Map<String, Object> oracleParam = JsonUtil.stringToMap(entity.getOracleParam());
            if(oracleParam.size() > 0){
                vo.setOracleLinkType(oracleParam.get(DbOracle.ORACLE_LINK_TYPE) != null ? oracleParam.get(DbOracle.ORACLE_LINK_TYPE).toString() : null);
                vo.setOracleRole(oracleParam.get(DbOracle.ORACLE_ROLE) != null ? oracleParam.get(DbOracle.ORACLE_ROLE).toString() : null);
                vo.setOracleService(oracleParam.get(DbOracle.ORACLE_SERVICE) != null ? oracleParam.get(DbOracle.ORACLE_SERVICE).toString() : null);
                vo.setClientEncoding(oracleParam.get(DbConst.ENCODING_CONVERT_CLIENT) != null ? oracleParam.get(DbConst.ENCODING_CONVERT_CLIENT).toString() : null);
                vo.setServerEncoding(oracleParam.get(DbConst.ENCODING_CONVERT_SERVER) != null ? oracleParam.get(DbConst.ENCODING_CONVERT_SERVER).toString() : null);
            }
        }
        return vo;
    }

    @Schema(description = "主键")
    private String id;

}
