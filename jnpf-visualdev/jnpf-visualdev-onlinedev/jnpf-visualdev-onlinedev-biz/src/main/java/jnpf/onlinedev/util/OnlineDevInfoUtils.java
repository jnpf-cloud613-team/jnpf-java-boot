package jnpf.onlinedev.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.base.util.DateTimeFormatConstant;
import jnpf.base.util.FormPublicUtils;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

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
public class OnlineDevInfoUtils {
    private final UserService userApi;

    /**
     * 转换数据格式(编辑页)
     *
     * @param modelList 控件
     * @param dataMap   数据
     * @return
     */
    public Map<String, Object> getInitLineData(List<FieLdsModel> modelList, Map<String, Object> dataMap, Map<String, Object> localCache) {
        Map<String, Object> posMap = (Map<String, Object>) localCache.get("__pos_map");
        Map<String, Object> orgTreeMap = (Map<String, Object>) localCache.get("__orgTree_map");
        for (FieLdsModel swapDataVo : modelList) {
            String jnpfKey = swapDataVo.getConfig().getJnpfKey();
            String vModel = swapDataVo.getVModel();
            Object value = dataMap.get(vModel);
            if (value == null || ObjectUtil.isEmpty(value)) {
                continue;
            }
            switch (jnpfKey) {
                case JnpfKeyConsts.RATE:
                case JnpfKeyConsts.SLIDER:
                    BigDecimal ratevalue = new BigDecimal(0);
                    if (dataMap.get(vModel) != null) {
                        ratevalue = new BigDecimal(dataMap.get(vModel).toString());
                    }
                    dataMap.put(vModel, ratevalue);
                    break;
                case JnpfKeyConsts.UPLOADFZ:
                case JnpfKeyConsts.UPLOADIMG:
                    List<Map<String, Object>> fileList = JsonUtil.getJsonToListMap(String.valueOf(value));
                    dataMap.put(vModel, fileList);
                    break;

                case JnpfKeyConsts.DATE:
                case JnpfKeyConsts.DATE_CALCULATE:
                    Long dateTime = DateTimeFormatConstant.getDateObjToLong(dataMap.get(vModel));
                    dataMap.put(vModel, dateTime != null ? dateTime : dataMap.get(vModel));
                    break;

                case JnpfKeyConsts.SWITCH:
                    dataMap.put(vModel, value != null ? Integer.parseInt(String.valueOf(value)) : null);
                    break;
                //系统自动生成控件
                case JnpfKeyConsts.CURRORGANIZE:
                case JnpfKeyConsts.CURRDEPT:
                    getTreeName(dataMap, orgTreeMap, vModel);
                    break;
                case JnpfKeyConsts.CURRPOSITION:
                    getTreeName(dataMap, posMap, vModel);
                    break;
                case JnpfKeyConsts.CREATEUSER:
                case JnpfKeyConsts.MODIFYUSER:
                    UserEntity userEntity = userApi.getInfo(String.valueOf(value));
                    String userValue = Objects.nonNull(userEntity) ? userEntity.getRealName() + "/" + userEntity.getAccount() : String.valueOf(value);
                    dataMap.put(vModel, userValue);
                    break;
                default:
                    dataMap.put(vModel, FormPublicUtils.getDataConversion(value));
                    break;
            }
        }
        return dataMap;
    }


    private static void getTreeName(Map<String, Object> dataMap, Map<String, Object> posMap, String vModel) {
        String posIds = String.valueOf(dataMap.get(vModel));
        StringJoiner posName = new StringJoiner(",");
        List<String> posList = new ArrayList<>();
        try {
            posList = JsonUtil.getJsonToList(posIds, String.class);
        } catch (Exception e) {
            posList.add(posIds);
        }
        if (!posList.isEmpty()) {
            for (String t : posList) {
                if (posMap.get(t) != null) {
                    posName.add(posMap.get(t).toString());
                }
            }
        }
        if (posName.length() > 0) {
            dataMap.put(vModel, posName.toString());
        } else {
            dataMap.put(vModel, " ");
        }
    }
}
