package jnpf.service.impl;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.URLUtil;
import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpServletResponse;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.GlobalConst;
import jnpf.constant.MsgCode;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.model.*;
import jnpf.service.FileService;
import jnpf.util.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private List<String> whiteImageFolder = Arrays.asList(FileTypeConstant.USERAVATAR.toLowerCase(), FileTypeConstant.BIVISUALPATH.toLowerCase());

    public static final List<String> IMAGE_PATH = ImmutableList.of("/api/file/Image/");


    private final ConfigValueUtil configValueUtil;


    @Override
    public String getPath(String type) {
        return FilePathUtil.getFilePath(type);
    }

    @Override
    public String getLocalBasePath() {
        return FileUploadUtils.getLocalBasePath();
    }

    @Override
    public UploaderVO uploadFile(MergeChunkDto mergeChunkDto, MultipartFile file) {
        String fileType = UpUtil.getFileType(file);
        if (StringUtil.isEmpty(fileType)) {
            fileType = mergeChunkDto.getFileType();
        }
        //验证类型
        if (!OptimizeUtil.fileType(configValueUtil.getAllowUploadFileType(), fileType)) {
            throw new DataException(MsgCode.FA017.get());
        }
        PathTypeModel pathTypeModel = new PathTypeModel();
        pathTypeModel.setPathType(mergeChunkDto.getPathType());
        pathTypeModel.setTimeFormat(mergeChunkDto.getTimeFormat());
        pathTypeModel.setSortRule(mergeChunkDto.getSortRule());
        pathTypeModel.setFolder(mergeChunkDto.getFolder());

        if ("selfPath".equals(pathTypeModel.getPathType()) && StringUtil.isNotEmpty(pathTypeModel.getFolder())) {

            String folder = pathTypeModel.getFolder();
            folder = folder.replace("\\\\", "/");
            //文件夹名以字母或数字开头，由字母、数字、下划线和连字符组成，长度不超过100个字符
            String regex = "^[a-zA-Z0-9][a-zA-Z0-9_\\-\\\\\\/]{0,99}$";
            if (!folder.matches(regex)) {
                throw new DataException(MsgCode.FA038.get());
            }

        }
        //实际文件名
        String fileName = DateUtil.dateNow("yyyyMMdd") + "_" + RandomUtil.uuId() + "." + fileType;
        //文件上传路径
        String type = mergeChunkDto.getType();

        //文件自定义路径相对路径
        StringBuilder relativeFilePath = new StringBuilder();
        if (pathTypeModel != null && "selfPath".equals(pathTypeModel.getPathType()) && pathTypeModel.getSortRule() != null) {
            // 按路径规则顺序构建生成目录
            String sortRule = pathTypeModel.getSortRule();
            List<String> rules = null;
            if (sortRule.contains("[")) {
                rules = JsonUtil.getJsonToList(sortRule, String.class);
            } else {
                rules = Arrays.asList(pathTypeModel.getSortRule().split(","));
            }
            for (String rule : rules) {
                // 按用户存储
                if ("1".equals(rule)) {
                    UserInfo userInfo = UserProvider.getUser();
                    relativeFilePath.append(userInfo.getUserAccount()).append("/");
                }
                // 按照时间格式
                else if (StringUtil.isNotEmpty(pathTypeModel.getTimeFormat()) && "2".equals(rule)) {
                    String timeFormat = pathTypeModel.getTimeFormat();
                    timeFormat = timeFormat.replace("YYYY", "yyyy");
                    timeFormat = timeFormat.replace("DD", "dd");
                    LocalDate currentDate = LocalDate.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
                    String currentDateStr = currentDate.format(formatter);
                    relativeFilePath.append(currentDateStr);
                    if (!currentDateStr.endsWith("/")) {
                        relativeFilePath.append("/");
                    }
                }
                // 按自定义目录
                else if (StringUtil.isNotEmpty(pathTypeModel.getFolder()) && "3".equals(rule)) {
                    String folder = pathTypeModel.getFolder();
                    folder = folder.replace("\\\\", "/");
                    relativeFilePath.append(folder);
                    if (!folder.endsWith("/")) {
                        relativeFilePath.append("/");
                    }
                }
            }

            if (StringUtil.isNotEmpty(relativeFilePath.toString())) {
                relativeFilePath = new StringBuilder(StringUtil.replaceMoreStrToOneStr(relativeFilePath.toString(), "/"));
                if (relativeFilePath.toString().startsWith("/")) {
                    relativeFilePath = new StringBuilder(relativeFilePath.substring(1));
                }
                fileName = relativeFilePath.toString().replace("/", ",") + fileName;
            }
        }


        UploaderVO vo = UploaderVO.builder().fileSize(file.getSize()).fileExtension(fileType).build();
        FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(type, fileName).setThumbnail(true), file);
        fileName = fileInfo.getFilename();
        String thFilename = fileInfo.getThFilename();
        if (!StringUtil.isNotEmpty(thFilename)) {
            //小图没有压缩直接用原图
            thFilename = fileName;
        }
        //自定义文件实际文件名
        if (StringUtil.isNotEmpty(relativeFilePath.toString())) {
            fileName = relativeFilePath.toString().replace("/", ",") + fileName;
            thFilename = relativeFilePath.toString().replace("/", ",") + thFilename;
        }
        vo.setName(fileName);

        vo.setUrl(UploaderUtil.uploaderImg(IMAGE_PATH.get(0) + type + "/", fileName));
        vo.setThumbUrl(UploaderUtil.uploaderImg(IMAGE_PATH.get(0) + type + "/", thFilename));


        return vo;
    }

    @Override
    public ChunkRes checkChunk(Chunk chunk) {
        String type = chunk.getExtension();
        if (!OptimizeUtil.fileType(configValueUtil.getAllowUploadFileType(), type)) {
            throw new DataException(MsgCode.FA017.get());
        }
        String identifier = chunk.getIdentifier();
        String path = getPath(FileTypeConstant.TEMPORARY);
        String filePath = XSSEscape.escapePath(path + identifier);
        List<File> chunkFiles = FileUtil.getFile(new File(FileUploadUtils.getLocalBasePath() + filePath));
        List<Integer> existsChunk = chunkFiles.stream().filter(f -> {
            if (f.getName().endsWith(".tmp")) {
                FileUtils.deleteQuietly(f);
                return false;
            } else {
                return f.getName().startsWith(identifier);
            }
        }).map(f -> Integer.parseInt(f.getName().replace(chunk.getIdentifier().concat("-"), ""))).collect(Collectors.toList());
        return ChunkRes.builder().merge(chunk.getTotalChunks().equals(existsChunk.size())).chunkNumbers(existsChunk).build();
    }

    @Override
    public ChunkRes uploadChunk(Chunk chunk, MultipartFile file) {
        String type = chunk.getExtension();
        if (!OptimizeUtil.fileType(configValueUtil.getAllowUploadFileType(), type)) {
            throw new DataException(MsgCode.FA017.get());
        }
        ChunkRes chunkRes = ChunkRes.builder().build();
        chunkRes.setMerge(false);
        File chunkFile = null;
        File chunkTmpFile = null;
        try {
            String filePath = FileUploadUtils.getLocalBasePath() + getPath(FileTypeConstant.TEMPORARY);
            Integer chunkNumber = chunk.getChunkNumber();
            String identifier = XSSEscape.escapePath(chunk.getIdentifier());
            String chunkTempPath = XSSEscape.escapePath(filePath + identifier);
            File path = new File(chunkTempPath);
            if (!path.exists()) {
                path.mkdirs();
            }
            String chunkName = XSSEscape.escapePath(identifier.concat("-") + chunkNumber);
            String chunkTmpName = XSSEscape.escapePath(chunkName.concat(".tmp"));
            chunkFile = new File(chunkTempPath, chunkName);
            chunkTmpFile = new File(chunkTempPath, chunkTmpName);
            if (chunkFile.exists() && chunkFile.length() == chunk.getCurrentChunkSize()) {
                log.info("该分块已经上传：" + chunkFile.getName());
            } else {
                @Cleanup InputStream inputStream = file.getInputStream();
                FileUtils.copyInputStreamToFile(inputStream, chunkTmpFile);
                boolean b = chunkTmpFile.renameTo(chunkFile);
                if (b) {
                    log.info("成功" + chunkFile.getName());
                } else {
                    log.info("失败" + chunkFile.getName());
                }

            }
            int existsSize = (int) FileUtil.getFile(new File(chunkTempPath)).stream().filter(f ->
                    f.getName().startsWith(identifier) && !f.getName().endsWith(".tmp")
            ).count();
            chunkRes.setMerge(Objects.equals(existsSize, chunk.getTotalChunks()));
        } catch (Exception e) {
            try {
                FileUtils.deleteQuietly(chunkTmpFile);
                FileUtils.deleteQuietly(chunkFile);
            } catch (Exception ee) {
                e.printStackTrace();
            }
            log.info("上传异常：" + e);
            throw new DataException(MsgCode.FA033.get());
        }
        return chunkRes;
    }

    @Override
    public UploaderVO mergeChunk(MergeChunkDto mergeChunkDto) {
        String identifier = XSSEscape.escapePath(mergeChunkDto.getIdentifier());
        String path = FileUploadUtils.getLocalBasePath() + getPath(FileTypeConstant.TEMPORARY);
        String filePath = XSSEscape.escapePath(path + identifier);
        String uuid = RandomUtil.uuId();
        String partFile = XSSEscape.escapePath(path + uuid + "." + mergeChunkDto.getExtension());
        UploaderVO vo = UploaderVO.builder().build();
        try {
            @Cleanup FileOutputStream destTempfos = new FileOutputStream(partFile);
            List<File> mergeFileList = FileUtil.getFile(new File(filePath));
            for (int i = 0; i < mergeFileList.size(); i++) {
                String chunkName = identifier.concat("-") + (i + 1);
                File files = new File(filePath, chunkName);
                if (files.exists()) {
                    FileUtils.copyFile(files, destTempfos);
                }
            }
            File partFiles = new File(partFile);
            if (partFiles.exists()) {
                MultipartFile multipartFile = FileUtil.createFileItem(partFiles);
                vo = uploadFile(mergeChunkDto, multipartFile);
                FileUtil.deleteTmp(multipartFile);
            }
            destTempfos.close();
            FileUtils.deleteQuietly(new File(filePath));
            FileUtils.deleteQuietly(new File(partFile));
        } catch (IOException e) {
            log.error("合并分片失败: {}", e.getMessage());
            throw new DataException(MsgCode.FA033.get());
        }
        return vo;
    }

    @Override
    public void downloadFile(String encryption, String downName) {
        String fileNameAll = DesUtil.aesDecode(encryption);
        if (!StringUtil.isEmpty(fileNameAll)) {
            fileNameAll = fileNameAll.replace("\n", "");
            String[] data = fileNameAll.split("#");
            String cacheKEY = data.length > 0 ? data[0] : "";
            String fileName = XSSEscape.escapePath(data.length > 1 ? data[1] : "");
            String type = data.length > 2 ? data[2] : "";
            Object ticketObj = TicketUtil.parseTicket(cacheKEY);
            fileName = URLUtil.decode(fileName, GlobalConst.DEFAULT_CHARSET);
            FileParameter fileParameter = new FileParameter(type, fileName);
            //验证缓存
            if (ticketObj != null) {
                //某些手机浏览器下载后会有提示窗口, 会访问两次、三次下载地址，请求头不是app类型
                TicketUtil.updateTicket(cacheKEY, "1", 10L);
                FileUploadUtils.downloadFile(fileParameter, inputStream ->
                        FileDownloadUtil.outFile(inputStream, downName));
                if (FileTypeConstant.FILEZIPDOWNTEMPPATH.equals(type)) { //删除打包的临时文件，释放存储
                    FileUploadUtils.deleteFileByPathAndFileName(fileParameter);
                }
            } else {
                if (FileTypeConstant.FILEZIPDOWNTEMPPATH.equals(type)) { //删除打包的临时文件，释放存储
                    FileUploadUtils.deleteFileByPathAndFileName(fileParameter);
                }
                throw new DataException(MsgCode.FA039.get());
            }
        }
    }

    @Override
    public boolean fileExists(String path, String fileName) {
        return FileUploadUtils.exists(new FileParameter(path, fileName));
    }

    @Override
    public String previewFile(PreviewParams previewParams) {
        String fileName = XSSEscape.escape(previewParams.getFileName());
        //读取允许文件预览类型
        String fileType = UpUtil.getFileType(fileName);
        if (!OptimizeUtil.fileType(configValueUtil.getAllowUploadFileType(), fileType)) {
            throw new DataException(MsgCode.FA040.get());
        }

        //解析文件url 获取类型
        String type = null;

        String fileNameAll = previewParams.getFileDownloadUrl();
        if (!StringUtil.isEmpty(fileNameAll)) {
            String[] data = fileNameAll.split("/");
            if (data.length > 4) {
                type = data[4];
            } else {
                type = "";
            }

        }
        String url;
        //文件预览策略
        if ("yozo".equals(configValueUtil.getPreviewType())) {
            url = "";
        } else {
            String downFileName = fileName;
            if (downFileName.contains(",")) {
                downFileName = downFileName.substring(downFileName.lastIndexOf(",") + 1);
            }
            url = configValueUtil.getApiDomain() + IMAGE_PATH.get(0) + type + "/" + fileName + "?fullfilename=" + downFileName + "&s=" + UserProvider.getUser().getSecurityKey();
            //encode编码
            String fileUrl = Base64.encodeBase64String(url.getBytes());
            url = configValueUtil.getKkFileUrl() + "onlinePreview?url=" + fileUrl;
        }
        return url;
    }

    @Override
    public void flushFile(String type, String fileName, String securityKey, boolean redirect) {
        //页面播放视频的情况下取消重定向
        boolean isPlayVideo = StringUtil.isNotEmpty(ServletUtil.getHeader(HttpHeaders.RANGE));
        //目录校验, 开放UserAvatar、BiVisualPath
        if (!whiteImageFolder.contains(type.toLowerCase())) {
            if (StringUtil.isEmpty(securityKey)) {
                throw new DataException(MsgCode.FA039.get());
            }
            String ticket = DesUtil.aesOrDecode(securityKey, false, true);
            String token = TicketUtil.parseTicket(ticket);
            if (token == null) {
                throw new DataException(MsgCode.FA039.get());
            }
            UserInfo user = UserProvider.getUser(token);
            if (user.getUserId() == null) {
                TicketUtil.deleteTicket(ticket);
                throw new DataException(MsgCode.FA039.get());
            }
        }
        if (!isPlayVideo && StringUtil.isNotEmpty(securityKey) && redirect) {
            String downName = fileName;
            if (downName.contains(",")) {
                downName = downName.substring(downName.lastIndexOf(",") + 1);
            }
            String urlEncodeFileName = URLEncodeUtil.encode(fileName, GlobalConst.DEFAULT_CHARSET);
            String urlEncodeDownName = URLEncodeUtil.encode(downName, GlobalConst.DEFAULT_CHARSET);
            String url = configValueUtil.getApiDomain() + UploaderUtil.uploaderFile(urlEncodeFileName + "#" + type) + "&name=" + urlEncodeDownName + "&fullfilename=" + urlEncodeDownName;
            try {
                ServletUtil.getResponse().sendRedirect(url);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            FileUploadUtils.downloadFile(new FileParameter(type, fileName), inputStream -> {
                if (isPlayVideo) {
                    FileInfo remoteFileInfo = FileUploadUtils.getRemoteFileInfo(new FileParameter(type, fileName));
                    streamVideo(inputStream, remoteFileInfo.getFilename(), remoteFileInfo.getSize());
                } else {
                    FileDownloadUtil.flushFile(inputStream, fileName);
                }
            });
        }
    }


    public void streamVideo(InputStream input, String fileName, long length) {
        HttpServletResponse response = ServletUtil.getResponse();
        String rangeHeader = ServletUtil.getHeader(HttpHeaders.RANGE);
        final long CHUNK_SIZE = 1024L * 1024L; // 1MB
        String range = rangeHeader.replace("bytes=", "");
        String[] ranges = range.split("-");

        long start = Long.parseLong(ranges[0]);
        long end;

        if (ranges.length > 1 && !ranges[1].isEmpty()) {
            // 客户端明确指定了结束位置：bytes=start-end
            end = Long.parseLong(ranges[1]);
        } else {
            // 客户端只指定了开始位置：bytes=start-
            end = start + CHUNK_SIZE - 1;
        }
        // 确保不超出文件边界
        end = Math.min(end, length - 1);

        // 边界检查
        if (start >= length || end < start) {
            response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
            response.setHeader("Content-Range", "bytes */" + length);
            return;
        }
        long contentLength = end - start + 1;

        if (fileName == null) {
            fileName = ".mp4";
        }
        // 设置响应头
        response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
        response.setContentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.ALL).toString());
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
        response.setHeader("Accept-Ranges", "bytes");

        try {
            // 注意：end + 1 是因为copyRange的结束位置是不包含的
            StreamUtils.copyRange(input, response.getOutputStream(), start, end + 1);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
