package jnpf.base.util.common;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Predicate;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.FormColumnModel;
import jnpf.model.visualjson.analysis.FormColumnTableModel;
import jnpf.model.visualjson.analysis.FormEnum;
import jnpf.model.visualjson.config.RuleConfig;
import jnpf.util.JsonUtil;
import jnpf.util.visiual.JnpfKeyConsts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 代码生成器数据处理工具类
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/8/26
 */
public class DataControlUtils {

    DataControlUtils() {
    }

    /**
     * 将字符串的首字母转大写
     *
     * @param name 需要转换的字符串
     * @return
     */
    public static String captureName(String name) {
        char[] ch = name.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            if (i == 0) {
                ch[0] = Character.toUpperCase(ch[0]);
            }
        }
        StringBuilder a = new StringBuilder();
        a.append(ch);
        return a.toString();

    }

    public static String initialLowercase(String name) {
        char[] ch = name.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            if (i == 0) {
                ch[0] = Character.toLowerCase(ch[0]);
            }
        }
        StringBuilder a = new StringBuilder();
        a.append(ch);
        return a.toString();
    }

    public static String getPlaceholder(String jnpfKey) {
        String placeholderName = "请选择";
        switch (jnpfKey) {
            case JnpfKeyConsts.BILLRULE:
            case JnpfKeyConsts.MODIFYUSER:
            case JnpfKeyConsts.CREATEUSER:
            case JnpfKeyConsts.COM_INPUT:
            case JnpfKeyConsts.TEXTAREA:
                placeholderName = "请输入";
                break;
            default:
                break;
        }
        return placeholderName;
    }

    /**
     * 去重
     *
     * @param keyExtractor
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static FieLdsModel setAbleIDs(FieLdsModel fieLdsModel) {
        if (fieLdsModel.getAbleDepIds() != null) {
            fieLdsModel.setAbleDepIds(JSON.toJSONString(fieLdsModel.getAbleDepIds()));
        }
        if (fieLdsModel.getAblePosIds() != null) {
            fieLdsModel.setAblePosIds(JSON.toJSONString(fieLdsModel.getAblePosIds()));
        }
        if (fieLdsModel.getAbleUserIds() != null) {
            fieLdsModel.setAbleUserIds(JSON.toJSONString(fieLdsModel.getAbleUserIds()));
        }
        if (fieLdsModel.getAbleRoleIds() != null) {
            fieLdsModel.setAbleRoleIds(JSON.toJSONString(fieLdsModel.getAbleRoleIds()));
        }
        if (fieLdsModel.getAbleGroupIds() != null) {
            fieLdsModel.setAbleGroupIds(JSON.toJSONString(fieLdsModel.getAbleGroupIds()));
        }
        if (fieLdsModel.getAbleIds() != null) {
            fieLdsModel.setAbleIds(JSON.toJSONString(fieLdsModel.getAbleIds()));
        }
        //model字段验证reg转换
        if (fieLdsModel.getConfig().getRegList() != null) {
            String o1 = JSON.toJSONString(JsonUtil.getObjectToString(fieLdsModel.getConfig().getRegList()));
            fieLdsModel.getConfig().setReg(o1);
        }
        return fieLdsModel;
    }

    /**
     * 单据规则配置获取
     *
     * @param formAllModel
     * @return
     */
    public static Map<String, Object> getBillRule(List<FormAllModel> formAllModel) {
        Map<String, Object> billRuleMap = new HashMap<>();
        for (FormAllModel t : formAllModel) {
            if (FormEnum.MAST.getMessage().equals(t.getJnpfKey())) {
                FieLdsModel fieLdsModel = t.getFormColumnModel().getFieLdsModel();
                RuleConfig ruleJson = getRuleJson(fieLdsModel);
                if (Objects.nonNull(ruleJson)) {
                    billRuleMap.put(fieLdsModel.getVModel(), ruleJson);
                }
            }
            if (FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())) {
                FieLdsModel fieLdsModel = t.getFormMastTableModel().getMastTable().getFieLdsModel();
                RuleConfig ruleJson = getRuleJson(fieLdsModel);
                if (Objects.nonNull(ruleJson)) {
                    billRuleMap.put(fieLdsModel.getVModel(), ruleJson);
                }
            }
            if (FormEnum.TABLE.getMessage().equals(t.getJnpfKey())) {
                FormColumnTableModel childModel = t.getChildList();
                String aliasLowName = childModel.getAliasLowName();
                List<FormColumnModel> childList = childModel.getChildList();
                for (FormColumnModel child : childList) {
                    FieLdsModel fieLdsModel = child.getFieLdsModel();
                    RuleConfig ruleJson = getRuleJson(fieLdsModel);
                    if (Objects.nonNull(ruleJson)) {
                        billRuleMap.put(aliasLowName + "_" + fieLdsModel.getVModel(), ruleJson);
                    }
                }
            }
        }
        return billRuleMap;
    }

    private static RuleConfig getRuleJson(FieLdsModel fieLdsModel) {
        Integer ruleType = fieLdsModel.getConfig().getRuleType();
        if (Objects.equals(ruleType, 2)) {
            RuleConfig ruleConfig = fieLdsModel.getConfig().getRuleConfig();
            if (ruleConfig != null) {
                return ruleConfig;
            }
        }
        return null;
    }
}
