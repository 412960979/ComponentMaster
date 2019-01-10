package com.wn.gradle;


import com.google.gson.JsonParser;

public class JsonUtils {
    private JsonUtils() {
    }

    /**
     * 压缩json<br/>
     * 将格式化的json字符串压缩为一行，去掉空格、tab，并把换行符改为显式的\r\n <br/>
     * ！！！只能处理正确json字符串，不对json字符串做校验
     *
     * @param json
     * @return
     */
    public static String compressJson(String json) {
        if (json == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean skip = true;// true 允许截取(表示未进入string双引号)
        boolean escaped = false;// 转义符
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            escaped = !escaped && c == '\\';
            if (skip) {
                if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                    continue;
                }
            }
            sb.append(c);
            if (c == '"') {
                skip = !skip;
            }
        }
        return sb.toString().replaceAll("\r\n", "\\\\r\\\\n");
    }

    public static boolean isJsonString(String json) {
        try {
            new JsonParser().parse(json);
            return true;
        } catch (Throwable e) {
            System.out.println("resHook====================" + e.toString());
            return false;
        }
    }
}
