package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import java.util.UUID;

public interface ChatbotFiguresSource {

    DailyCollectionFigures daily(UUID institutionId);

    OutstandingIncomeFigures outstandingAndIncome(UUID institutionId);

    WeeklyReportFigures weekly(UUID institutionId);
}
