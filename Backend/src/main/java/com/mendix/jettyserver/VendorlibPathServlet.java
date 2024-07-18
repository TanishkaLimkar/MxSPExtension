package com.mendix.jettyserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.stream.Collectors;

public class VendorlibPathServlet extends HttpServlet {

    private String vendorlibPath;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            requestBody.append(reader.lines().collect(Collectors.joining()));
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"error\":\"Failed to read request body\"}");
            return;
        }

        System.out.println("Received JSON request body: " + requestBody.toString());

        vendorlibPath = parseVendorlibPathFromJson(requestBody.toString());
        if (vendorlibPath == null || vendorlibPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("{\"error\":\"Vendorlib path not found in JSON\"}");
            return;
        }

        MendixServer.setVendorlibPath(vendorlibPath);
        MendixServer.loadJarsFromVendorLib();

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("{\"message\":\"Vendorlib path set successfully\"}");
    }

    private String parseVendorlibPathFromJson(String json) {
        String vendorlibPath = "";
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            if (jsonObject.has("path")) {
                vendorlibPath = jsonObject.get("path").getAsString();
            } else {
                System.err.println("Error: 'path' not found in JSON.");
            }
        } catch (JsonParseException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.err.println("Error: Provided JSON is not a valid object.");
            e.printStackTrace();
        }
        return vendorlibPath;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (vendorlibPath != null) {
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("vendorlibPath", vendorlibPath);
            out.println(jsonResponse.toString());
        } else {
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("error", "Vendorlib path not set");
            out.println(jsonResponse.toString());
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
}

