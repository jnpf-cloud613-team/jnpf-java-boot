package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.constant.MsgCode;
import jnpf.entity.FileEntity;
import jnpf.base.ActionResult;
import jnpf.base.vo.PaginationVO;
import jnpf.mapper.FileMapper;
import jnpf.model.YozoFileParams;
import jnpf.service.YozoService;
import jnpf.utils.SplicingUrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/5/13
 */
@Service
@RequiredArgsConstructor
public class YozoServiceImpl extends SuperServiceImpl<FileMapper,FileEntity> implements YozoService {

    private final FileMapper fileMapper;

    public static final String F_FILE_VERSION = "F_FileVersion";

    @Override
    public String getPreviewUrl(YozoFileParams params) {
        return SplicingUrlUtil.getPreviewUrl(params);
    }

    @Override
    public ActionResult<Object> saveFileId(String fileVersionId, String fileId, String fileName) {
        FileEntity fileEntity =new FileEntity();
        fileEntity.setId(fileId);
        fileEntity.setFileName(fileName);
        fileEntity.setFileVersionId(fileVersionId);
        fileEntity.setType("create");
        this.save(fileEntity);

        return ActionResult.success(MsgCode.SU001.get());
    }

    @Override
    public FileEntity selectByName(String fileNa) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(FileEntity::getFileName,fileNa);
        return this.getOne(wrapper);
    }

    @Override
    public ActionResult<Object> saveFileIdByHttp(String fileVersionId, String fileId, String fileUrl) {
        String fileName = "";
        String url = "";
        String name = "";
        try {
            url = URLDecoder.decode(fileUrl, "UTF-8");
            if (url.contains("/")) {
                fileName = url.substring(url.lastIndexOf("/") + 1);
            } else {
                fileName = url.substring(url.lastIndexOf("\\") + 1);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //同一url文件数
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("F_Url", url);
        Long total = fileMapper.selectCount(wrapper);
        if (total == 0) {
            name = fileName;
        } else {
            String t = total.toString();
            name = fileName + "(" + t + ")";
        }
        FileEntity fileEntity = new FileEntity();
        fileEntity.setType(url.contains("http") ? "http" : "local");
        fileEntity.setFileVersionId(fileVersionId);
        fileEntity.setId(fileId);
        fileEntity.setFileName(name);
        fileEntity.setUrl(url);
        fileMapper.insert(fileEntity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Override
    public ActionResult<Object> deleteFileByVersionId(String versionId) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();

        wrapper.eq(F_FILE_VERSION, versionId);
        int i = fileMapper.delete(wrapper);
        if (i == 1) {
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    @Override
    public FileEntity selectByVersionId(String fileVersionId) {
        QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq(F_FILE_VERSION, fileVersionId);
        return fileMapper.selectOne(wrapper);
    }

    @Override
    public ActionResult<Object> deleteBatch(String[] versions) {
        for (String version : versions) {
            QueryWrapper<FileEntity> wrapper = new QueryWrapper<>();
            wrapper.eq(F_FILE_VERSION, version);
            int i = fileMapper.delete(wrapper);
            if (i == 0) {
                return ActionResult.fail(MsgCode.FA045.get(version));
            }
        }
        return ActionResult.success(MsgCode.SU003.get());

    }

    @Override
    public void editFileVersion(String oldFileId, String newFileId) {
        UpdateWrapper<FileEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq(F_FILE_VERSION, oldFileId);
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileVersionId(newFileId);
        fileEntity.setOldFileVersionId(oldFileId);
        fileMapper.update(fileEntity, wrapper);
    }

    @Override
    public List<FileEntity> getAllList(PaginationVO pageModel) {
        Page<FileEntity> page = new Page<>(pageModel.getCurrentPage(), pageModel.getPageSize());
        IPage<FileEntity> iPage = fileMapper.selectPage(page, null);
        return iPage.getRecords();
    }

}
