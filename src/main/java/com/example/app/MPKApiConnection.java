package com.example.app;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class MPKApiConnection {
    static int requestsFinished = 0;
    private static HttpURLConnection connection;

    public static String getVehiclesData(LinkedList <String> requestedlines) throws IOException {
        var url = "https://mpk.wroc.pl/bus_position";
        var urlParameters = new StringBuilder();

        for(String line: requestedlines)
            urlParameters.append("busList[][]=").append(line).append("&");


        byte[] postBody = urlParameters.toString().getBytes(StandardCharsets.UTF_8);

        try {
            var myUrl = new URL(url);

            connection = (HttpURLConnection) myUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");


            try (DataOutputStream body = new DataOutputStream(connection.getOutputStream())) {
                body.write(postBody);
            }

            StringBuilder results;
            String currLine;

            try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

                results = new StringBuilder();

                while ((currLine = reader.readLine()) != null) {
                    results.append(currLine);
                }
            }
            System.out.println("[" + ++requestsFinished + "]got results");
            return results.toString();

        } finally {
            connection.disconnect();
        }
    }
}