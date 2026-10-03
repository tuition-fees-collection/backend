package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import com.tuitionnetwork.tuitionnetwork.chatbot.api.ChatbotAskResponse;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.service.AuditService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private static final Map<String, AuditAction> AUDIT_BY_FUNCTION = Map.of(
            "get_daily_collections", AuditAction.CHATBOT_DAILY_COLLECTIONS,
            "get_outstanding_and_income", AuditAction.CHATBOT_OUTSTANDING_AND_INCOME,
            "get_weekly_report", AuditAction.CHATBOT_WEEKLY_REPORT
    );

    private static final Set<String> ALLOWED = AUDIT_BY_FUNCTION.keySet();

    private final IdentityAccessService identityAccessService;
    private final AuditService auditService;
    private final ChatbotRateLimiter rateLimiter;
    private final ChatbotSafety safety;
    private final ChatbotModel model;
    private final ChatbotFiguresSource figures;
    private final ChatbotReplyTemplates replies;

    public ChatbotService(
            IdentityAccessService identityAccessService,
            AuditService auditService,
            ChatbotRateLimiter rateLimiter,
            ChatbotSafety safety,
            ChatbotModel model,
            ChatbotFiguresSource figures,
            ChatbotReplyTemplates replies
    ) {
        this.identityAccessService = identityAccessService;
        this.auditService = auditService;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
        this.model = model;
        this.figures = figures;
        this.replies = replies;
    }

    public ChatbotAskResponse ask(String message) {
        identityAccessService.requireInstitutionOfficer();
        AuthenticatedActor actor = identityAccessService.currentActor();
        UUID institutionId = actor.institutionId();

        if (!rateLimiter.allow(actor.accountId())) {
            throw new ChatbotRateLimitedException();
        }

        boolean arabic = safety.isArabic(message);
        String blocked = safety.detectBlocked(message);
        if (blocked != null) {
            auditService.recordAuthenticated(AuditAction.CHATBOT_BLOCKED, actor);
            String reply = "jailbreak".equals(blocked)
                    ? safety.injectionRefusal(arabic)
                    : safety.safeRefusal(arabic);
            return ChatbotAskResponse.refused(reply);
        }

        ChatbotDecision decision = model.classify(message);
        if (!decision.hasFunction()) {
            auditService.recordAuthenticated(AuditAction.CHATBOT_REFUSED, actor);
            return ChatbotAskResponse.refused(screenFreeText(decision.directReply(), arabic));
        }

        String functionName = decision.functionName();
        if (!ALLOWED.contains(functionName)) {
            auditService.recordAuthenticated(AuditAction.CHATBOT_REFUSED, actor);
            return ChatbotAskResponse.refused(safety.fallbackReply(arabic));
        }

        try {
            return switch (functionName) {
                case "get_daily_collections" -> {
                    DailyCollectionFigures data = figures.daily(institutionId);
                    yield answer(actor, functionName, replies.daily(data, message), data);
                }
                case "get_outstanding_and_income" -> {
                    OutstandingIncomeFigures data = figures.outstandingAndIncome(institutionId);
                    yield answer(actor, functionName, replies.outstanding(data, message), data);
                }
                case "get_weekly_report" -> {
                    WeeklyReportFigures data = figures.weekly(institutionId);
                    yield answer(actor, functionName, replies.weekly(data, message), data);
                }
                default -> ChatbotAskResponse.refused(safety.fallbackReply(arabic));
            };
        } catch (RuntimeException ex) {
            log.error("Chatbot aggregate failed for institution {} function {}", institutionId, functionName, ex);
            throw new ChatbotQueryException();
        }
    }

    private ChatbotAskResponse answer(
            AuthenticatedActor actor,
            String functionName,
            String reply,
            Object data
    ) {
        auditService.recordAuthenticated(AUDIT_BY_FUNCTION.get(functionName), actor);
        return ChatbotAskResponse.answered(reply, functionName, data);
    }

    private String screenFreeText(String directReply, boolean arabic) {
        String cleaned = safety.cleanDirectReply(directReply);
        if (cleaned.isBlank() || safety.looksLikeLeak(cleaned)) {
            return safety.fallbackReply(arabic);
        }
        return cleaned;
    }
}
