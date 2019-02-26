package jmri.server.json.reporter;

import static jmri.server.json.reporter.JsonReporter.LAST_REPORT;
import static jmri.server.json.reporter.JsonReporter.REPORT;
import static jmri.server.json.reporter.JsonReporter.REPORTER;
import static jmri.server.json.reporter.JsonReporter.REPORTERS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import jmri.InstanceManager;
import jmri.Reporter;
import jmri.ReporterManager;
import jmri.server.json.JSON;
import jmri.server.json.JsonException;
import jmri.server.json.JsonNamedBeanHttpService;

/**
 *
 * @author Randall Wood Copyright 2016, 2018
 */
public class JsonReporterHttpService extends JsonNamedBeanHttpService<Reporter> {

    public JsonReporterHttpService(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public JsonNode doGet(String type, String name, Locale locale) throws JsonException {
        return this.doGetReporter(InstanceManager.getDefault(ReporterManager.class).getReporter(name), name, locale);
    }

    @Override
    public JsonNode doPost(String type, String name, JsonNode data, Locale locale) throws JsonException {
        Reporter reporter = this.postNamedBean(InstanceManager.getDefault(jmri.ReporterManager.class).getBySystemName(name), data, name, type, locale);
        if (data.path(JSON.USERNAME).isTextual()) {
            reporter.setUserName(data.path(JSON.USERNAME).asText());
        }
        if (data.path(JSON.COMMENT).isTextual()) {
            reporter.setComment(data.path(JSON.COMMENT).asText());
        }
        if (!data.path(REPORT).isMissingNode()) {
            if (data.path(REPORT).isNull()) {
                reporter.setReport(null);
            } else {
                reporter.setReport(data.path(REPORT).asText());
            }
        }
        return this.doGet(type, name, locale);
    }

    @Override
    public JsonNode doPut(String type, String name, JsonNode data, Locale locale) throws JsonException {
        try {
            InstanceManager.getDefault(ReporterManager.class).provideReporter(name);
        } catch (IllegalArgumentException ex) {
            throw new JsonException(400, Bundle.getMessage(locale, "ErrorCreatingObject", REPORTER, name));
        } catch (Exception ex) {
            throw new JsonException(500, Bundle.getMessage(locale, "ErrorCreatingObject", REPORTER, name));
        }
        return this.doPost(type, name, data, locale);
    }

    @Override
    public ArrayNode doGetList(String type, Locale locale) throws JsonException {
        ArrayNode root = this.mapper.createArrayNode();
        for (Reporter r : InstanceManager.getDefault(ReporterManager.class).getNamedBeanSet()) {
            root.add(this.doGet(REPORTER, r.getSystemName(), locale));
        }
        return root;
    }

    // package protected
    JsonNode doGetReporter(Reporter reporter, String name, Locale locale) throws JsonException {
        ObjectNode root = this.getNamedBean(reporter, name, REPORTER, locale); // throws JsonException if reporter == null
        ObjectNode data = root.with(JSON.DATA);
        data.put(JSON.STATE, reporter.getState());
        if (reporter.getCurrentReport() != null) {
            String report = reporter.getCurrentReport().toString();
            data.put(REPORT, report);
            //value matches text displayed on panel
            data.put(JSON.VALUE, (report.isEmpty() ? Bundle.getMessage(locale, "Blank") : report));
        } else {
            data.putNull(REPORT);
            data.put(JSON.VALUE, Bundle.getMessage(locale, "NoReport"));
        }
        if (reporter.getLastReport() != null) {
            data.put(LAST_REPORT, reporter.getLastReport().toString());
        } else {
            data.putNull(LAST_REPORT);
        }
        return root;
    }

    @Override
    public JsonNode doSchema(String type, boolean server, Locale locale) throws JsonException {
        switch (type) {
            case REPORTER:
            case REPORTERS:
                return doSchema(type,
                        server,
                        "jmri/server/json/reporter/reporter-server.json",
                        "jmri/server/json/reporter/reporter-client.json");
            default:
                throw new JsonException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Bundle.getMessage(locale, "ErrorUnknownType", type));
        }
    }
}
