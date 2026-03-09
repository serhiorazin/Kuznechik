import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class WingsCustomerRequest {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 90_000;

    private WingsCustomerRequest() {
    }

    public static String createCustomerIfInn(
            String jsonPayload,
            String baseUrl,
            String requestId,
            String token
    ) {
        if (jsonPayload == null || !jsonPayload.contains("inn")) {
            throw new IllegalArgumentException("Payload must contain inn");
        }

        String fullRequestId = "create_cust_" + requestId;

        HttpsURLConnection connection = null;
        try {
            URL createUrl = new URL(baseUrl + "/1.0.0/create-customer");
            connection = (HttpsURLConnection) createUrl.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("requestId", fullRequestId);
            connection.setRequestProperty("Accept-Language", "en");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            String body = readResponseBody(connection, responseCode);
            String response = formatResponse(body, responseCode);

            if (responseCode == 202) {
                String checkResponse = sendCheckRequest(baseUrl, fullRequestId, token);
                response = response + "\n" + "CheckRequest: " + checkResponse;
            }

            return response;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String sendCheckRequest(String baseUrl, String fullRequestId, String token)
            throws IOException {
        HttpsURLConnection connection = null;
        try {
            URL checkUrl = new URL(baseUrl + "/1.0.0/checkRequest");
            connection = (HttpsURLConnection) checkUrl.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("requestId", fullRequestId);
            connection.setRequestProperty("Accept-Language", "en");
            connection.setRequestProperty("Authorization", "Bearer " + token);

            int responseCode = connection.getResponseCode();
            String body = readResponseBody(connection, responseCode);
            return formatResponse(body, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readResponseBody(HttpsURLConnection connection, int responseCode)
            throws IOException {
        InputStream stream = responseCode >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder res = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                res.append(line);
            }
            return res.toString();
        }
    }

    private static String formatResponse(String body, int code) {
        return "Response Body: " + body + "\n" + "Response Code: " + code;
    }
}
