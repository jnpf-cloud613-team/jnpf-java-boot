package jnpf.base.util.dbutil;

import jnpf.database.constant.DbConst;

import java.util.Random;

/**
 * 表字段相关工具类
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.3
 * @copyright 引迈信息技术有限公司
 * @date 2022-06-08
 */
public class TableUtil {


    TableUtil(){

    }

    private static Random random = new Random();


    /**
     * 随机生成包含大小写字母及数字的字符串
     *
     * @param length
     * @return
     */
    public static String getStringRandom(int length) {
        StringBuilder val = new StringBuilder();
        //参数length，表示生成几位随机数
        for (int i = 0; i < length; i++) {
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            //输出字母还是数字
            if ("char".equalsIgnoreCase(charOrNum)) {
                //输出是大写字母还是小写字母
                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val.append((char) (random.nextInt(26) + temp));
            } else {
                val.append(random.nextInt(10));
            }
        }
        return val.toString();
    }

    /**
     * 检测自带表
     *
     * @param tableName 表明
     * @return ignore
     */
    public static Boolean checkByoTable(String tableName) {
        String[] tables = DbConst.BYO_TABLE.split(",");
        boolean exists;
        for (String table : tables) {
            exists = tableName.toLowerCase().equals(table);
            if (exists) {
                return true;
            }
        }
        return false;
    }

}
