package jnpf.base.util;

import com.baomidou.dynamic.datasource.annotation.DS;
import jnpf.base.service.BillRuleService;
import jnpf.base.service.DataInterfaceService;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DbTableService;
import jnpf.constant.MsgCode;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserService;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static jnpf.util.Constants.ADMIN_KEY;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/4/9 13:28
 */
@Slf4j
@DS("")
@Component
@RequiredArgsConstructor
public class ServiceBaseUtil {

    private final DbLinkService dblinkService;
    private final DbTableService dbTableService;
    private final UserService userService;
    private final OrganizeService organizeService;
    private final PositionService positionService;
    private final BillRuleService billRuleService;
    private final DataInterfaceService dataInterfaceService;

    //--------------------------------数据连接------------------------------
    public DbLinkEntity getDbLink(String dbLink) {
        DbLinkEntity link = StringUtil.isNotEmpty(dbLink) && !"0".equals(dbLink) ? dblinkService.getInfo(dbLink) : TenantDataSourceUtil.getDataSourceConfig();
        if (dbLink == null) {
            throw new DataException(MsgCode.SYS054.get());
        }
        return link;
    }

    public boolean isExistTable(String dbLinkId, String table) throws SQLException {
        return dbTableService.isExistTable(dbLinkId, table);
    }

    public void createTable(List<DbTableFieldModel> dbTable) throws SQLException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        for (DbTableFieldModel dbTableFieldModel : dbTable) {
            dbTableService.createTable(dbTableFieldModel);
        }
    }

    public void addField(DbTableFieldModel dbTable) throws SQLException {
        dbTableService.addField(dbTable);
    }

    /**
     * 获取所有字段
     *
     * @param linkId 链接名
     * @param table  表名
     * @return
     * @throws Exception
     */
    public List<DbFieldModel> getFieldList(String linkId, String table) throws SQLException {
        return dbTableService.getFieldList(linkId, table);
    }


    //--------------------------------用户------------------------------

    public UserEntity getUserInfo(String id) {
        UserEntity entity = null;
        if (StringUtil.isNotEmpty(id)) {
            entity = id.equalsIgnoreCase(ADMIN_KEY) ? userService.getUserByAccount(id) : userService.getInfo(id);
        }
        return entity;
    }


    //--------------------------------单据规则------------------------------
    public String getBillNumber(String enCode) {
        String billNo = "";
        try {
            billNo = billRuleService.getBillNumber(enCode, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return billNo;
    }


    //--------------------------------组织------------------------------

    public String getOrganizeName(String idStr) {
        return organizeService.getNameByIdStr(idStr);
    }

    public OrganizeEntity getOrganizeInfo(String id) {
        return StringUtil.isNotEmpty(id) ? organizeService.getInfo(id) : null;
    }

    public List<OrganizeEntity> getOrganizeId(String organizeId) {
        List<OrganizeEntity> organizeList = new ArrayList<>();
        organizeService.getOrganizeId(organizeId, organizeList);
        Collections.reverse(organizeList);
        return organizeList;
    }

    //--------------------------------岗位------------------------------
    public String getPositionName(String idStr) {
        return positionService.getNameByIdStr(idStr);
    }

    //--------------------------------远端------------------------------
    public void infoToId(String interId, Map<String, String> parameterMap) {
        dataInterfaceService.infoToId(interId, null, parameterMap);
    }

}
