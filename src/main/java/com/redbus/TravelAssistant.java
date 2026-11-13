
package com.redbus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class TravelAssistant {

    public static String ask(String busDetails, String question) {

        try {
            String apiKey = System.getenv("OPENROUTER_API_KEY");

            if (apiKey == null || apiKey.isEmpty()) {
                return "OpenRouter API key not found!";
            }

            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content",
                "You are a helpful bus travel assistant. " +
                "Bus details: " + busDetails +
                "\nUser question: " + question
            );

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "openrouter/auto");
            requestBody.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                    "https://openrouter.ai/api/v1/chat/completions"
                ))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    requestBody.toString()
                ))
                .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                return "AI Error: " + response.body();
            }

            JSONObject result = new JSONObject(response.body());

            return result.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        } catch (Exception e) {
            return "AI Assistant Error: " + e.getMessage();
        }
    }
}
