package jnpf.generater.service.impl;

import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.mapper.VisualdevMapper;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.base.service.VisualAliasService;
import jnpf.base.util.VisualUtils;
import jnpf.base.util.common.AliasModel;
import jnpf.base.util.common.GenerateCommon;
import jnpf.base.util.common.GenerateParamModel;
import jnpf.base.util.custom.VelocityEnum;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.GenerateConstant;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.DataSourceUtil;
import jnpf.entity.FileParameter;
import jnpf.generater.factory.CodeGenerateFactoryV3;
import jnpf.generater.model.TemplateMethodEnum;
import jnpf.generater.service.VisualdevGenService;
import jnpf.model.FileListVO;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.TableModel;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.app.Velocity;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * 可视化开发功能表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-04-02
 */
@Service
@RequiredArgsConstructor
public class VisualdevGenServiceImpl extends SuperServiceImpl<VisualdevMapper, VisualdevEntity> implements VisualdevGenService {

    private final DataSourceUtil dataSourceUtil;
    private final DbLinkService dataSourceApi;
    private final ConfigValueUtil configValueUtil;
    private final CodeGenerateFactoryV3 generateFactoryV3;
    private final SysconfigService sysconfigService;
    private final VisualAliasService aliasService;

    // 模板修改时间缓存
    private Map<String, String> templateCache = new HashMap<>();

    /**
     * vue3代码生成
     *
     * @param entity           可视化开发功能
     * @param downloadCodeForm 下载相关信息
     * @return
     * @throws Exception
     */
    @Override
    public String codeGengerateV3(VisualdevEntity entity, DownloadCodeForm downloadCodeForm) {
        UserInfo userInfo = UserProvider.getUser();
        DbLinkEntity linkEntity = null;
        if (entity != null) {
            String localBasePath = GenerateCommon.getLocalBasePath() + configValueUtil.getTemplateCodePathVue3();
            //初始化模板
            Velocity.reset();
            VelocityEnum.INIT.initVelocity(localBasePath);

            //视图代码生成独立逻辑
            if (VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
                return viewCode(entity, downloadCodeForm, localBasePath, userInfo);
            }
            // 是否存在关联数据库
            if (StringUtil.isNotEmpty(entity.getDbLinkId())) {
                linkEntity = dataSourceApi.getInfo(entity.getDbLinkId());
            }
            // 是否存在关联表
            if (StringUtil.isNotEmpty(entity.getVisualTables())) {
                String fileName = entity.getFullName() + "_" + DateUtil.nowDateTime();

                List<TableModel> list = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
                Map<String, AliasModel> tableAliasMap = aliasService.getAllFiledsAlias(entity.getId());

                //获取主表
                String mainTable = list.stream().filter(t -> "1".equals(t.getTypeId())).findFirst().orElse(new TableModel()).getTable();
                Map<String, String> fieldsMap = tableAliasMap.get(mainTable).getFieldsMap();
                //获取主键
                String pKeyName = VisualUtils.getpKey(linkEntity, mainTable);

                //自定义包名
                String modulePackageName = StringUtil.isNotEmpty(downloadCodeForm.getModulePackageName()) ? downloadCodeForm.getModulePackageName() :
                        GenerateConstant.PACKAGE_NAME;
                downloadCodeForm.setModulePackageName(modulePackageName);
                downloadCodeForm.setMainClassName(tableAliasMap.get(mainTable).getAliasName());
                //获取其他子表的主键
                Map<String, Object> childpKeyMap = new HashMap<>(16);
                for (TableModel tableModel : list) {
                    String childKey = VisualUtils.getpKey(linkEntity, tableModel.getTable());
                    if (childKey.length() > 2 && "f_".equalsIgnoreCase(childKey.substring(0, 2))) {
                        childKey = childKey.substring(2);
                    }
                    childpKeyMap.put(tableModel.getTable(), childKey);
                }

                String templatesPath = null;
                //功能表单
                if (OnlineDevData.FORM_TYPE_DEV.equals(entity.getType())) {
                    switch (entity.getWebType()) {
                        case 1:
                            templatesPath = downloadCodeForm.getEnableFlow() == 1 ? TemplateMethodEnum.T5.getMethod() : TemplateMethodEnum.T4.getMethod();
                            break;
                        case 2:
                            templatesPath = downloadCodeForm.getEnableFlow() == 1 ? TemplateMethodEnum.T3.getMethod() : TemplateMethodEnum.T2.getMethod();
                            break;
                        default:
                            break;
                    }
                }

                //模版下载到当前服务器
                downloadLocal(localBasePath);

                //执行代码生成器
                GenerateParamModel generateParamModel = GenerateParamModel.builder()
                        .dataSourceUtil(dataSourceUtil)
                        .path(localBasePath)
                        .fileName(fileName)
                        .templatesPath(templatesPath)
                        .downloadCodeForm(downloadCodeForm)
                        .entity(entity)
                        .userInfo(userInfo)
                        .configValueUtil(configValueUtil)
                        .linkEntity(linkEntity)
                        .pKeyNameOriginal(pKeyName)
                        .pKeyName(fieldsMap.get(pKeyName))
                        .template7Model(GenerateCommon.getTemplate7Model(sysconfigService.getList(GenerateConstant.SYSCONFIG)))
                        .tableAliseMap(tableAliasMap)
                        .build();
                generateFactoryV3.runGenerator(templatesPath, generateParamModel);
                return fileName;
            }
        }
        return null;
    }

    //视图代码
    private @NotNull String viewCode(VisualdevEntity entity, DownloadCodeForm downloadCodeForm, String localBasePath, UserInfo userInfo) {
        String fileName = entity.getFullName() + "_" + DateUtil.nowDateTime();
        String mainClass = "St" + entity.getEnCode();
        downloadCodeForm.setMainClassName(mainClass);
        //执行代码生成器
        GenerateParamModel generateParamModel = GenerateParamModel.builder()
                .path(localBasePath)
                .fileName(fileName)
                .downloadCodeForm(downloadCodeForm)
                .entity(entity)
                .userInfo(userInfo)
                .configValueUtil(configValueUtil)
                .className(mainClass)
                .templatesPath(TemplateMethodEnum.T6.getMethod())
                .template7Model(GenerateCommon.getTemplate7Model(sysconfigService.getList(GenerateConstant.SYSCONFIG)))
                .build();
        generateFactoryV3.runGenerator(TemplateMethodEnum.T6.getMethod(), generateParamModel);
        return fileName;
    }

    //非本地模板需要下载-获取模板如下
    private void downloadLocal(String localBasePath) {
        if (!FileUploadUtils.getDefaultPlatform().startsWith(GenerateConstant.LOCAL)) {
            List<FileListVO> fileList = new ArrayList<>();
            fileList.addAll(FileUploadUtils.getFileList(new FileParameter().setRemotePath(FileTypeConstant.TEMPLATECODEPATHV3).setRecursive(true)));
            for (FileListVO fileListVO : fileList) {
                String fName = fileListVO.getFileName();
                String fPath = fileListVO.getFilePath();
                String cacheKey = fPath + fName;
                File localFile = new File(localBasePath + fPath + fName);
                String lastTime = templateCache.get(cacheKey);
                if (localFile.exists() && Objects.equals(lastTime, fileListVO.getFileTime())) {
                    continue;
                }
                templateCache.put(cacheKey, fileListVO.getFileTime());
                FileUploadUtils.downloadFileToLocal(new FileParameter(fPath, fName));
            }
        }
    }
}
