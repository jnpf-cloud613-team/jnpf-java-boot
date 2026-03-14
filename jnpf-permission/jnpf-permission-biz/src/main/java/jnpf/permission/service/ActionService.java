package jnpf.permission.service;


import jnpf.base.service.SuperService;
import jnpf.permission.entity.ActionEntity;
import jnpf.permission.model.action.ActionPagination;

import java.util.List;

public interface ActionService extends SuperService<ActionEntity> {

    Boolean insertOrUpdate(ActionEntity actionEntity);

    Boolean deleteById(String actionId);

    List<ActionEntity> getActionList(ActionPagination actionPagination);



}
