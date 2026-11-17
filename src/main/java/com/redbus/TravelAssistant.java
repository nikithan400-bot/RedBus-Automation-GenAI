
package com.redbus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;

public class TravelAssistant {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public static String ask(String busDetails, String question) {

        try {

            String apiKey = System.getenv("OPENROUTER_API_KEY");

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return "AI Error: OpenRouter API key not found. "
                    + "Check OPENROUTER_API_KEY.";
            }

            JSONObject systemMessage = new JSONObject();

            systemMessage.put("role", "system");

            systemMessage.put(
                "content",
                "You are RouteWise AI, a reliable bus fare comparison assistant. "
                + "Analyze only the bus details provided. "
                + "Give a concise and useful fare comparison. "
                + "Never invent bus fares, departure times, discounts, "
                + "coupon codes, ratings, amenities or availability. "
                + "Show discount amount and percentage only when supported "
                + "by the supplied information. "
                + "Show coupon codes only when actually provided. "
                + "Clearly distinguish between an offer that is not shown "
                + "and information that cannot be verified. "
                + "Never describe demo data as live RedBus information."
                + "Analyze the SPECIAL OFFERS section separately from bus fares. "
+ "List all visible coupon codes and their actual eligibility conditions. "
+ "Do not claim that no coupon is available if coupon codes are provided. "
+ "Do not assume a coupon applies to every bus. "
+ "Do not calculate a discounted fare unless eligibility and discount "
+ "are verified from the supplied information. "
            );

            JSONObject userMessage = new JSONObject();

            userMessage.put("role", "user");

            userMessage.put(
                "content",
                "BUS DETAILS:\n" + busDetails
                + "\n\nUSER REQUEST AND PREFERENCES:\n" + question
            );

            JSONArray messages = new JSONArray();

            messages.put(systemMessage);
            messages.put(userMessage);

            JSONObject requestBody = new JSONObject();

            requestBody.put("model", "openrouter/auto");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 1000);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    "https://openrouter.ai/api/v1/chat/completions"
                ))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "RouteWise AI")
                .POST(HttpRequest.BodyPublishers.ofString(
                    requestBody.toString()
                ))
                .build();

            System.out.println("\nConnecting to OpenRouter AI...");

            HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                "OpenRouter HTTP Status: " + response.statusCode()
            );

            String responseBody = response.body();

            if (response.statusCode() != 200) {

                System.out.println("OpenRouter Error Response:");
                System.out.println(responseBody);

                return "AI Error: OpenRouter returned HTTP "
                    + response.statusCode()
                    + ". Check the console for details.";
            }

            JSONObject result = new JSONObject(responseBody);

            if (result.has("error") && !result.isNull("error")) {

                System.out.println(
                    "AI API Error: " + result.get("error")
                );

                return "AI Error: OpenRouter could not process the request.";
            }

            JSONArray choices = result.optJSONArray("choices");

            if (choices == null || choices.length() == 0) {

                System.out.println("No choices found in AI response:");
                System.out.println(responseBody);

                return "AI Error: No response choices received from OpenRouter.";
            }

            JSONObject choice = choices.optJSONObject(0);

            if (choice == null) {
                return "AI Error: Invalid response from OpenRouter.";
            }

            JSONObject message = choice.optJSONObject("message");

            if (message == null) {
                return "AI Error: AI message was missing.";
            }

            Object content = message.opt("content");

            // Normal text response
            if (content instanceof String) {

                String answer = ((String) content).trim();

                if (!answer.isEmpty()) {
                    return answer;
                }
            }

            // Array-based response content
            if (content instanceof JSONArray) {

                JSONArray contentArray = (JSONArray) content;

                StringBuilder textContent = new StringBuilder();

                for (int i = 0; i < contentArray.length(); i++) {

                    JSONObject part = contentArray.optJSONObject(i);

                    if (part != null) {

                        String text = part.optString("text", "");

                        if (!text.isBlank()) {
                            textContent.append(text).append("\n");
                        }
                    }
                }

                if (!textContent.toString().trim().isEmpty()) {
                    return textContent.toString().trim();
                }
            }

            System.out.println("\nAI returned empty content.");

            System.out.println(
                "Finish reason: "
                + choice.optString("finish_reason", "unknown")
            );

            System.out.println("Full API response:");
            System.out.println(responseBody);

            return "AI Error: OpenRouter returned an empty answer. "
                + "Check the console output for the actual API response.";

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return "AI Error: Request was interrupted.";

        } catch (Exception e) {

            e.printStackTrace();

            return "AI Assistant Error: " + e.getMessage();
        }
    }
}
