package jnpf.aop;

import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.PdfUtil;
import jnpf.util.XSSEscape;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.aspect.*;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyFileAspect implements FileStorageAspect {


    private final ConfigValueUtil configValueUtil;


    @Override
    public FileInfo uploadAround(UploadAspectChain chain, FileInfo fileInfo, UploadPretreatment pre, FileStorage fileStorage, FileRecorder fileRecorder) {
        checkFilePath(fileInfo);
        if(configValueUtil.isCheckFilePdf()) {
            try {
                String ext = fileInfo.getExt();
                if("pdf".equalsIgnoreCase(ext)) {
                    log.error("检测PDF文件");
                    if (PdfUtil.containsJavaScript(pre.getFileWrapper().getInputStream())) {
                        throw new DataException(MsgCode.FA053.get());
                    }
                }
            } catch (IOException e) {
                log.error("PDF文档解析失败: {}", e.getMessage());
            }
        }
        return FileStorageAspect.super.uploadAround(chain, fileInfo, pre, fileStorage, fileRecorder);
    }

    @Override
    public boolean deleteAround(DeleteAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder) {
        checkFilePath(fileInfo);
        return FileStorageAspect.super.deleteAround(chain, fileInfo, fileStorage, fileRecorder);
    }

    @Override
    public boolean existsAround(ExistsAspectChain chain, FileInfo fileInfo, FileStorage fileStorage) {
        checkFilePath(fileInfo);
        return FileStorageAspect.super.existsAround(chain, fileInfo, fileStorage);
    }

    @Override
    public void downloadAround(DownloadAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        checkFilePath(fileInfo);
        FileStorageAspect.super.downloadAround(chain, fileInfo, fileStorage, consumer);
    }

    @Override
    public void downloadThAround(DownloadThAspectChain chain, FileInfo fileInfo, FileStorage fileStorage, Consumer<InputStream> consumer) {
        checkFilePath(fileInfo);
        FileStorageAspect.super.downloadThAround(chain, fileInfo, fileStorage, consumer);
    }

    private void checkFilePath(FileInfo fileInfo){
        //处理特殊文件名
        fileInfo.setPath(XSSEscape.escapePath(fileInfo.getPath()));
        fileInfo.setFilename(XSSEscape.escapePath(fileInfo.getFilename()));
    }
}
