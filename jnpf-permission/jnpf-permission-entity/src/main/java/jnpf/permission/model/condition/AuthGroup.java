package jnpf.permission.model.condition;

import jnpf.emnus.SearchMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthGroup {
    private String logic = SearchMethodEnum.AND.getSymbol();
    private List<AuthItem> groups;
}
