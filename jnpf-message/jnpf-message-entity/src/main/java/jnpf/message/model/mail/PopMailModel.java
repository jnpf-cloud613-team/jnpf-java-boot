package jnpf.message.model.mail;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PopMailModel {
    private Integer receiveCount = 0;
    private List<ReceiveModel> mailList = new ArrayList<>();
}
