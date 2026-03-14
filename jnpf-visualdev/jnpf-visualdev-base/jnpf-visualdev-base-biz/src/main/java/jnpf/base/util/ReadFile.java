package jnpf.base.util;

import jnpf.base.model.read.ReadEnum;
import jnpf.base.model.read.ReadListVO;
import jnpf.base.model.read.ReadModel;
import jnpf.util.FileUtil;
import jnpf.util.RandomUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/8/20
 */
@Slf4j
public class ReadFile {

    ReadFile() {
    }

    /**
     * 预览代码
     *
     * @param codePath
     * @return
     */
    public static List<ReadListVO> priviewCode(String codePath) {
        File fileAll = new File(jnpf.util.XSSEscape.escapePath(codePath));
        List<File> fileList = new ArrayList<>();
        if (fileAll.exists()) {
            FileUtil.getFile(fileAll, fileList);
        }
        Map<String, List<ReadModel>> data = new LinkedHashMap<>();
        for (int i = fileList.size() - 1; i >= 0; i--) {
            File file = fileList.get(i);
            String path = file.getAbsolutePath();
            ReadEnum readEnum = ReadEnum.getMessage(path);
            if (readEnum != null) {
                ReadModel readModel = new ReadModel();
                String fileContent = readFile(file);
                readModel.setFileContent(fileContent);
                readModel.setFileName(file.getName());
                readModel.setFileType(readEnum.getMessage());
                readModel.setId(RandomUtil.uuId());
                String folderName = FileUtil.getFileType(file);
                readModel.setFolderName(folderName);
                List<ReadModel> readModelList = data.get(readEnum.getMessage()) != null ? data.get(readEnum.getMessage()) : new ArrayList<>();
                readModelList.add(readModel);
                data.put(readEnum.getMessage(), readModelList);
            }
        }
        List<ReadListVO> list = new ArrayList<>();
        for (Map.Entry<String, List<ReadModel>> item : data.entrySet()) {
            ReadListVO listVO = new ReadListVO();
            listVO.setFileName(item.getKey());
            listVO.setChildren(item.getValue());
            listVO.setId(RandomUtil.uuId());
            list.add(listVO);
        }
        return list;
    }


    /**
     * 读取指定目录下的文件
     *
     * @param path 文件的路径
     * @return 文件内容
     */
    private static String readFile(File path) {
        String fileRead = "";
        try {
            //创建一个输入流对象
            @Cleanup InputStream is = new FileInputStream(path);
            @Cleanup ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            while ((n = is.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            //释放资源
            is.close();
            fileRead = out.toString();
        } catch (IOException e) {
            log.error("代码生成器读取文件报错:" + e.getMessage());
        }
        return fileRead;
    }
}
