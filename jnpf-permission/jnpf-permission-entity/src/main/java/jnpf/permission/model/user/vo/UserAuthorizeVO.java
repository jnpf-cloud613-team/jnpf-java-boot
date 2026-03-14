package jnpf.permission.model.user.vo;

import jnpf.permission.model.user.mod.UserAuthorizeModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthorizeVO {
    private List<UserAuthorizeModel> button;
    private List<UserAuthorizeModel> column;
    private List<UserAuthorizeModel> module;
    private List<UserAuthorizeModel> resource;
    private List<UserAuthorizeModel> form;
    private List<UserAuthorizeModel> portal;
    private List<UserAuthorizeModel> flow;
    private List<UserAuthorizeModel> print;
}
