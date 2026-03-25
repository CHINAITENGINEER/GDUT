package com.campus.task.common.enums;

import lombok.Getter;

/**
 * 接单者等级枚举（参考B站升级规则）
 */
@Getter
public enum UserLevel {
    LV1(1, "新手接单者", 0,     1.00),
    LV2(2, "初级接单者", 200,   0.95),
    LV3(3, "熟练接单者", 1500,  0.90),
    LV4(4, "资深接单者", 4500,  0.85),
    LV5(5, "专业接单者", 10800, 0.80),
    LV6(6, "顶级接单者", 28800, 0.75);

    private final int level;
    private final String levelName;
    private final int requiredExp;    // 升至该级所需累计经验
    private final double feeDiscount; // 手续费折扣率

    UserLevel(int level, String levelName, int requiredExp, double feeDiscount) {
        this.level = level;
        this.levelName = levelName;
        this.requiredExp = requiredExp;
        this.feeDiscount = feeDiscount;
    }

    /**
     * 根据经验值计算当前等级
     */
    public static UserLevel calcLevel(int exp) {
        UserLevel result = LV1;
        for (UserLevel lv : values()) {
            if (exp >= lv.requiredExp) {
                result = lv;
            }
        }
        return result;
    }

    /**
     * 根据等级数字获取枚举
     */
    public static UserLevel of(int level) {
        for (UserLevel lv : values()) {
            if (lv.level == level) return lv;
        }
        return LV1;
    }

    /**
     * 获取下一级（Lv6返回null）
     */
    public UserLevel next() {
        if (this == LV6) return null;
        return values()[this.ordinal() + 1];
    }
}
