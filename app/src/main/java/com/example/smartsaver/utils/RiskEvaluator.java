package com.example.smartsaver.utils;

public class RiskEvaluator {

    public static String generateRecommendation(String duration, String risk, double amount) {
        duration = duration.toLowerCase();
        risk = risk.toLowerCase();

        if (duration.equals("short") && risk.equals("low")) {
            return "Consider a high-yield savings account or short-term treasury bills.";
        } else if (duration.equals("medium") && risk.equals("medium")) {
            return "Look into diversified mutual funds or stable ETFs.";
        } else if (duration.equals("long") && risk.equals("high")) {
            return "You may consider investing in high-growth stocks or crypto assets (be cautious).";
        } else if (duration.equals("long") && risk.equals("low") && amount > 50000) {
            return "For a safe long-term goal, consider government bonds and blue-chip stocks.";
        } else {
            return "Based on your selections, we recommend a balanced investment strategy: some cash, some stocks, some bonds.";
        }
    }

    public static String determineRiskColor(String risk) {
        risk = risk.toLowerCase();
        switch (risk) {
            case "low":
                return "green";
            case "medium":
                return "orange";
            case "high":
                return "red";
            default:
                return "gray";
        }
    }
}
