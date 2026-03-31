package com.campus.task.module.recommendation.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务标签提取工具类。
 *
 * <p>使用 HanLP 便携版对任务标题和描述进行中文分词，
 * 仅保留名词（nature 以 'n' 开头），过滤停用词、单字词、纯数字，
 * 去重后返回干净的标签列表，替代原来基于正则 split 的粗糙方案。
 *
 * <p>降级策略：若 HanLP 初始化失败（如资源文件缺失），自动回退到
 * 正则分词 + 停用词过滤，保证系统可用性。
 */
public class TaskTagExtractor {

    /** 最多提取的标签数量 */
    private static final int MAX_TAGS = 20;

    /** 最短有效词长度（字符数）*/
    private static final int MIN_WORD_LEN = 2;

    /**
     * 扩展停用词表：除 HanLP 自带停用词外，过滤校园任务场景下的无意义高频词。
     * HanLP 便携版会自动加载 data/dictionary/stopwords.txt，这里补充业务相关词。
     */
    private static final Set<String> EXTRA_STOPWORDS = new HashSet<>(Arrays.asList(
            // 通用虚词
            "\u4e00\u4e9b", "\u8fd9\u4e2a", "\u90a3\u4e2a", "\u4ec0\u4e48", "\u600e\u4e48",
            "\u54ea\u4e9b", "\u8fd9\u4e9b", "\u4efb\u52a1", "\u9700\u8981", "\u8981\u6c42",
            "\u5e2e\u5fd9", "\u5e2e\u52a9", "\u5b8c\u6210", "\u5904\u7406", "\u8fdb\u884c",
            "\u63d0\u4f9b", "\u5185\u5bb9", "\u76f8\u5173", "\u5de5\u4f5c", "\u670d\u52a1",
            // 平台高频但无区分度的词
            "\u6821\u56ed", "\u540c\u5b66", "\u8bf7", "\u6c42", "\u52a0", "\u505a",
            "\u53ef", "\u80fd", "\u4f1a", "\u6709", "\u65e0",
            // 英文无意义词
            "the", "a", "an", "to", "of", "in", "on", "by", "for", "and", "or"
    ));

    /** HanLP 是否可用（懒加载检测）*/
    private static volatile Boolean hanLpAvailable = null;

    private TaskTagExtractor() {}

    /**
     * 从任务标题和描述中提取名词标签。
     *
     * @param title       任务标题（可为 null）
     * @param description 任务描述（可为 null）
     * @return 去重后的名词标签列表，最多 {@value MAX_TAGS} 个
     */
    public static List<String> extract(String title, String description) {
        String text = buildText(title, description);
        if (!StringUtils.hasText(text)) return Collections.emptyList();

        if (isHanLpAvailable()) {
            return extractWithHanLP(text);
        } else {
            return extractWithRegex(text);
        }
    }

    // =========================================================
    // HanLP 分词路径
    // =========================================================

    /**
     * 使用 HanLP 分词，仅保留名词（词性以 'n' 开头）。
     * HanLP 词性标注说明：
     *   n   - 普通名词
     *   nn  - 专有名词
     *   nz  - 其他专名
     *   nt  - 机构团体名
     *   ns  - 地名
     *   nr  - 人名（排除，避免推荐因人名偏移）
     */
    private static List<String> extractWithHanLP(String text) {
        try {
            List<Term> terms = HanLP.segment(text);
            return terms.stream()
                    .filter(t -> t.nature != null)
                    .filter(t -> {
                        String nat = t.nature.toString();
                        // 保留名词，排除人名（nr）
                        return nat.startsWith("n") && !nat.startsWith("nr");
                    })
                    .map(t -> t.word.trim().toLowerCase())
                    .filter(w -> w.length() >= MIN_WORD_LEN)
                    .filter(w -> !isPureNumber(w))
                    .filter(w -> !EXTRA_STOPWORDS.contains(w))
                    .distinct()
                    .limit(MAX_TAGS)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // HanLP 运行时异常，降级到正则
            hanLpAvailable = false;
            return extractWithRegex(text);
        }
    }

    // =========================================================
    // 降级：正则分词路径
    // =========================================================

    /**
     * 降级方案：按空格、标点、中文标点切割，过滤停用词。
     * 与改进前的 extractTaskTags 逻辑保持一致，作为兜底。
     */
    private static List<String> extractWithRegex(String text) {
        return Arrays.stream(text.split(
                        "[\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF\\u2000-\\u206F]+"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> s.length() >= MIN_WORD_LEN)
                .filter(s -> !isPureNumber(s))
                .filter(s -> !EXTRA_STOPWORDS.contains(s))
                .distinct()
                .limit(MAX_TAGS)
                .collect(Collectors.toList());
    }

    // =========================================================
    // 工具方法
    // =========================================================

    private static String buildText(String title, String description) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(title))       sb.append(title).append(" ");
        if (StringUtils.hasText(description)) sb.append(description);
        return sb.toString().trim();
    }

    /** 检查是否为纯数字（包括小数），纯数字标签无意义 */
    private static boolean isPureNumber(String s) {
        return s.matches("[0-9]+\\.?[0-9]*");
    }

    /**
     * 检测 HanLP 是否可用。
     * 首次调用时尝试分一个简单句，成功则标记可用，异常则标记不可用。
     * 结果缓存，避免每次调用都重复检测。
     */
    private static boolean isHanLpAvailable() {
        if (hanLpAvailable != null) return hanLpAvailable;
        synchronized (TaskTagExtractor.class) {
            if (hanLpAvailable != null) return hanLpAvailable;
            try {
                List<Term> test = HanLP.segment("\u6d4b\u8bd5");
                hanLpAvailable = test != null;
            } catch (Exception e) {
                hanLpAvailable = false;
            }
        }
        return hanLpAvailable;
    }
}
