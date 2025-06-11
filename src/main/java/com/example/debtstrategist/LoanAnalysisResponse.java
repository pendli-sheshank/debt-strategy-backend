package com.example.debtstrategist;

import java.util.List;
import java.util.Map;

public class LoanAnalysisResponse {
    private double monthlyEMI;
    private List<Map<String, Object>> amortizationSchedule;
    private List<Map<String, Object>> aiStrategies;

    // Getters and Setters
    public double getMonthlyEMI() { return monthlyEMI; }
    public void setMonthlyEMI(double monthlyEMI) { this.monthlyEMI = monthlyEMI; }
    public List<Map<String, Object>> getAmortizationSchedule() { return amortizationSchedule; }
    public void setAmortizationSchedule(List<Map<String, Object>> amortizationSchedule) { this.amortizationSchedule = amortizationSchedule; }
    public List<Map<String, Object>> getAiStrategies() { return aiStrategies; }
    public void setAiStrategies(List<Map<String, Object>> aiStrategies) { this.aiStrategies = aiStrategies; }
}