package com.example.debtstrategist;

// Imports for the Google Cloud VERTEX AI SDK
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {

    @Value("${app.gemini.project-id}")
    private String projectId;

    // New public method for the controller to call for prompt-based analysis
    public LoanAnalysisResponse analyzeLoanFromPrompt(String prompt) throws IOException {
        LoanAnalysisRequest parsedRequest = parsePromptToLoanDetails(prompt);
        if (parsedRequest == null || parsedRequest.getPrincipal() == 0 || parsedRequest.getApr() == 0 || parsedRequest.getTenureInYears() == 0) {
            throw new IllegalArgumentException("Could not parse all required loan details from the prompt.");
        }
        return analyzeLoan(parsedRequest);
    }

    public LoanAnalysisResponse analyzeLoan(LoanAnalysisRequest request) throws IOException {
        double principal = request.getPrincipal();
        double apr = request.getApr();
        int tenureInYears = request.getTenureInYears();
        int gracePeriodInMonths = request.getGracePeriodInMonths();
        double gracePeriodPayment = request.getGracePeriodPayment();
        double monthlyRate = (apr / 100.0) / 12.0;

        // --- Step 1: Calculate the final principal after the grace period to determine the correct EMI ---
        double principalForEmiCalc = principal;
        if (gracePeriodInMonths > 0) {
            for (int i = 0; i < gracePeriodInMonths; i++) {
                double interestForMonth = principalForEmiCalc * monthlyRate;
                principalForEmiCalc += interestForMonth;
                principalForEmiCalc -= Math.min(principalForEmiCalc, gracePeriodPayment);
            }
        }

        // --- Step 2: Calculate the standard EMI based on the post-grace period balance ---
        int emiPaymentPeriodInMonths = (tenureInYears * 12) - gracePeriodInMonths;
        double emi = 0;
        if (monthlyRate > 0 && emiPaymentPeriodInMonths > 0) {
            emi = (principalForEmiCalc * monthlyRate * Math.pow(1 + monthlyRate, emiPaymentPeriodInMonths)) / (Math.pow(1 + monthlyRate, emiPaymentPeriodInMonths) - 1);
        } else if (emiPaymentPeriodInMonths > 0) {
            emi = principalForEmiCalc / emiPaymentPeriodInMonths;
        }

        // --- Step 3: NEW - Generate the complete schedule from Month 1 ---
        List<Map<String, Object>> schedule = new ArrayList<>();
        double remainingBalance = principal; // Start simulation with the ORIGINAL principal
        int totalLoanMonths = tenureInYears * 12;

        for (int month = 1; month <= totalLoanMonths; month++) {
            double interestForMonth = remainingBalance * monthlyRate;
            double principalPaid;
            double paymentForMonth;

            if (month <= gracePeriodInMonths) {
                // --- We are IN the grace period ---
                paymentForMonth = Math.min(remainingBalance + interestForMonth, gracePeriodPayment);
                principalPaid = paymentForMonth - interestForMonth;
            } else {
                // --- We are IN the standard repayment period ---
                paymentForMonth = Math.min(remainingBalance + interestForMonth, emi);
                principalPaid = paymentForMonth - interestForMonth;
            }

            remainingBalance += interestForMonth;
            remainingBalance -= paymentForMonth;

            Map<String, Object> monthEntry = new HashMap<>();
            monthEntry.put("month", month);
            monthEntry.put("payment", paymentForMonth);
            monthEntry.put("principalPaid", principalPaid);
            monthEntry.put("interestPaid", interestForMonth);
            monthEntry.put("remainingBalance", Math.max(0, remainingBalance));
            schedule.add(monthEntry);

            if (remainingBalance <= 0.01) {
                break; // Stop if the loan is paid off
            }
        }

        // --- Step 4: Assemble the response ---
        LoanAnalysisResponse response = new LoanAnalysisResponse();
        response.setMonthlyEMI(emi);
        response.setAmortizationSchedule(schedule);

        try {
            response.setAiStrategies(generateAIStrategies(principal, apr, tenureInYears, emi));
        } catch (IOException e) {
            e.printStackTrace();
            response.setAiStrategies(new ArrayList<>());
        }
        return response;
    }


    // New private helper to PARSE the prompt using VERTEX AI
    private LoanAnalysisRequest parsePromptToLoanDetails(String prompt) throws IOException {
        try (VertexAI vertexAi = new VertexAI(this.projectId, "us-central1")) {
            GenerativeModel model = new GenerativeModel("gemini-2.0-flash", vertexAi);
            String promptText = String.format(
                    "You are an assistant that extracts loan information from text. Analyze the following prompt and extract the principal, annual interest rate (APR) as a number, and tenure in years. Assume grace period and grace payment are 0 if not mentioned. Return ONLY a single, raw JSON object with keys: 'principal', 'apr', 'tenureInYears', 'gracePeriodInMonths', 'gracePeriodPayment'. User Prompt: \"%s\"",
                    prompt
            );

            GenerateContentResponse response = model.generateContent(promptText);
            String jsonResponseText = ResponseHandler.getText(response).trim().replace("```json", "").replace("```", "");

            LoanAnalysisRequest request = new LoanAnalysisRequest();
            String[] parts = jsonResponseText.replace("{", "").replace("}", "").replace("\"", "").split(",");
            for (String part : parts) {
                String[] keyValue = part.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    try {
                        if (key.equals("principal")) request.setPrincipal(Double.parseDouble(value));
                        if (key.equals("apr")) request.setApr(Double.parseDouble(value));
                        if (key.equals("tenureInYears")) request.setTenureInYears(Integer.parseInt(value));
                        if (key.equals("gracePeriodInMonths")) request.setGracePeriodInMonths(Integer.parseInt(value));
                        if (key.equals("gracePeriodPayment")) request.setGracePeriodPayment(Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        System.err.println("Could not parse value for key: " + key);
                    }
                }
            }
            return request;
        }
    }

    // Original private helper to GET STRATEGIES using VERTEX AI
    private List<Map<String, Object>> generateAIStrategies(double principal, double apr, int tenure, double emi) throws IOException {
        try (VertexAI vertexAi = new VertexAI(this.projectId, "us-central1")) {
            GenerativeModel model = new GenerativeModel("gemini-2.0-flash", vertexAi);
            String promptText = String.format(
                    "As a financial advisor, analyze this loan: Principal=%.2f, APR=%.1f%%, Tenure=%d years, standard EMI=%.2f. Suggest three distinct, actionable strategies to pay off this loan faster. For each strategy, you must provide a 'title', 'description', 'calculation' explaining the savings, estimated 'monthsSaved', and estimated 'interestSaved'. Format the entire response as a list separated by '###'. Example for one strategy: Title: The Extra Payment Method\nDescription: Consistently paying a little extra each month dramatically reduces the principal and total interest paid.\nCalculation: By paying an extra 100.00 each month, the loan is paid off much faster.\nMonths Saved: 24\nInterest Saved: 50123.45",
                    principal, apr, tenure, emi
            );
            GenerateContentResponse response = model.generateContent(promptText);
            String aiResponseText = ResponseHandler.getText(response);
            List<Map<String, Object>> strategies = new ArrayList<>();
            String[] strategyBlocks = aiResponseText.split("###");
            for (String block : strategyBlocks) {
                if (block.trim().isEmpty()) continue;
                Map<String, Object> strategy = new HashMap<>();
                String[] lines = block.trim().split("\n");
                for (String line : lines) {
                    String cleanLine = line.trim().startsWith("*") ? line.trim().substring(1).trim() : line.trim();
                    String[] parts = cleanLine.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim().toLowerCase().replace(" ", "");
                        String value = parts[1].trim();
                        if (key.equals("monthssaved") || key.equals("interestsaved")) {
                            try {
                                strategy.put(key, Double.parseDouble(value.replace(",", "")));
                            } catch (NumberFormatException e) {
                                strategy.put(key, value);
                            }
                        } else {
                            strategy.put(key, value);
                        }
                    }
                }
                if (strategy.containsKey("title")) {
                    strategies.add(strategy);
                }
            }
            return strategies;
        }
    }
}