package jnpf.onlinedev.service.impl;

import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.util.FlowFormDataUtil;
import jnpf.onlinedev.model.OnlineInfoModel;
import jnpf.onlinedev.service.VisualDevInfoService;
import jnpf.onlinedev.util.OnlineSwapDataUtils;
import jnpf.util.FlowFormConstant;
import jnpf.util.JsonUtilEx;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


/**
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/10/26
 */
@Service
@RequiredArgsConstructor
public class VisualDevInfoServiceImpl implements VisualDevInfoService {

    private final OnlineSwapDataUtils onlineSwapDataUtils;
    private final FlowFormDataUtil flowDataUtil;

    @Override
    public VisualdevModelDataInfoVO getEditDataInfo(String id, VisualdevEntity visualdevEntity, OnlineInfoModel model) {
        VisualdevModelDataInfoVO vo = new VisualdevModelDataInfoVO();
        Map<String, Object> editDataInfo = flowDataUtil.getEditDataInfo(visualdevEntity, id, model);
        if (editDataInfo != null && editDataInfo.size() > 0) {
            vo.setId(editDataInfo.get(FlowFormConstant.ID));
            vo.setData(JsonUtilEx.getObjectToString(editDataInfo));
        }
        return vo;
    }


    @Override
    public VisualdevModelDataInfoVO getDetailsDataInfo(String id, VisualdevEntity visualdevEntity) {
        return this.getDetailsDataInfo(id, visualdevEntity, OnlineInfoModel.builder().needRlationFiled(true).needSwap(true).build());
    }

    @Override
    public VisualdevModelDataInfoVO getDetailsDataInfo(String id, VisualdevEntity visualdevEntity, OnlineInfoModel infoModel) {
        return onlineSwapDataUtils.getDetailsDataInfo(id, visualdevEntity, infoModel);
    }

}
