package jnpf.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.DocumentShareEntity;
import jnpf.mapper.DocumentShareMapper;
import jnpf.service.DocumentShareService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 邮件配置
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
public class DocumentShareServiceImpl extends SuperServiceImpl<DocumentShareMapper, DocumentShareEntity> implements DocumentShareService {

    @Override
    public DocumentShareEntity getByDocIdAndShareUserId(String docId, String shareUserId) {

        QueryWrapper<DocumentShareEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DocumentShareEntity::getDocumentId, docId);
        queryWrapper.lambda().eq(DocumentShareEntity::getShareUserId, shareUserId);
        List<DocumentShareEntity> list = list(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<DocumentShareEntity> getShareToMe( List<String> strings) {
        QueryWrapper<DocumentShareEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(DocumentShareEntity::getShareUserId, strings);
        return list(queryWrapper);
    }
}
