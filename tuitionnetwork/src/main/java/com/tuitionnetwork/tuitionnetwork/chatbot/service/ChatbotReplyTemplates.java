package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class ChatbotReplyTemplates {

    private final ChatbotSafety safety;

    public ChatbotReplyTemplates(ChatbotSafety safety) {
        this.safety = safety;
    }

    public String daily(DailyCollectionFigures figures, String message) {
        String amount = money(figures.totalCollected());
        long count = figures.transactionCount();
        if (safety.isArabic(message)) {
            String when = count == 0 ? "في هذا اليوم" : "النهاردة";
            return "جمّعنا " + amount + " جنيه " + when + "، من " + count + " عملية دفع.";
        }
        String when = count == 0 ? "on that date" : "today";
        return amount + " EGP collected " + when + ", across " + count + " transactions.";
    }

    public String outstanding(OutstandingIncomeFigures figures, String message) {
        String outstanding = money(figures.outstandingTotal());
        String income = money(figures.totalIncome());
        if (safety.isArabic(message)) {
            return "فيه " + figures.outstandingCount() + " طالب لسه عليهم فلوس بإجمالي "
                    + outstanding + " جنيه. إجمالي الدخل الكلي حتى الآن " + income + " جنيه.";
        }
        return figures.outstandingCount() + " students still have outstanding fees, totaling "
                + outstanding + " EGP. Total income so far is " + income + " EGP.";
    }

    public String weekly(WeeklyReportFigures figures, String message) {
        String total = money(figures.weekTotal());
        if (safety.isArabic(message)) {
            return "إجمالي التحصيل في آخر 7 أيام كان " + total + " جنيه، من "
                    + figures.transactionCount() + " عملية دفع.";
        }
        return "Total collections over the last 7 days were " + total + " EGP, across "
                + figures.transactionCount() + " transactions.";
    }

    private static String money(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount == null ? BigDecimal.ZERO : amount);
    }
}
