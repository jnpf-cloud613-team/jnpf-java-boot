package jnpf.permission.model.action;

import jnpf.permission.entity.ActionEntity;
import lombok.Data;

import java.util.List;

@Data
public class ActionForm {

    private List<ActionEntity> ids;

    private String menuId;

}
