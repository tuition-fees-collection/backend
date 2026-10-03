package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ChatbotSafety {

    private static final Pattern PHONE = Pattern.compile("(?:\\+?2?0)?01[0125]\\d{8}");
    private static final Pattern NATIONAL_ID = Pattern.compile("\\b\\d{13,14}\\b");
    private static final Pattern LONG_DIGIT_RUN = Pattern.compile("\\d{8,}");
    private static final Pattern ARABIC = Pattern.compile("[\\u0600-\\u06FF]");
    private static final Pattern CATEGORY_HEADER = Pattern.compile("(?im)^\\s*Category\\s*:.*$");
    private static final Pattern RESPONSE_LABEL = Pattern.compile("(?im)^\\s*(Response|الرد)\\s*:\\s*");
    private static final Pattern JSON_TOOL_CALL = Pattern.compile("\\{[^{}]*[\"'](name|function|parameters)[\"']\\s*:");

    private static final List<Pattern> JAILBREAK = List.of(
            Pattern.compile("(بدون|من\\s*غير|من\\s*دون|بلا)\\s*(أي\\s*)?(قيود|حدود|قواعد)"),
            Pattern.compile("تجاهل\\w*.{0,15}(تعليمات|أوامر|قواعد|حدود)"),
            Pattern.compile("(انسى|إنسى)\\w*.{0,15}(تعليمات|قواعد|أوامر)"),
            Pattern.compile("(اعتبر|تخيل)\\w*.{0,10}(نفسك|إنك|إنه|انك)"),
            Pattern.compile("(إنت|انت)\\s*(دلوقتي|الآن|من\\s*دلوقتي)\\s*(مساعد|بوت)"),
            Pattern.compile("مساعد\\s*(بدون|بلا|من\\s*غير)\\s*(قيود|صلاحيات|حدود)"),
            Pattern.compile("(اظهر|اكشف|قول)\\w*.{0,10}تعليمات\\w*.{0,15}(الداخلية|الأصلية)?"),
            Pattern.compile("السيستم\\s*برومت"),
            Pattern.compile("ignore\\s+(?:all\\s+|any\\s+|the\\s+)?(?:previous|prior|above|earlier|your)\\s+(?:previous\\s+|prior\\s+|your\\s+)?(?:instructions|rules|prompt)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+(are|'re)\\s+now\\s+an?\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(no|without|zero)\\s+restrictions?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwithout\\s+(any\\s+)?limits?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bact\\s+as\\s+(a|an|if)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpretend\\s+(you|to\\s+be)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\breveal\\s+your\\s+(system\\s+)?prompt\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(system\\s+prompt|developer\\s+mode|jailbreak)\\b", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> PERSONAL = List.of(
            Pattern.compile("رقم\\s*(ال)?(تليفون|موبايل|هاتف|جوال)"),
            Pattern.compile("الرقم\\s*القومي"),
            Pattern.compile("عنوان\\s*(ال)?(طالب|طلاب|ولي\\s*الأمر)"),
            Pattern.compile("(اسم|أسماء|بالاسم)\\s*(ال)?(طالب|طلاب|ولي\\s*الأمر)"),
            Pattern.compile("(مين|من)\\s*(هو\\s*)?(ال)?طالب\\s*(اللي|الذي)"),
            Pattern.compile("(أي|اي)\\s*طالب\\s*(أطلبه|اطلبه|معين)"),
            Pattern.compile("(بيانات|تفاصيل)\\s*(ال)?(طالب|طلاب|شخصية)"),
            Pattern.compile("(أكتر|اكتر|اكبر)\\s*طالب\\s*(دافع|لسه\\s*عليه)"),
            Pattern.compile("قايمة\\s*(ال)?طلاب.{0,20}(اسم|تليفون|رقم)"),
            Pattern.compile("\\bphone\\s*number\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnational\\s*id\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstudent'?s?\\s*(name|address|phone)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwhich\\s+student\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bwho\\s+is\\s+the\\s+student\\b", Pattern.CASE_INSENSITIVE)
    );

    public boolean isArabic(String message) {
        return message != null && ARABIC.matcher(message).find();
    }

    public String detectBlocked(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        if (JAILBREAK.stream().anyMatch(pattern -> pattern.matcher(message).find())) {
            return "jailbreak";
        }
        if (PERSONAL.stream().anyMatch(pattern -> pattern.matcher(message).find())) {
            return "personal";
        }
        return null;
    }

    public boolean looksLikeLeak(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return PHONE.matcher(text).find()
                || NATIONAL_ID.matcher(text).find()
                || LONG_DIGIT_RUN.matcher(text).find();
    }

    public String cleanDirectReply(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = RESPONSE_LABEL.matcher(CATEGORY_HEADER.matcher(text).replaceAll("")).replaceAll("").trim();
        if (JSON_TOOL_CALL.matcher(cleaned).find()) {
            return "";
        }
        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 500).trim();
        }
        return cleaned;
    }

    public String safeRefusal(boolean arabic) {
        return arabic
                ? "معنديش صلاحية إني أشارك بيانات شخصية زي أسماء الطلاب أو أرقام التليفونات أو الأرقام القومية. أقدر بس أقولك الأرقام الإجمالية (التحصيل اليومي، المتبقي والدخل، أو تقرير الأسبوع)."
                : "I can't share personal information such as student names, phone numbers, or national IDs. I can only report aggregate totals (today's collections, outstanding/income, or the weekly report).";
    }

    public String injectionRefusal(boolean arabic) {
        return arabic
                ? "معنديش صلاحية إني أتجاهل تعليماتي أو أغيّر دوري. أقدر بس أقولك الأرقام الإجمالية (التحصيل اليومي، المتبقي والدخل، أو تقرير الأسبوع)."
                : "I can't ignore my instructions or change my role. I can only report aggregate totals (today's collections, outstanding/income, or the weekly report).";
    }

    public String fallbackReply(boolean arabic) {
        return arabic
                ? "معنديش رد جاهز لسؤال زي ده هنا."
                : "I can't help with that here.";
    }
}
