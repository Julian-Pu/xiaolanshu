package com.xiaolanshu.search;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.Locale;

/**
 * 拼音工具（基于 pinyin4j，离线）：
 * - toPinyin(char)：单字 → 小写无音调拼音（非汉字返回字符本身）
 * - toPinyin(String, sep)：整串转换
 */
public final class PinyinUtil {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();
    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private PinyinUtil() {
    }

    public static String toPinyin(char c) {
        if (c < 128) {
            return String.valueOf(c).toLowerCase(Locale.ROOT);
        }
        try {
            String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
            return arr == null || arr.length == 0 ? String.valueOf(c) : arr[0];
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return String.valueOf(c);
        }
    }

    public static String toPinyin(String text, String sep) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(toPinyin(c)).append(sep);
        }
        String out = sb.toString();
        return sep.isEmpty() ? out : out.substring(0, out.length() - sep.length());
    }
}
