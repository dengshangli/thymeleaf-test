package com.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.VelocityException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.PrintWriter;

@Controller
public class PlaygroundController {

    private final TemplateEngine playgroundTemplateEngine;
    private final VelocityEngine playgroundVelocityEngine;
    private final ObjectMapper objectMapper;

    public PlaygroundController(@Qualifier("playgroundTemplateEngine") TemplateEngine playgroundTemplateEngine,
                                @Qualifier("playgroundVelocityEngine") VelocityEngine playgroundVelocityEngine,
                                ObjectMapper objectMapper) {
        this.playgroundTemplateEngine = playgroundTemplateEngine;
        this.playgroundVelocityEngine = playgroundVelocityEngine;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/playground";
    }

    @GetMapping("/playground")
    public String playground() {
        return "playground";
    }

    public record RenderRequest(String engine, String template, String modelJson) {}

    public record RenderResponse(String renderedHtml, String error) {}

    @PostMapping(value = "/playground/render", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RenderResponse> render(@RequestBody RenderRequest req) {
        if (req == null || !StringUtils.hasText(req.template())) {
            return ResponseEntity.badRequest().body(new RenderResponse(null, "template 不能为空"));
        }

        String engine = (req.engine() == null ? "" : req.engine().trim().toLowerCase(Locale.ROOT));
        if (!StringUtils.hasText(engine)) {
            engine = "thymeleaf";
        }
        if (!engine.equals("thymeleaf") && !engine.equals("velocity")) {
            return ResponseEntity.badRequest().body(new RenderResponse(null, "engine 不支持: " + engine + "（仅支持 thymeleaf/velocity）"));
        }

        Map<String, Object> model = Collections.emptyMap();
        String json = req.modelJson();
        if (StringUtils.hasText(json)) {
            try {
                model = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new RenderResponse(null, "JSON 解析失败: " + e.getMessage()));
            }
        }

        try {
            if (engine.equals("velocity")) {
                VelocityContext ctx = new VelocityContext(model);
                StringWriter out = new StringWriter();
                boolean ok = playgroundVelocityEngine.evaluate(ctx, out, "playground", new StringReader(req.template()));
                if (!ok) {
                    return ResponseEntity.badRequest().body(new RenderResponse(null, "Velocity 渲染失败: evaluate 返回 false"));
                }
                return ResponseEntity.ok(new RenderResponse(out.toString(), null));
            } else {
                Context ctx = new Context(Locale.SIMPLIFIED_CHINESE);
                ctx.setVariables(model);
                String rendered = playgroundTemplateEngine.process(req.template(), ctx);
                return ResponseEntity.ok(new RenderResponse(rendered, null));
            }
        } catch (Throwable e) {
            String prefix = engine.equals("velocity") ? "Velocity" : "Thymeleaf";
            String errorMsg;
            try {
                errorMsg = extractDetailedError(e, engine, req.template());
            } catch (Exception ex) {
                // 如果提取详细错误信息时出错，至少返回基本错误信息
                errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
            return ResponseEntity.badRequest().body(new RenderResponse(null, prefix + " 渲染失败: " + errorMsg));
        }
    }

    /**
     * 提取详细的错误信息，包括行号和列号
     */
    private String extractDetailedError(Throwable e, String engine, String template) {
        StringBuilder sb = new StringBuilder();
        
        try {
            // 1) 先把“基础错误信息”变短：Thymeleaf 的 message 经常包含整个 template
            sb.append(summarizeErrorMessage(e));

            // 2) 再从异常链里兜底提取 line/col（TemplateProcessingException / attoparser / message patterns）
            LineCol lc = findLineCol(e);
            if (lc != null && lc.line > 0) {
                sb.append("\n\n📍 错误位置:");
                sb.append("\n   行号: ").append(lc.line);
                if (lc.col > 0) {
                    sb.append("\n   列号: ").append(lc.col);
                }

                // 3) 提供错误行内容 + ^ 指针（前端会高亮缩进的代码行）
                if (template != null && !template.isEmpty()) {
                    appendLineSnippet(sb, template, lc.line, lc.col);
                }
            }
        } catch (Exception ex) {
            // 如果提取过程中出错，返回基本错误信息
            String msg = e.getMessage();
            if (msg != null) {
                sb.append(msg);
            } else {
                sb.append(e.getClass().getSimpleName());
            }
        }
        
        // 如果没有提取到详细信息，至少显示原始消息
        if (sb.length() == 0) {
            String msg = e.getMessage();
            if (msg != null) {
                sb.append(msg);
            } else {
                sb.append(e.getClass().getSimpleName());
            }
        }
        
        return sb.toString();
    }

    private static final class LineCol {
        final int line;
        final int col;
        private LineCol(int line, int col) {
            this.line = line;
            this.col = col;
        }
    }

    private static final Pattern[] LINE_COL_PATTERNS = new Pattern[] {
            // Thymeleaf / attoparser 常见格式: "(line 12, col 34)"
            Pattern.compile("\\(\\s*line\\s+(\\d+)\\s*,\\s*col\\s+(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\(\\s*line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE),
            // 其他变体: "line 12, col 34"
            Pattern.compile("\\bline\\s+(\\d+)\\b[^\\d]+\\bcol\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bline\\s+(\\d+)\\b[^\\d]+\\bcolumn\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            // stacktrace 常见："at line 12, col 34"
            Pattern.compile("\\bat\\s+line\\s+(\\d+)\\s*,\\s*(?:col|column)\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            // 中文变体（如果未来有）："行号: 12 列号: 34"
            Pattern.compile("行号\\s*[:：]\\s*(\\d+)[^\\d]+列号\\s*[:：]\\s*(\\d+)")
    };

    private static LineCol findLineCol(Throwable e) {
        // 0) 先从 stacktrace 文本里找（有些库只把行列号放进 message/stacktrace）
        String st = stackTraceToString(e);
        LineCol fromStack = parseLineColFromText(st);
        if (fromStack != null && (fromStack.line > 0 || fromStack.col > 0)) return fromStack;

        for (Throwable t = e; t != null; t = t.getCause()) {
            // 1) Thymeleaf processing exception 直接拿
            if (t instanceof TemplateProcessingException tpe) {
                int line = safeInt(tpe.getLine());
                int col = safeInt(tpe.getCol());
                if (line > 0 || col > 0) return new LineCol(line, col);
            }

            // 2) Velocity parse error
            if (t instanceof ParseErrorException pee) {
                int line = safeInt(pee.getLineNumber());
                int col = safeInt(pee.getColumnNumber());
                if (line > 0 || col > 0) return new LineCol(line, col);
            }

            // 3) 反射兜底：attoparser.ParseException / 其他异常可能有 getLine/getCol/getLineNumber/getColumnNumber
            Integer line = firstNonNull(
                    tryInvokeIntGetter(t, "getLine"),
                    tryInvokeIntGetter(t, "getLineNumber")
            );
            Integer col = firstNonNull(
                    tryInvokeIntGetter(t, "getCol"),
                    tryInvokeIntGetter(t, "getColumn"),
                    tryInvokeIntGetter(t, "getColumnNumber")
            );
            if ((line != null && line > 0) || (col != null && col > 0)) {
                return new LineCol(line == null ? 0 : line, col == null ? 0 : col);
            }

            // 4) 从 message 解析
            LineCol parsed = parseLineColFromText(t.getMessage());
            if (parsed != null && (parsed.line > 0 || parsed.col > 0)) return parsed;
        }
        return null;
    }

    private static LineCol parseLineColFromText(String msg) {
        if (msg == null || msg.isBlank()) return null;
        for (Pattern p : LINE_COL_PATTERNS) {
            Matcher m = p.matcher(msg);
            if (m.find()) {
                int line = safeParseInt(m.group(1));
                int col = safeParseInt(m.group(2));
                if (line > 0 || col > 0) return new LineCol(line, col);
            }
        }
        return null;
    }

    private static void appendLineSnippet(StringBuilder sb, String template, int line, int col) {
        try {
            String[] lines = template.split("\n", -1);
            if (line <= 0 || line > lines.length) return;
            String errorLine = lines[line - 1];
            sb.append("\n\n📄 错误行内容:");
            sb.append("\n   ").append(errorLine);
            if (col > 0 && errorLine != null) {
                sb.append("\n   ");
                int max = Math.min(col - 1, errorLine.length());
                for (int i = 0; i < max; i++) {
                    char c = errorLine.charAt(i);
                    sb.append(c == '\t' ? "    " : " ");
                }
                sb.append("^");
            }
        } catch (Exception ignore) {
            // ignore
        }
    }

    /**
     * Thymeleaf 报错经常包含整个 template（很长），这里截断以便把“行号/列号”凸显出来。
     */
    private static String summarizeErrorMessage(Throwable e) {
        if (e == null) return "Unknown error";
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return e.getClass().getSimpleName();

        // 去掉 "(template: "...")" 这段巨长内容
        String lower = msg.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("(template:");
        if (idx < 0) {
            idx = lower.indexOf("(template :");
        }
        if (idx >= 0) {
            return msg.substring(0, idx).trim();
        }

        // 兜底截断
        int limit = 600;
        if (msg.length() > limit) {
            return msg.substring(0, limit) + "...(truncated)";
        }
        return msg;
    }

    private static String stackTraceToString(Throwable e) {
        if (e == null) return "";
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static Integer tryInvokeIntGetter(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... items) {
        if (items == null) return null;
        for (T it : items) if (it != null) return it;
        return null;
    }

    private static int safeInt(int v) {
        return v;
    }

    private static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return 0;
        }
    }
}


