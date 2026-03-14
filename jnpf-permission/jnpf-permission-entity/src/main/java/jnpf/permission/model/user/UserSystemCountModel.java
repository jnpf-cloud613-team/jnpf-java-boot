package jnpf.permission.model.user;

import jnpf.base.Pagination;
import lombok.Data;

import java.util.List;

@Data
public class UserSystemCountModel {

    private Pagination pagination;
    //是否过滤禁用用户
    private boolean filter;
    //用户范围
    private List<String> userIds;



    public UserSystemCountModel(Pagination pagination, boolean filter, List<String> userIds) {
        this.pagination = pagination;
        this.filter = filter;
        this.userIds = userIds;
    }


}
