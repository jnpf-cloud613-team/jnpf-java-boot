package jnpf.base.service.impl;

import com.google.common.base.CaseFormat;
import jnpf.base.entity.AiEntity;
import jnpf.base.model.ai.VisualAiModel;
import jnpf.base.service.AiService;
import jnpf.base.service.VisualAiService;
import jnpf.constant.GenerateConstant;
import jnpf.constant.MsgCode;
import jnpf.constants.AiConstants;
import jnpf.exception.DataException;
import jnpf.model.ai.AiFormFieldModel;
import jnpf.model.ai.AiFormModel;
import jnpf.model.ai.AiModel;
import jnpf.model.ai.SendAiMessage;
import jnpf.util.AiLimitUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在线开发ai实现
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/2 10:05:25
 */
@Service
@RequiredArgsConstructor
public class VisualAiServiceImpl implements VisualAiService {

    private final AiService aiService;


    @Override
    public VisualAiModel form(String keyword) {
        if (!AiLimitUtil.tryAcquire(UserProvider.getLoginId())) {
            throw new DataException(MsgCode.SYS182.get());
        }
        VisualAiModel visualAiModel = new VisualAiModel();
        List<AiFormModel> aiModelList = new ArrayList<>();
        AiEntity aDefault = aiService.getDefault();
        List<AiFormModel> list = SendAiMessage.generatorModelVO(keyword, JsonUtil.getJsonToBean(aDefault, AiModel.class));
        if (CollectionUtils.isNotEmpty(list)) {
            for (int i = 0; i < list.size(); i++) {
                AiFormModel aiFormModel = list.get(i);
                if (Objects.equals(0, i)) {
                    String enCode = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, aiFormModel.getTableName());
                    visualAiModel.setFullName(aiFormModel.getTableTitle());
                    visualAiModel.setEnCode(enCode);
                    aiFormModel.setIsMain(true);
                } else {
                    aiFormModel.setIsMain(false);
                }
                List<AiFormFieldModel> fields = getAiFormFieldModels(aiFormModel);
                aiFormModel.setFields(fields);
                aiModelList.add(aiFormModel);
            }
        }
        visualAiModel.setAiModelList(aiModelList);
        return visualAiModel;
    }

    private static @NotNull List<AiFormFieldModel> getAiFormFieldModels(AiFormModel aiFormModel) {
        List<AiFormFieldModel> fields = new ArrayList<>();
        List<AiFormFieldModel> fieldList = aiFormModel.getFields();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            int index = 1;
            for (int j = 0; j < fieldList.size(); j++) {
                AiFormFieldModel aiFormFieldModel = fieldList.get(j);
                String fieldName = aiFormFieldModel.getFieldName();
                String jnpfKey = aiFormFieldModel.getFieldComponent();
                if (containsChinese(fieldName) || GenerateConstant.containKeyword(fieldName)) {
                    String formatIndex = String.format("%03d", index);
                    String suffix = "_num" + formatIndex;
                    aiFormFieldModel.setFieldName(jnpfKey + suffix);
                    index++;
                }
                List<String> jnpfKeyList = new ArrayList<>();
                for (String s : AiConstants.GEN_MODEL_COMPNENT.split("-")) {
                    if (StringUtils.isNotBlank(s)) jnpfKeyList.add(s.trim());
                }
                if (!jnpfKeyList.contains(jnpfKey)) {
                    aiFormFieldModel.setFieldComponent(JnpfKeyConsts.COM_INPUT);
                }
                //子表控件处理
                if (!Boolean.TRUE.equals(aiFormModel.getIsMain())) {
                    //子表不能有单选框，多选框-调整成下拉框
                    List<String> childNotRadio = new ArrayList<>();
                    childNotRadio.add(JnpfKeyConsts.RADIO);
                    childNotRadio.add(JnpfKeyConsts.CHECKBOX);
                    //子表不能有的其他控件
                    List<String> childNotOther = new ArrayList<>();
                    childNotOther.add(JnpfKeyConsts.TEXTAREA);
                    childNotOther.add(JnpfKeyConsts.LINK);
                    childNotOther.add(JnpfKeyConsts.BUTTON);
                    childNotOther.add(JnpfKeyConsts.ALERT);
                    childNotOther.add(JnpfKeyConsts.BARCODE);
                    childNotOther.add(JnpfKeyConsts.QR_CODE);
                    childNotOther.add(JnpfKeyConsts.EDITOR);
                    if (childNotRadio.contains(jnpfKey)) {
                        aiFormFieldModel.setFieldComponent(JnpfKeyConsts.SELECT);
                    } else if (childNotOther.contains(jnpfKey)) {
                        aiFormFieldModel.setFieldComponent(JnpfKeyConsts.COM_INPUT);
                    }
                }

                if (containsKeyWord(aiFormFieldModel.getFieldName())) {
                    fields.add(aiFormFieldModel);
                }
            }
        }
        return fields;
    }

    /**
     * 判断是否包含中文（字段空或者包含中文字段名称重命名）
     *
     * @param str
     * @return
     */
    private static boolean containsChinese(String str) {
        if (StringUtil.isEmpty(str)) return true;
        Pattern pattern = Pattern.compile("[\u4E00-\u9FA5]");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    /**
     * 字段是否包含特定（用于过滤掉不要的字段如：以_fk结尾是外键，本系统内已自动生成外键）
     *
     * @param str
     * @return
     */
    private static boolean containsKeyWord(String str) {
        return !str.toLowerCase().endsWith("_fk");
    }
}
