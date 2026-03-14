package jnpf.permission.model.user.mod;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.permission.model.user.page.PaginationUser;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class UserIdModelByPage extends PaginationUser {
    /**
     * 用户id集合
     */
    @Schema(description = "用户id集合")
    private Object ids;

    public List<String> getIds() {
        List<String> idList = new ArrayList<>(16);
        if (this.ids != null) {
            if (this.ids instanceof List) {
                List<Object> list = (List<Object>) this.ids;
                Object object = list.isEmpty() ? null : list.get(0);
                if (Objects.nonNull(object) && object instanceof String) {
                    idList.addAll(list.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toList()));
                }
            } else {
                String userIds = (String) this.ids;
                idList.add(userIds);
            }
        }
        return idList;
    }

    @Schema(description = "类型")
    private String type;

}
