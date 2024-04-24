package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final Integer requestLimit;
    private final LinkedList<Instant> requestQueue = new LinkedList<>();

    public CrptApi(TimeUnit timeUnit, Integer requestLimit){
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void create(Object obj, String signature) throws InterruptedException {
        boolean shouldWait = true;
        while (shouldWait) {
            Instant timePoint = Instant.now();
            Instant timePointStart = timePoint.minus(1, timeUnit.toChronoUnit());
            var toDelete = new LinkedList<Instant>();
            for (Instant time : requestQueue){
                if (time.isBefore(timePointStart)){
                    toDelete.add(time);
                } else {
                    break;
                }
            }

            toDelete.forEach(time -> requestQueue.remove(time));
            if (requestQueue.size() < requestLimit) {
                send(obj, signature);
                requestQueue.addLast(timePoint);
                shouldWait = false;
            } else {
                var toWait = requestQueue.peekFirst().plus(1,timeUnit.toChronoUnit()).toEpochMilli()-Instant.now().toEpochMilli();
                if (toWait > 0) {
                    Thread.sleep(toWait);
                }
            }
        }
    }
    public void send(Object obj, String signature) {
        try{
            String strUrl = "http://localhost:4567/";
            URL url = new URL(strUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("signature", signature);

            connection.setDoOutput(true);

            StringWriter writer = new StringWriter();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(writer, obj);
            OutputStream out = connection.getOutputStream();
            String result = writer.toString();
            out.write(result.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
            connection.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

            for (int c; (c = in.read()) >= 0;)
                System.out.print((char)c);

        } catch (IOException e){
            System.out.println("Error sending request: " + e.getMessage());
        }
    }
}