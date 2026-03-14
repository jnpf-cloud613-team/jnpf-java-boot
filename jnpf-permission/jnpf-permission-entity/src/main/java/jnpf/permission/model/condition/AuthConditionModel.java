package jnpf.permission.model.condition;

import jnpf.emnus.SearchMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthConditionModel {
    private String matchLogic = SearchMethodEnum.AND.getSymbol();
    private List<AuthGroup> conditionList;
}
