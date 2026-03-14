package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import com.google.common.collect.ImmutableList;
import jnpf.base.UserInfo;
import jnpf.emnus.SearchMethodEnum;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.FieldEnum;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatejson.FlowParamModel;
import jnpf.flowable.model.templatenode.nodejson.GroupsModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.ProperCond;
import jnpf.flowable.model.templatenode.nodejson.TemplateJsonModel;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 20:22
 */

@Slf4j
public class FlowJsonUtil {

    private FlowJsonUtil() {}

    public static final String FALSE = "false";


    /**
     * 节点条件判断
     **/
    public static boolean nodeConditionDecide(FlowMethod flowMethod) {
        List<ProperCond> conditionList = flowMethod.getConditions();
        String matchLogic = flowMethod.getMatchLogic();
        boolean flag = false;
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("js");
        Map<String, Object> map = flowMethod.getFormData();
        List<String> expressionAll = new ArrayList<>();
        StringBuilder condition = new StringBuilder();
        for (ProperCond cond : conditionList) {
            StringBuilder expression = new StringBuilder();
            expression.append("(");
            String logic = cond.getLogic();
            List<GroupsModel> groups = cond.getGroups();
            for (int i = 0; i < groups.size(); i++) {
                GroupsModel groupsModel = groups.get(i);
                String contain = "!=-1";
                String field = groupsModel.getField();
                String jnpfKey = groupsModel.getJnpfKey();
                int fieldType = groupsModel.getFieldType();
                String symbol = groupsModel.getSymbol();
                boolean inLike = "like".equals(symbol) || "notLike".equals(symbol);
                boolean includes = "in".equals(symbol) || "notIn".equals(symbol);
                List<Object> formIncludeValue = new ArrayList<>();
                Object form = Objects.equals(FieldEnum.FIELD.getCode(), fieldType) ? formValue(flowMethod, jnpfKey, map.get(field), formIncludeValue) : formula(groupsModel, map, formIncludeValue);
                Object formValue = form;
                if ("<>".equals(symbol)) {
                    symbol = "!=";
                }
                int fieldValueType = groupsModel.getFieldValueType();
                String valueJnpfKey = StringUtil.isNotEmpty(groupsModel.getFieldValueJnpfKey()) ? groupsModel.getFieldValueJnpfKey() : jnpfKey;
                Object filedData = groupsModel.getFieldValue();

                List<Integer> valueType = ImmutableList.of(FieldEnum.CONDITION.getCode(), FieldEnum.SYSTEM.getCode());
                Object value = null;
                List<Object> filedIncludeValue = new ArrayList<>();
                if (valueType.contains(fieldValueType)) {

                    TemplateJsonModel templateJsonModel = new TemplateJsonModel();
                    templateJsonModel.setField(String.valueOf(groupsModel.getFieldValue()));
                    templateJsonModel.setSourceType(Objects.equals(FieldEnum.CONDITION.getCode(), fieldValueType) ? FieldEnum.SYSTEM.getCode() : FieldEnum.FIELD.getCode());
                    templateJsonModel.setRelationField(String.valueOf(groupsModel.getFieldValue()));


                    TaskEntity taskEntity = flowMethod.getTaskEntity();

                    Map<String, Object> mapData = new HashMap<>();
                    if (null != taskEntity && StringUtils.isNotEmpty(taskEntity.getGlobalParameter())) {

                        mapData = JsonUtil.stringToMap(taskEntity.getGlobalParameter());

                    }
                    if (CollUtil.isEmpty(mapData)) {
                        NodeModel global = flowMethod.getNodes().get(NodeEnum.GLOBAL.getType());
                        if (null != global) {
                            List<FlowParamModel> paramModelList = global.getGlobalParameterList();
                            for (FlowParamModel model : paramModelList) {
                                mapData.put(model.getFieldName(), model.getDefaultValue());
                            }
                        }
                    }

                    List<TemplateJsonModel> list = new ArrayList<>();
                    list.add(templateJsonModel);
                    RecordEntity recordEntity = new RecordEntity();
                    recordEntity.setNodeCode(flowMethod.getNodeCode());
                    UserEntity createUser = flowMethod.getCreateUser();
                    UserEntity delegate = flowMethod.getDelegate();

                    FlowModel parameterModel = new FlowModel();
                    parameterModel.setFormData(mapData);
                    parameterModel.setRecordEntity(recordEntity);
                    parameterModel.setTaskEntity(taskEntity);
                    Map<String, String> resMap = FlowUtil.parameterMap(parameterModel, list, createUser, delegate);
                    if (includes) {
                        if (filedData instanceof List) {
                            try {
                                List<List<String>> dataAll = (List<List<String>>) filedData;
                                for (List<String> data : dataAll) {
                                    for (int s = 0; s < data.size(); s++) {
                                        if (s == data.size() - 1) {
                                            filedIncludeValue.add("'" + data.get(s) + "'");
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                List<String> filedList = (List<String>) filedData;
                                for (String id : filedList) {
                                    filedIncludeValue.add("'" + resMap.get(id) + "'");
                                }
                            }
                        }
                    } else {
                        value = resMap.get(filedData);
                        if (value != null) {
                            value = "'" + value + "'";
                        }
                    }
                } else {
                    if (includes) {
                        if (filedData instanceof List) {
                            List<String> filedList = new ArrayList<>();
                            try {
                                List<List<String>> dataAll = JsonUtil.getJsonToBean(filedData, List.class);
                                for (List<String> data : dataAll) {
                                    for (int s = 0; s < data.size(); s++) {
                                        if (s == data.size() - 1) {
                                            filedList.add(data.get(s));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                List<String> dataAll = JsonUtil.getJsonToBean(filedData, List.class);
                                for (String data : dataAll) {
                                    filedList.add(data);
                                }
                            }
                            for (String id : filedList) {
                                List<Object> includeValue = new ArrayList<>();
                                if (Objects.equals(FieldEnum.CUSTOM.getCode(), fieldValueType)) {
                                    filedValue(flowMethod, id, valueJnpfKey, form, includeValue);
                                } else {
                                    filedData(flowMethod, id, valueJnpfKey, form, includeValue);
                                }
                                filedIncludeValue.addAll(includeValue);
                            }
                        } else {
                            List<String> filedList = ImmutableList.of(String.valueOf(filedData));
                            for (String id : filedList) {
                                List<Object> includeValue = new ArrayList<>();
                                if (Objects.equals(FieldEnum.CUSTOM.getCode(), fieldValueType)) {
                                    filedValue(flowMethod, id, valueJnpfKey, form, includeValue);
                                } else {
                                    filedData(flowMethod, id, valueJnpfKey, form, includeValue);
                                }
                                filedIncludeValue.addAll(includeValue);
                            }
                        }
                    } else {
                        value = Objects.equals(FieldEnum.CUSTOM.getCode(), fieldValueType) ? filedValue(flowMethod, filedData, valueJnpfKey, form, filedIncludeValue) : filedData(flowMethod, filedData, valueJnpfKey, form, filedIncludeValue);
                    }
                }
                Object fieldValue = value;
                String pression = formValue + symbol + fieldValue;
                // 比较的处理
                if ("<=".equals(symbol) || "<".equals(symbol) || ">".equals(symbol) || ">=".equals(symbol)) {
                    try {
                        String formValueStr = formValue == null ? "" : formValue.toString();
                        if (formValueStr.startsWith("'") && formValueStr.endsWith("'")) {
                            formValueStr = formValueStr.substring(1, formValueStr.length() - 1);
                        }
                        boolean time = ReUtil.isMatch(PatternPool.TIME, formValueStr);
                        if (time) {
                            formValueStr = DateUtil.parse(formValueStr).getTime() + "";
                        }
                        BigDecimal a = new BigDecimal(formValueStr);
                        String fieldValueStr = fieldValue == null ? "" : fieldValue.toString();
                        if (fieldValueStr.startsWith("'") && fieldValueStr.endsWith("'")) {
                            fieldValueStr = fieldValueStr.substring(1, fieldValueStr.length() - 1);
                        }
                        boolean valTime = ReUtil.isMatch(PatternPool.TIME, fieldValueStr);
                        if (valTime) {
                            fieldValueStr = DateUtil.parse(fieldValueStr).getTime() + "";
                        }
                        BigDecimal b = new BigDecimal(fieldValueStr);
                        boolean res = false;
                        if ("<=".equals(symbol)) {
                            res = a.compareTo(b) <= 0;
                        } else if ("<".equals(symbol)) {
                            res = a.compareTo(b) < 0;
                        } else if (">".equals(symbol)) {
                            res = a.compareTo(b) > 0;
                        } else if (">=".equals(symbol)) {
                            res = a.compareTo(b) >= 0;
                        }
                        pression = res + "";
                    } catch (Exception e) {
                        log.info(e.getMessage());
                        pression = FALSE;
                    }
                }
                if (Objects.equals(symbol, "between")) {
                    try {
                        String formValueStr = formValue == null ? "" : formValue.toString();
                        if (formValueStr.startsWith("'") && formValueStr.endsWith("'")) {
                            formValueStr = formValueStr.substring(1, formValueStr.length() - 1);
                        }
                        boolean time = ReUtil.isMatch(PatternPool.TIME, formValueStr);
                        if (time) {
                            formValueStr = DateUtil.parse(formValueStr).getTime() + "";
                        }
                        String fieldValueStr = fieldValue == null ? "" : fieldValue.toString();
                        if (fieldValueStr.startsWith("'") && fieldValueStr.endsWith("'")) {
                            fieldValueStr = fieldValueStr.substring(1, fieldValueStr.length() - 1);
                        }
                        String[] split = fieldValueStr.split(",");
                        StringBuilder json = new StringBuilder();
                        String searchModel = " && ";
                        StringJoiner joiner = new StringJoiner(searchModel);
                        json.append("(");
                        if (split.length > 1) {
                            BigDecimal a = new BigDecimal(formValueStr);
                            List<String> valueList = new ArrayList<>();
                            valueList.addAll(Arrays.asList(fieldValueStr.split(",")));
                            for (int j = 0; j < valueList.size(); j++) {
                                String val = valueList.get(j);
                                if ("null".equals(val)) {
                                    val = "0";
                                }
                                boolean valTime = ReUtil.isMatch(PatternPool.TIME, val);
                                if (valTime) {
                                    val = DateUtil.parse(val).getTime() + "";
                                }
                                BigDecimal b = new BigDecimal(val);
                                if (j == 0) {
                                    joiner.add((a.compareTo(b) >= 0) + "");
                                } else {
                                    joiner.add((a.compareTo(b) <= 0) + "");
                                }
                            }
                            json.append(joiner);
                            json.append(")");
                            pression = json + "";
                        } else {
                            pression = formValue + "==" + fieldValue;
                        }
                    } catch (Exception e) {
                        FlowJsonUtil.log.info(e.getMessage());
                        pression = FALSE;
                    }
                }
                if (inLike) {
                    if ("notLike".equals(symbol)) {
                        contain = "==-1";
                    }
                    symbol = ".indexOf";
                    if (!(formValue instanceof CharSequence)) {
                        formValue = "'" + formValue + "'";
                    }
                    if (!(fieldValue instanceof CharSequence)) {
                        fieldValue = "'" + fieldValue + "'";
                    }
                    pression = formValue + ".toString()" + symbol + "(" + fieldValue + ")" + contain;
                }
                if (includes) {
                    try {
                        boolean isNotIn = "notIn".equals(symbol);
                        String searchModel = isNotIn ? " && " : " || ";
                        symbol = !isNotIn ? " == " : " != ";
                        StringBuilder json = new StringBuilder();
                        json.append("(");
                        for (int formIndex = 0; formIndex < formIncludeValue.size(); formIndex++) {
                            Object formData = formIncludeValue.get(formIndex);
                            if (!(formData instanceof CharSequence)) {
                                formData = "'" + formData + "'";
                            }
                            for (int fieldIndex = 0; fieldIndex < filedIncludeValue.size(); fieldIndex++) {
                                Object fieldData = filedIncludeValue.get(fieldIndex);
                                if (!(fieldData instanceof CharSequence)) {
                                    fieldData = "'" + fieldData + "'";
                                }
                                json.append(formData).append(symbol).append(fieldData);
                                if (!(formIndex == formIncludeValue.size() - 1 && fieldIndex == filedIncludeValue.size() - 1)) {
                                    json.append(searchModel);
                                }
                            }
                        }
                        json.append(")");
                        pression = formIncludeValue.isEmpty() || filedIncludeValue.isEmpty() ? FALSE : json.toString();
                    } catch (Exception e) {
                        log.info(e.getMessage());
                        pression = FALSE;
                    }
                }
                if (ObjectUtil.equals(symbol, "null")) {
                    pression = "(" + formValue + " == null || " + formValue + " == '')";
                }
                if (ObjectUtil.equals(symbol, "notNull")) {
                    pression = "(" + formValue + " != null && " + formValue + " != '')";
                }
                expression.append(pression);
                if (!StringUtils.isEmpty(logic) && i != groups.size() - 1) {
                    expression.append(" ").append(search(logic)).append(" ");
                }
            }
            expression.append(")");
            expressionAll.add(expression.toString());
        }
        for (int i = 0; i < expressionAll.size(); i++) {
            String script = expressionAll.get(i);
            String search = i != expressionAll.size() - 1 ? search(matchLogic) : "";
            condition.append(script).append(" ").append(search).append(" ");
        }
        try {
            flag = (Boolean) scriptEngine.eval(condition.toString());
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return flag;
    }

    /**
     * 条件表达式
     *
     * @param logic
     */
    private static String search(String logic) {
        return SearchMethodEnum.AND.getSymbol().equalsIgnoreCase(logic) ? "&&" : "||";
    }

    /**
     * 条件数据修改
     *
     * @param flowMethod
     * @param value
     */
    private static Object filedValue(FlowMethod flowMethod, Object value, String jnpfKey, Object form, List<Object> filedIncludeValue) {
        UserInfo userInfo = flowMethod.getUserInfo();
        if ("currentUser".equals(value)) {
            value = userInfo.getUserId();
        }
        try {
            List<List<String>> dataAll = JsonUtil.getJsonToBean(value, List.class);
            List<String> id = new ArrayList<>();
            for (List<String> data : dataAll) {
                for (int i = 0; i < data.size(); i++) {
                    if (i == data.size() - 1) {
                        id.add(data.get(i));
                        filedIncludeValue.add("'" + data.get(i) + "'");
                    }
                }
            }
            value = String.join(",", id);
        } catch (Exception e) {
            try {
                List<String> id = new ArrayList<>();
                List<String> dataAll = JsonUtil.getJsonToList(value, String.class);
                if (JnpfKeyConsts.CURRORGANIZE.equals(jnpfKey)) {
                    value = dataAll.stream().filter(t -> ("'" + t + "'").equals(form)).findFirst().orElse(null);
                } else {
                    for (String data : dataAll) {
                        id.add(data);
                        filedIncludeValue.add("'" + data + "'");
                    }
                    value = String.join(",", id);
                }
            } catch (Exception e1) {
                log.info(e1.getMessage());
            }
        }
        if (value instanceof CharSequence) {
            value = "'" + value + "'";
        }
        if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            value = new BigDecimal(bigDecimal.stripTrailingZeros().toPlainString());
        }
        if (filedIncludeValue.isEmpty()) {
            filedIncludeValue.add(value);
        }
        return value;
    }

    /**
     * 条件数据修改
     *
     * @param flowMethod
     * @param value
     */
    private static Object filedData(FlowMethod flowMethod, Object value, String jnpfKey, Object form, List<Object> filedIncludeValue) {
        Map<String, Object> map = flowMethod.getFormData();
        value = map.get(value);
        UserEntity userEntity = flowMethod.getUserEntity();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        try {
            List<List<String>> dataAll = JsonUtil.getJsonToBean(value, List.class);
            List<String> id = new ArrayList<>();
            for (List<String> data : dataAll) {
                for (int i = 0; i < data.size(); i++) {
                    if (i == data.size() - 1) {
                        id.add(data.get(i));
                        filedIncludeValue.add("'" + data.get(i) + "'");
                    }
                }
            }
            value = String.join(",", id);
        } catch (Exception e) {
            try {
                List<String> id = new ArrayList<>();
                List<String> dataAll = JsonUtil.getJsonToList(value, String.class);
                if (JnpfKeyConsts.CURRORGANIZE.equals(jnpfKey) || JnpfKeyConsts.COMSELECT.equals(jnpfKey)) {
                    value = dataAll.stream().filter(t -> ("'" + t + "'").equals(form)).findFirst().orElse(null);
                } else {
                    for (String data : dataAll) {
                        id.add(data);
                        filedIncludeValue.add("'" + data + "'");
                    }
                    value = String.join(",", id);
                }
            } catch (Exception ignored) {
                log.info(ignored.getMessage());
            }
        }
        if (JnpfKeyConsts.CREATETIME.equals(jnpfKey)) {
            Date creatorTime = taskEntity.getCreatorTime();
            value = null == creatorTime ? null : creatorTime.getTime();
        } else if (JnpfKeyConsts.CREATEUSER.equals(jnpfKey)) {
            value = taskEntity.getCreatorUserId();
        } else if (JnpfKeyConsts.CURRORGANIZE.equals(jnpfKey)) {
            value = userEntity.getOrganizeId();
        } else if (JnpfKeyConsts.CURRPOSITION.equals(jnpfKey)) {
            value = userEntity.getPositionId();
        } else if (JnpfKeyConsts.MODIFYTIME.equals(jnpfKey)) {
            Date lastModifyTime = taskEntity.getLastModifyTime();
            value = null == lastModifyTime ? null : lastModifyTime.getTime();
        } else if (JnpfKeyConsts.MODIFYUSER.equals(jnpfKey)) {
            value = taskEntity.getLastModifyUserId();
        }
        if (value instanceof CharSequence) {
            value = "'" + value + "'";
        }
        if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            value = new BigDecimal(bigDecimal.stripTrailingZeros().toPlainString());
        }
        if (filedIncludeValue.isEmpty()) {
            filedIncludeValue.add(value);
        }
        return value;
    }

    /**
     * 表单数据修改
     *
     * @param form
     */
    private static Object formValue(FlowMethod flowMethod, String jnpfKey, Object form, List<Object> fieldIncludeValue) {
        Object result = form;
        UserEntity userEntity = flowMethod.getUserEntity();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        try {
            List<List<String>> dataAll = JsonUtil.getJsonToBean(form, List.class);
            List<String> id = new ArrayList<>();
            for (List<String> data : dataAll) {
                for (int i = 0; i < data.size(); i++) {
                    if (i == data.size() - 1) {
                        id.add(data.get(i));
                        fieldIncludeValue.add("'" + data.get(i) + "'");
                    }
                }
            }
            result = String.join(",", id);
        } catch (Exception e) {
            try {
                List<String> id = new ArrayList<>();
                List<String> dataAll = JsonUtil.getJsonToList(form, String.class);
                for (String data : dataAll) {
                    id.add(data);
                    fieldIncludeValue.add("'" + data + "'");
                }
                result = String.join(",", id);
            } catch (Exception e1) {
                log.info(e1.getMessage());
            }
        }
        if (JnpfKeyConsts.CREATETIME.equals(jnpfKey)) {
            Date creatorTime = taskEntity.getCreatorTime();
            result = null == creatorTime ? null : creatorTime.getTime();
        } else if (JnpfKeyConsts.CREATEUSER.equals(jnpfKey)) {
            result = StringUtil.isNotEmpty(taskEntity.getDelegateUserId()) ? taskEntity.getDelegateUserId() : taskEntity.getCreatorUserId();
        } else if (JnpfKeyConsts.CURRORGANIZE.equals(jnpfKey)) {
            result = userEntity.getOrganizeId();
        } else if (JnpfKeyConsts.CURRPOSITION.equals(jnpfKey)) {
            result = userEntity.getPositionId();
        } else if (JnpfKeyConsts.MODIFYTIME.equals(jnpfKey)) {
            Date lastModifyTime = taskEntity.getLastModifyTime();
            result = null == lastModifyTime ? null : lastModifyTime.getTime();
        } else if (JnpfKeyConsts.MODIFYUSER.equals(jnpfKey)) {
            result = taskEntity.getLastModifyUserId();
        }
        if (result instanceof CharSequence) {
            result = "'" + result + "'";
        }
        if (result instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) result;
            result = new BigDecimal(bigDecimal.stripTrailingZeros().toPlainString());
        }
        if (fieldIncludeValue.isEmpty()) {
            fieldIncludeValue.add(result);
        }
        return result;
    }

    /**
     * 表达式
     */
    private static Object formula(GroupsModel properCond, Map<String, Object> data, List<Object> fieldIncludeValue) {
        Object result = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("function getNum(val) {\n" +
                    "  return isNaN(val) ? 0 : Number(val)\n" +
                    "};\n" +
                    "// 求和\n" +
                    "function SUM() {\n" +
                    "  var value = 0\n" +
                    "  for (var i = 0; i < arguments.length; i++) {\n" +
                    "    value += getNum(arguments[i])\n" +
                    "  }\n" +
                    "  return value\n" +
                    "};\n" +
                    "// 求差\n" +
                    "function SUBTRACT(num1, num2) {\n" +
                    "  return getNum(num1) - getNum(num2)\n" +
                    "};\n" +
                    "// 相乘\n" +
                    "function PRODUCT() {\n" +
                    "  var value = 1\n" +
                    "  for (var i = 0; i < arguments.length; i++) {\n" +
                    "    value = value * getNum(arguments[i])\n" +
                    "  }\n" +
                    "  return value\n" +
                    "};\n" +
                    "// 相除\n" +
                    "function DIVIDE(num1, num2) {\n" +
                    "  return getNum(num1) / (getNum(num2) === 0 ? 1 : getNum(num2))\n" +
                    "};\n" +
                    "// 获取参数的数量\n" +
                    "function COUNT() {\n" +
                    "  var value = 0\n" +
                    "  for (var i = 0; i < arguments.length; i++) {\n" +
                    "    value ++\n" +
                    "  }\n" +
                    "  return value\n" +
                    "};\n");
            String field = field(properCond.getField(), data, false);
            ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
            ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("js");
            String eval = builder + " var result = " + field + ";";
            scriptEngine.eval(eval);
            double d = (double) scriptEngine.get("result");
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setRoundingMode(RoundingMode.UP);
            result = nf.format(d);
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        if (result != null) {
            result = "'" + result + "'";
        }
        fieldIncludeValue.add(result);
        return result;
    }

    /**
     * 替换文本值
     *
     * @param content
     * @param data
     * @return
     */
    public static String field(String content, Map<String, Object> data, boolean isData) {
        String pattern = "\\{([^}]+)}";
        Pattern patternList = Pattern.compile(pattern);
        Matcher matcher = patternList.matcher(content);
        Map<String, List<String>> parameterMap = data(matcher, data);
        Map<String, Object> result = new HashMap<>();
        if (isData) {
            Map<String, String> datas = new HashMap<>();
            for (String key : parameterMap.keySet()) {
                datas.put(key, data.get(key) != null ? String.valueOf(data.get(key)) : "");
            }
            result.putAll(datas);
        } else {
            Map<String, Object> dataAll = new HashMap<>();
            for (Map.Entry<String, List<String>> stringListEntry : parameterMap.entrySet()) {
                String key = stringListEntry.getKey();
                StringJoiner joiner = new StringJoiner(",");
                List<String> list = parameterMap.get(key);
                for (String id : list) {
                    joiner.add("'" + id + "'");
                }
                String value = joiner.toString();
                if (list.size() > 1) {
                    value = "SUM(" + joiner + ")";
                }
                dataAll.put(key, value);
            }
            result.putAll(dataAll);
        }
        StringSubstitutor strSubstitutor = new StringSubstitutor(result, "{", "}");
        return strSubstitutor.replace(content);
    }

    /**
     * 赋值
     */
    private static Map<String, List<String>> data(Matcher matcher, Map<String, Object> dataAll) {
        Map<String, List<String>> map = new HashMap<>();
        Map<String, String> keyAll = new HashMap<>();
        while (matcher.find()) {
            String group = matcher.group().replace("{", "").replace("}", "");
            keyAll.put(group, group);
        }
        for (String id : keyAll.keySet()) {
            List<String> valueData = new ArrayList<>();
            String[] valueAll = id.split("-");
            String key = valueAll[0];
            Object childDataAll = dataAll.get(key) != null ? dataAll.get(key) : "";
            if (valueAll.length > 1) {
                String data = valueAll[1];
                if (childDataAll instanceof List) {
                    List<Map<String, Object>> childData = (List<Map<String, Object>>) childDataAll;
                    for (Map<String, Object> childDatum : childData) {
                        Object childDatas = childDatum.get(data);
                        valueData.add(childDatas + "");
                    }
                }
            } else {
                valueData.add(childDataAll + "");
            }
            map.put(id, valueData);
        }
        return map;
    }

}
