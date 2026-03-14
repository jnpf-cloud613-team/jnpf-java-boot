package jnpf.base.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.permission.entity.UserEntity;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Clob;
import java.util.*;

import static jnpf.util.Constants.ADMIN_KEY;

/**
 * 在线详情编辑工具类
 *
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/10/27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormInfoUtils {

    private final ServiceBaseUtil serviceUtil;


    /**
     * 转换数据格式(编辑页)
     *
     * @param modelList 控件
     * @param dataMap   数据
     * @return
     */
    public Map<String, Object> swapDataInfoType(List<FieLdsModel> modelList, Map<String, Object> dataMap) {
        dataMap = Optional.ofNullable(dataMap).orElse(new HashMap<>());
        try {
            DynamicDataSourceUtil.switchToDataSource(null);
            List<String> systemConditions = Arrays.asList(JnpfKeyConsts.CURRORGANIZE, JnpfKeyConsts.CURRDEPT, JnpfKeyConsts.CURRPOSITION);
            List<String> nullIsList = Arrays.asList(JnpfKeyConsts.UPLOADFZ, JnpfKeyConsts.UPLOADIMG);
            for (FieLdsModel swapDataVo : modelList) {
                String jnpfKey = swapDataVo.getConfig().getJnpfKey();
                String vModel = swapDataVo.getVModel();

                //clob字段转换
                FormInfoUtils.swapClob(dataMap, vModel);
                //获取原字段数据
                FormPublicUtils.relationGetJnpfId(dataMap, jnpfKey, dataMap.get(vModel), vModel);

                Object value = dataMap.get(vModel);
                if (value == null || ObjectUtil.isEmpty(value)) {
                    if (systemConditions.contains(jnpfKey)) {
                        dataMap.put(vModel, " ");
                    }
                    if (nullIsList.contains(jnpfKey)) {
                        dataMap.put(vModel, Collections.emptyList());
                    }
                    continue;
                }
                switch (jnpfKey) {
                    case JnpfKeyConsts.UPLOADFZ:
                    case JnpfKeyConsts.UPLOADIMG:
                        setUploadList(dataMap, vModel);
                        break;
                    case JnpfKeyConsts.DATE:
                    case JnpfKeyConsts.DATE_CALCULATE:
                        Long dateTime = DateTimeFormatConstant.getDateObjToLong(dataMap.get(vModel));
                        dataMap.put(vModel, dateTime != null ? dateTime : dataMap.get(vModel));
                        break;
                    case JnpfKeyConsts.CREATETIME:
                    case JnpfKeyConsts.MODIFYTIME:
                        String pattern = DateTimeFormatConstant.YEAR_MONTH_DHMS;
                        Long time = DateTimeFormatConstant.getDateObjToLong(dataMap.get(vModel));
                        dataMap.put(vModel, time != null ? DateUtil.dateToString(new Date(time), pattern) : "");
                        break;
                    case JnpfKeyConsts.SWITCH:
                    case JnpfKeyConsts.SLIDER:
                    case JnpfKeyConsts.RATE:
                    case JnpfKeyConsts.CALCULATE:
                    case JnpfKeyConsts.NUM_INPUT:
                        dataMap.put(vModel, value != null ? new BigDecimal(String.valueOf(value)) : null);
                        break;
                    case JnpfKeyConsts.CURRPOSITION:
                        String posName = serviceUtil.getPositionName(String.valueOf(value));
                        dataMap.put(vModel, StringUtil.isNotEmpty(posName) ? posName : value);
                        break;

                    case JnpfKeyConsts.CREATEUSER:
                    case JnpfKeyConsts.MODIFYUSER:
                        UserEntity userEntity = serviceUtil.getUserInfo(String.valueOf(value));
                        String userValue;
                        if (Objects.nonNull(userEntity)) {
                            userValue = userEntity.getAccount().equalsIgnoreCase(ADMIN_KEY) ? "管理员/" + ADMIN_KEY : userEntity.getRealName() + "/" + userEntity.getAccount();
                        } else {
                            userValue = String.valueOf(value);
                        }
                        dataMap.put(vModel, userValue);
                        break;
                    case JnpfKeyConsts.CURRORGANIZE:
                        String orgName = serviceUtil.getOrganizeName(String.valueOf(value));
                        dataMap.put(vModel, StringUtil.isNotEmpty(orgName) ? orgName : value);
                        break;
                    case JnpfKeyConsts.COM_INPUT:
                        if (dataMap.get(vModel) != null) {
                            dataMap.put(vModel, String.valueOf(dataMap.get(vModel)));
                        }
                        break;
                    default:
                        dataMap.put(vModel, FormPublicUtils.getDataConversion(value));
                        break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataException(e.getMessage());
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return dataMap;
    }

    private static void setUploadList(Map<String, Object> dataMap, String vModel) {
        //数据传递-乱塞有bug强行置空
        List<Map<String, Object>> fileList = new ArrayList<>();
        try {
            fileList = JsonUtil.getJsonToListMap(dataMap.get(vModel).toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        dataMap.put(vModel, fileList);
    }


    public static void swapClob(Map<String, Object> map, String key) {
        if (map != null && map.get(key) != null && map.get(key) instanceof Clob) {
            Clob clob = (Clob) map.get(key);
            StringBuilder sb = new StringBuilder();
            // 获取CLOB字段的内容长度
            int length = 0;
            // 以流的形式读取CLOB字段的内容
            try (java.io.Reader reader = clob.getCharacterStream()) {
                length = (int) clob.length();
                char[] buffer = new char[length];
                int bytesRead;
                // 逐个字符读取并添加到字符串构建器中
                while ((bytesRead = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            map.put(key, sb.toString());
        }
    }
}
