package jnpf.base.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import jnpf.base.entity.BillNumEntity;
import jnpf.base.service.BillNumService;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.model.visualjson.config.PrefixSuffixModel;
import jnpf.model.visualjson.config.RuleConfig;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VisualBillUtil {

    private final BillNumService billNumService;

    @Synchronized
    @DS("")
    public Object getBillNumber(String visualId, FieLdsModel fieLdsModel, Map<String, Object> data, Object thisValue) {
        String jnpfKey = fieLdsModel.getConfig().getJnpfKey();
        if (JnpfKeyConsts.BILLRULE.equals(jnpfKey) && fieLdsModel.getConfig().getRuleType() != null
                && Objects.equals(fieLdsModel.getConfig().getRuleType(), 2) && ObjectUtil.isEmpty(thisValue)) {
            ConfigModel config = fieLdsModel.getConfig();
            RuleConfig ruleConfig = config.getRuleConfig();
            Integer type = ruleConfig.getType();
            String flowId = null;
            if (data.get(FlowFormConstant.FLOWID) != null && StringUtil.isNotEmpty(data.get(FlowFormConstant.FLOWID).toString())) {
                flowId = data.get(FlowFormConstant.FLOWID).toString();
            }

            StringBuilder strNumber = new StringBuilder();
            //前缀
            String preFixStr = setPreSuffFix(data, ruleConfig.getPrefixList());
            if (StringUtil.isNotEmpty(preFixStr) && preFixStr.length() > 100) {
                throw new DataException(MsgCode.VS027.get(config.getLabel()));
            }
            strNumber.append(preFixStr);

            String ruleId = config.getFormId();
            BillNumEntity billNum = billNumService.getBillNum(ruleId, visualId, flowId);
            switch (type) {
                case 2:
                    // 随机数编号
                    if (ObjectUtil.equal(ruleConfig.getRandomType(), 1)) {
                        strNumber.append(cn.hutool.core.util.RandomUtil.randomNumbers(ruleConfig.getRandomDigit()));
                    } else {
                        strNumber.append(cn.hutool.core.util.RandomUtil.randomStringUpper(ruleConfig.getRandomDigit()));
                    }
                    if (billNum != null) {
                        billNumService.removeByRuleId(ruleId, visualId, flowId);
                    }
                    break;
                case 3:
                    // UUID
                    strNumber.append(IdUtil.randomUUID().toUpperCase());
                    if (billNum != null) {
                        billNumService.removeByRuleId(ruleId, visualId, flowId);
                    }
                    break;
                default:
                    getDefault(visualId, ruleConfig, billNum, ruleId, flowId, strNumber);
                    break;
            }
            //后缀
            String suffFixStr = setPreSuffFix(data, ruleConfig.getSuffixList());
            if (StringUtil.isNotEmpty(suffFixStr) && suffFixStr.length() > 100) {
                throw new DataException(MsgCode.VS027.get(config.getLabel()));
            }
            strNumber.append(suffFixStr);
            return strNumber.toString();
        } else {
            return thisValue;
        }
    }

    private void getDefault(String visualId, RuleConfig ruleConfig, BillNumEntity billNum, String ruleId, String flowId, StringBuilder strNumber) {
        // 时间格式
        RuleConfig rule = BeanUtil.copyProperties(ruleConfig, RuleConfig.class);
        rule.setRandomDigit(null);
        rule.setRandomType(null);
        String ruleJosn = JsonUtil.getObjectToString(rule);

        String dateFormat = getTimeFormat(ruleConfig.getDateFormat());
        String dateValue = "no".equals(dateFormat) ? "" : DateUtil.dateNow(dateFormat);
        //获取位数最大值
        Integer digit = ruleConfig.getDigit();
        StringBuilder maxStr = new StringBuilder();
        for (int i = 0; i < digit; i++) {
            maxStr.append("9");
        }
        Integer maxValue = Integer.parseInt(maxStr.toString());
        //起始值
        Integer startNumber = Integer.parseInt(ruleConfig.getStartNumber());
        Integer thisNum = 0;

        //处理流水号归0
        if (billNum != null) {
            if (ruleJosn.equals(billNum.getRuleConfig())) {
                String dateValueOld = billNum.getDateValue();
                //判断时间值是否一致，一致流水号递增，不一致则重置流水号
                if (StringUtil.isEmpty(dateValueOld) || dateValueOld.equals(dateValue)) {
                    thisNum = billNum.getNum() + 1;
                    if (startNumber + thisNum > maxValue) {
                        thisNum = 0;
                    }
                }
            }
        } else {
            billNum = new BillNumEntity();
        }
        billNum.setRuleId(ruleId);
        billNum.setVisualId(visualId);
        billNum.setFlowId(flowId);
        billNum.setDateValue(dateValue);
        billNum.setNum(thisNum);
        billNum.setRuleConfig(ruleJosn);
        billNumService.saveBillNum(billNum);

        if (!"no".equals(dateValue)) {
            strNumber.append(dateValue);
        }
        strNumber.append(PadUtil.padRight(String.valueOf(startNumber + thisNum), ruleConfig.getDigit(), '0'));
    }

    /**
     * 获取时间格式
     *
     * @param dateFor
     * @return
     */
    private static String getTimeFormat(String dateFor) {
        String dateForValue = "no";
        if (StringUtil.isEmpty(dateFor)) {
            return dateForValue;
        }
        switch (dateFor) {
            case "YYYY":
                dateForValue = "yyyy";
                break;
            case "YYYYMM":
                dateForValue = "yyyyMM";
                break;
            case "YYYYMMDD":
                dateForValue = "yyyyMMdd";
                break;
            case "YYYYMMDDHH":
                dateForValue = "yyyyMMddHH";
                break;
            case "YYYYMMDDHHmm":
                dateForValue = "yyyyMMddHHmm";
                break;
            case "YYYYMMDDHHmmss":
                dateForValue = "yyyyMMddHHmmss";
                break;
            case "YYYYMMDDHHmmssSSS":
                dateForValue = "yyyyMMddHHmmssSSS";
                break;
            default:
                break;
        }
        return dateForValue;
    }

    /**
     * 设置前后缀的值
     *
     * @param data
     * @param list
     */
    private static String setPreSuffFix(Map<String, Object> data, List<PrefixSuffixModel> list) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(list)) {
            for (PrefixSuffixModel prefix : list) {
                //sourtype = 2自定义，1表单字段
                if (Objects.equals(prefix.getSourceType(), 2)) {
                    sb.append(prefix.getRelationField());
                } else {
                    if (StringUtil.isNotEmpty(prefix.getRelationField())) {
                        if (data.get(prefix.getRelationField()) != null) {
                            sb.append(data.get(prefix.getRelationField()));
                        } else if (prefix.getRelationField().toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                            String[] split = prefix.getRelationField().split("-");
                            if (split.length == 2 && data.get(split[1]) != null) {
                                sb.append(data.get(split[1]));
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}
