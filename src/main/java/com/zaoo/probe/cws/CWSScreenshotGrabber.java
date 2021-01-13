package com.zaoo.probe.cws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CWSScreenshotGrabber {

    private final TypeReference<List<ChromeExtension>> chromeExtensionsTR = new TypeReference<List<ChromeExtension>>() {
    };
    private final Pattern urlPattern = Pattern.compile("https:\\/\\/(.*)\\/(.*)=(s\\d*)-(h\\d*)-.*");

    public static void main(String[] args) throws IOException, InterruptedException {
        new CWSScreenshotGrabber().run();
    }

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File imageDir = new File("unown/img/");

    private void run() throws IOException {
        Request request = new Request.Builder()
                .url("https://script.google.com/macros/s/AKfycbxQGEcNbzLiZ6venvhiQRPJk1covSNNjzNZUFh_Rox4QxHV3Vk/exec")
                .build();
        Response response = client.newCall(request).execute();

        List<ChromeExtension> chromeExtensions = objectMapper.readValue(response.body().string(), chromeExtensionsTR);
        chromeExtensions.stream()
                .flatMap(chromeExtension -> Arrays.stream(chromeExtension.getScreenshots().split(",")))
                .filter(url -> !Strings.isNullOrEmpty(url))
                .forEach(this::saveToDisk);
    }

    private void saveToDisk(String url) {
        try {
            Matcher matcher = urlPattern.matcher(url);
            if (!matcher.matches()) {
                System.out.println("Image URL error:" + url);
            }

            // original
            String fileName = String.format("%s=%s-%s-e365.jpg",
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4));
            FileUtils.copyURLToFile(new URL(url), new File(imageDir, fileName));

            // high resolution
            url = String.format("https://%s/%s=s1280-h800-e365", matcher.group(1), matcher.group(2));
            fileName = String.format("%s=s1280-h800-e365.jpg",
                    matcher.group(2));
            FileUtils.copyURLToFile(new URL(url), new File(imageDir, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
