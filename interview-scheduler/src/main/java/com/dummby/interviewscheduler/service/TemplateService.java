package com.dummby.interviewscheduler.service;

import com.dummby.interviewscheduler.exception.ResourceNotFoundException;
import com.dummby.interviewscheduler.model.dto.TemplateRequest;
import com.dummby.interviewscheduler.model.entity.Candidate;
import com.dummby.interviewscheduler.model.entity.MessageTemplate;
import com.dummby.interviewscheduler.repository.MessageTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages message templates and resolves placeholders.
 *
 * Per Requirement Document §3 the canonical placeholders are:
 *   {{column_1}} → Candidate Name
 *   {{column_2}} → Job Position
 *   {{column_3}} → Interview Date
 *   {{column_4}} → Interview Time
 *   {{column_5}} → Meeting Link
 */
@Service
@RequiredArgsConstructor
public class TemplateService {

    public static final String DEFAULT_TEMPLATE_NAME = "tvm_interview_invite";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final MessageTemplateRepository repository;

    /** Seed the TVM Infotech interview-invite template once on startup. */
    @PostConstruct
    public void seedDefault() {
        if (repository.findByName(DEFAULT_TEMPLATE_NAME).isEmpty()) {
            repository.save(MessageTemplate.builder()
                    .name(DEFAULT_TEMPLATE_NAME)
                    .body(defaultBody())
                    .active(true)
                    .build());
        }
    }

    private String defaultBody() {
        return """
                Hello {{column_1}},

                Greetings from TVM Infotech Pvt. Ltd.!

                We are pleased to inform you that your interview for the {{column_2}} position (Angular / React / Java / Full Stack / HR) has been scheduled.

                Interview Details:
                📅 Date: {{column_3}}
                ⏰ Time: {{column_4}}
                💻 Mode: Online (Google Meet)
                🔗 Meeting Link: {{column_5}}

                Angular Developer:
                Develop dynamic and responsive web applications using Angular, TypeScript, HTML, and CSS.

                React Developer:
                Build interactive and scalable user interfaces using React.js, JavaScript, and REST APIs.

                Java Developer:
                Design and develop backend services and APIs using Java, Spring Boot, and Microservices architecture.

                Full Stack Developer:
                Work on both frontend (Angular/React) and backend (Java/Spring Boot) to build complete web solutions.

                HR Executive:
                Handle recruitment, coordination, and onboarding processes while supporting HR operations and compliance.

                Important Instructions:

                Please join using a laptop with a stable internet connection.

                The interview will include a video interaction and technical/HR discussion, so ensure you are well prepared.

                Additional Information:
                Selected candidates will undergo a 3-month unpaid training for practical project experience.
                Upon successful completion and project assignment, the salary will be ₹13,500 per month.

                Your confirmation implies acceptance of these terms and commitment to attend
                the interview. Please avoid withdrawing after confirming.

                ✅ Action Required:
                If you agree to the above terms and wish to participate, kindly reply “CONFIRMED”.

                Best regards,
                HR – TVM Infotech Pvt. Ltd.
                Chennai – 600100
                """;
    }

    @CacheEvict(value = "templates", allEntries = true)
    public MessageTemplate create(TemplateRequest req) {
        return repository.save(MessageTemplate.builder()
                .name(req.getName()).body(req.getBody()).active(req.isActive()).build());
    }

    @CacheEvict(value = "templates", allEntries = true)
    public MessageTemplate update(UUID id, TemplateRequest req) {
        MessageTemplate t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        t.setName(req.getName());
        t.setBody(req.getBody());
        t.setActive(req.isActive());
        return repository.save(t);
    }

    @Cacheable("templates")
    @Transactional(readOnly = true)
    public List<MessageTemplate> findAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public MessageTemplate findByName(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + name));
    }

    @Transactional(readOnly = true)
    public MessageTemplate findDefault() {
        return repository.findByName(DEFAULT_TEMPLATE_NAME)
                .or(() -> repository.findAll().stream().filter(MessageTemplate::isActive).findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("No active template configured"));
    }

    @CacheEvict(value = "templates", allEntries = true)
    public void delete(UUID id) { repository.deleteById(id); }

    /**
     * Render template body by substituting placeholders with candidate values.
     *
     * <p>Resolution order (first match wins):
     * <ol>
     *   <li>Candidate's dynamic {@code extraFields} map — supports any header from
     *       any Excel (e.g. {@code {{panel_name}}}, {@code {{job_code}}}, {@code {{column_7}}}).</li>
     *   <li>Legacy named aliases ({@code candidateName}, {@code jobPosition}, …)
     *       and the canonical {@code {{column_1}}}–{@code {{column_5}}} placeholders.</li>
     * </ol>
     * Unknown placeholders are replaced with an empty string.
     */
    public String render(String body, Candidate c) {
        Map<String, String> vars = new HashMap<>();

        // 1) Legacy / RD-canonical (lowest precedence — populated first, may be overwritten by extras)
        vars.put("column_1", safe(c.getCandidateName()));
        vars.put("column_2", safe(c.getJobPosition()));
        vars.put("column_3", safe(c.getInterviewDate()));
        vars.put("column_4", safe(c.getInterviewTime()));
        vars.put("column_5", safe(c.getMeetingLink()));
        vars.put("candidateName", safe(c.getCandidateName()));
        vars.put("jobPosition",   safe(c.getJobPosition()));
        vars.put("interviewDate", safe(c.getInterviewDate()));
        vars.put("interviewTime", safe(c.getInterviewTime()));
        vars.put("meetingLink",   safe(c.getMeetingLink()));
        vars.put("phoneNumber",   safe(c.getPhoneNumber()));
        // Snake-case aliases for consistency with dynamic slug naming
        vars.put("candidate_name", safe(c.getCandidateName()));
        vars.put("job_position",   safe(c.getJobPosition()));
        vars.put("interview_date", safe(c.getInterviewDate()));
        vars.put("interview_time", safe(c.getInterviewTime()));
        vars.put("meeting_link",   safe(c.getMeetingLink()));
        vars.put("phone_number",   safe(c.getPhoneNumber()));

        // 2) Dynamic extras win — covers any Excel header (Panel Name, Job Code, etc.)
        Map<String, String> extras = c.getExtraFields();
        if (extras != null) {
            extras.forEach((k, v) -> vars.put(k, safe(v)));
        }

        Matcher m = PLACEHOLDER.matcher(body);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String val = vars.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String safe(String s) { return s == null ? "" : s; }
}
