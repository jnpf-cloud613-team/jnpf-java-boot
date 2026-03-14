package jnpf.permission.model.condition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthItemConfig {
    private String jnpfKey;
    private String label;
}
