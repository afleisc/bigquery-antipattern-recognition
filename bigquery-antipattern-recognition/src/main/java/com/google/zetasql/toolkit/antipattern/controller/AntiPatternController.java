package com.google.zetasql.toolkit.antipattern.controller;

// Add these imports if not already fully there
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zetasql.toolkit.antipattern.AntiPatternVisitor;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnRequest;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResponse;
import com.google.zetasql.toolkit.antipattern.models.BigQueryRemoteFnResult;
import com.google.zetasql.toolkit.antipattern.util.AntiPatternHelper;
import org.springframework.stereotype.Controller; // Changed from @RestController
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody; // Import this

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller // <--- CHANGE THIS
public class AntiPatternController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // This method now correctly performs a redirect
    @GetMapping("/")
    public String redirectToUi() {
        return "redirect:/ui.html";
    }

    // This method now correctly performs a redirect
    @GetMapping("/ui")
    public String showUiPage() {
        return "redirect:/ui.html";
    }

    // This is an API endpoint, so it needs @ResponseBody
    @PostMapping("/")
    @ResponseBody // <--- ADD THIS
    public ObjectNode analyzeQueries(@RequestBody BigQueryRemoteFnRequest request) {
        ArrayNode replies = objectMapper.createArrayNode();
        // ... (rest of your method)
        for (JsonNode call : request.getCalls()) {
            BigQueryRemoteFnResponse queryResponse = analyzeSingleQuery(call); // Assuming this returns the right type
            ObjectNode resultNode = objectMapper.valueToTree(queryResponse);
            replies.add(resultNode);
        }

        ObjectNode finalResponse = objectMapper.createObjectNode();
        finalResponse.set("replies", replies);
        return finalResponse;
    }

    // This is also an API endpoint for the UI, so it needs @ResponseBody
    @PostMapping("/analyze-query-ui")
    @ResponseBody // <--- ADD THIS
    public List<BigQueryRemoteFnResult> analyzeSingleQueryForUi(@RequestBody String query) {
        try {
            InputQuery inputQuery = new InputQuery(query, "query from UI");
            List<AntiPatternVisitor> visitors = findAntiPatterns(inputQuery);
            if (visitors.isEmpty()) {
                List<BigQueryRemoteFnResult> noAntiPatternsResult = new ArrayList<>();
                noAntiPatternsResult.add(new BigQueryRemoteFnResult("None", "No anti-patterns found."));
                return noAntiPatternsResult;
            }
            return formatAntiPatterns(visitors);
        } catch (Exception e) {
            List<BigQueryRemoteFnResult> errorResult = new ArrayList<>();
            errorResult.add(new BigQueryRemoteFnResult("Error", e.getMessage()));
            return errorResult;
        }
    }

    // Private methods remain the same
    private BigQueryRemoteFnResponse analyzeSingleQuery(JsonNode call) {
        try {
            InputQuery inputQuery = new InputQuery(call.get(0).asText(), "query provided by UDF:");
            List<AntiPatternVisitor> visitors = findAntiPatterns(inputQuery);
            List<BigQueryRemoteFnResult> formattedAntiPatterns = new ArrayList<>();
            if (visitors.isEmpty()) {
                formattedAntiPatterns.add(new BigQueryRemoteFnResult("None", "No antipatterns found"));
            } else {
                formattedAntiPatterns = BigQueryRemoteFnResponse.formatAntiPatterns(visitors);
            }
            return new BigQueryRemoteFnResponse(formattedAntiPatterns, null);
        } catch (Exception e) {
            return new BigQueryRemoteFnResponse(null, e.getMessage());
        }
    }

    private List<AntiPatternVisitor> findAntiPatterns(InputQuery inputQuery) {
        List<AntiPatternVisitor> visitors = new ArrayList<>();
        AntiPatternHelper antiPatternHelper = new AntiPatternHelper("dannydeleo", true); // Assuming AntiPatternHelper is correctly initialized
        antiPatternHelper.checkForAntiPatternsInQueryWithParserVisitors(inputQuery, visitors);
        antiPatternHelper.checkForAntiPatternsInQueryWithAnalyzerVisitors(inputQuery, visitors);
        return visitors;
    }

    public static List<BigQueryRemoteFnResult> formatAntiPatterns(List<AntiPatternVisitor> visitors) {
        return visitors.stream()
                .map(visitor -> new BigQueryRemoteFnResult(visitor.getName(), visitor.getResult()))
                .collect(Collectors.toList());
    }
}