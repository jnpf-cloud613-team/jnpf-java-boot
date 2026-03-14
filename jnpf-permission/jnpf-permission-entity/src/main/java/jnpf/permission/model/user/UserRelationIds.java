package jnpf.permission.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 用户所有绑定信息
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/31 10:02:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRelationIds {
    @Builder.Default
    private List<String> group = Collections.emptyList();
    @Builder.Default
    private List<String> organize = Collections.emptyList();
    @Builder.Default
    private List<String> position = Collections.emptyList();
    @Builder.Default
    private List<String> role = Collections.emptyList();
}
