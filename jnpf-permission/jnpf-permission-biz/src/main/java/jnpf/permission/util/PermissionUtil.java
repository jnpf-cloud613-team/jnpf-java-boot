package jnpf.permission.util;

import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/1/27
 */
public class PermissionUtil {
    PermissionUtil() {
    }

    /**
     * 递归取组织结构id
     *
     * @param organizeInfo 组织信息集合
     * @param organizeId   组织id
     * @param infoType     信息类型 1:id 2:fullName
     */
    private static LinkedList<String> getOrganizeInfos(LinkedList<String> organizeInfo, String organizeId, Integer infoType, OrganizeService organizeService) {
        OrganizeEntity infoEntity = organizeService.getInfo(organizeId);
        if (infoEntity != null) {
            organizeInfo.add(infoType.equals(1) ? organizeId : infoEntity.getFullName());
            // -1 为顶级节点
            if (!"-1".equals(infoEntity.getParentId())) {
                getOrganizeInfos(organizeInfo, infoEntity.getParentId(), infoType, organizeService);
            } else {
                // 结束时，进行倒序排列
                Collections.reverse(organizeInfo);
            }
        }
        return organizeInfo;
    }

    public static List<LinkedList<String>> getOrgIdsTree(List<String> organizeIds, Integer infoType, OrganizeService organizeService) {
        List<LinkedList<String>> organizeIdsTree = new ArrayList<>();
        organizeIds.forEach(id -> organizeIdsTree.add(getOrganizeInfos(new LinkedList<>(), id, infoType, organizeService)));
        return organizeIdsTree;
    }

    /**
     * 获取组名连接信息
     *
     * @param organizeIds 组织id集合
     * @return 组织链式信息
     */
    public static String getLinkInfoByOrgId(List<String> organizeIds, OrganizeService organizeService) {
        StringBuilder organizeInfoVo = new StringBuilder();
        for (String id : organizeIds) {
            if (id != null) {
                StringBuilder organizeInfo = new StringBuilder();
                for (String name : getOrganizeInfos(new LinkedList<>(), id, 2, organizeService)) {
                    organizeInfo.append(name).append("/");
                }
                // 去除最后一个斜杠
                if (organizeInfo.length() > 0) {
                    organizeInfo = new StringBuilder(organizeInfo.substring(0, organizeInfo.length() - 1));
                }
                organizeInfo.append(",");
                organizeInfoVo.append(organizeInfo);
            }
        }
        return organizeInfoVo.toString();
    }

    /**
     * 获取组名连接信息
     *
     * @param organizeId 组织id
     * @return 组织链式信息
     */
    public static String getLinkInfoByOrgId(String organizeId, OrganizeService organizeService) {
        return getLinkInfoByOrgId(Collections.singletonList(organizeId), organizeService);
    }

    /**
     * 去掉尾部的封号
     */
    public static String getLinkInfoByOrgId(String organizeId, OrganizeService organizeService, boolean separateFlag) {
        String linkInfo = getLinkInfoByOrgId(organizeId, organizeService);
        if (StringUtil.isEmpty(linkInfo)) {
            return linkInfo;
        }
        if (!separateFlag) {
            linkInfo = linkInfo.substring(0, linkInfo.length() - 1);
        }
        return linkInfo;
    }

}
