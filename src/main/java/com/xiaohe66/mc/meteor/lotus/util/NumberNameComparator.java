/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.util;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberNameComparator implements Comparator<String> {
    private static final Pattern PATTERN = Pattern.compile("_(\\d+)_(\\d+)");
    public static final NumberNameComparator INSTANCE = new NumberNameComparator();

    private NumberNameComparator() {
    }

    @Override
    public int compare(String name1, String name2) {
        Matcher matcher1 = PATTERN.matcher(name1);
        String prefix1 = name1;
        int num1 = 0;
        int num2 = 0;
        if (matcher1.find()) {
            prefix1 = name1.substring(0, matcher1.start());
            num1 = Integer.parseInt(matcher1.group(1));
            num2 = Integer.parseInt(matcher1.group(2));
        }
        Matcher matcher2 = PATTERN.matcher(name2);
        String prefix2 = name2;
        int num3 = 0;
        int num4 = 0;
        if (matcher2.find()) {
            prefix2 = name2.substring(0, matcher2.start());
            num3 = Integer.parseInt(matcher2.group(1));
            num4 = Integer.parseInt(matcher2.group(2));
        }
        int result = prefix1.compareTo(prefix2);
        if (result == 0) {
            result = Integer.compare(num1, num3);
            if (result == 0) {
                result = Integer.compare(num2, num4);
            }
        }
        return result;
    }
}
