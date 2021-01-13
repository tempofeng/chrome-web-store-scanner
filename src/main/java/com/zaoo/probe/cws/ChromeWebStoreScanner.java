package com.zaoo.probe.cws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import okhttp3.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChromeWebStoreScanner {

    public static final int MIN_INSTALLED_USERS = 100 * 1000;
    public static final int APPS_PER_PAGE = 200;

    public static void main(String[] args) throws IOException {
        new ChromeWebStoreScanner().run();
    }

    private final OkHttpClient client = new OkHttpClient();
    private final Random random = new Random(System.currentTimeMillis());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void run() throws IOException {
        Map<String, App> apps = new HashMap<>();
        fetchFromSearch(apps, 6000);
        saveToCsv(apps, new File("cws.csv"));
    }

    public void run2() throws IOException {
        Map<String, App> apps = new HashMap<>();
//        fetchFromSearch(apps, 6000);
//        fetchFromCategory(apps, 6000);
        for (String searchTerm : ImmutableList.of(
                "shopicks")) {
            fetchFromSearch(searchTerm, apps, 20);
        }
        fetchDetail(apps, true);
//        fetchDetail(apps, true);
        saveToCsv(apps, new File("cws.csv"));
    }

    private void fetchDetail(Map<String, App> apps, boolean parseRecommendedApps) throws IOException {
        List<String> details = new ArrayList<>();
        for (Map.Entry<String, App> entry : apps.entrySet()) {
            if (entry.getValue().users < MIN_INSTALLED_USERS) {
                continue;
            }

            if (!Strings.isNullOrEmpty(entry.getValue().updated)) {
                continue;
            }

            String detail = detail(entry.getValue().id);
            updateApp(detail, entry.getValue());
            details.add(detail);
        }

        if (parseRecommendedApps) {
            for (String detail : details) {
                parseBasic(detail, apps);
            }
        }
    }

    private void fetchFromCategory(Map<String, App> apps, int limit) throws IOException {
        List<String> categories = ImmutableList.of("ext%2F22-accessibility",
                "ext%2F10-blogging",
                "ext%2F15-by-google",
                "ext%2F11-web-development",
                "ext%2F14-fun",
                "ext%2F6-news",
                "ext%2F28-photos",
                "ext%2F7-productivity",
                "ext%2F38-search-tools",
                "ext%2F12-shopping",
                "ext%2F1-communication",
                "ext%2F13-sports");

        for (String category : categories) {
            System.out.println("category:" + category);

            for (int i = 0; i < limit; i += APPS_PER_PAGE) {
                String googleResp = items(APPS_PER_PAGE, i, null, category);
                if (!parseBasic(googleResp, apps)) {
                    break;
                }
            }
        }
    }

    private void fetchFromSearch(Map<String, App> apps, int limit) throws IOException {
        for (char c = 'a'; c <= 'z'; c++) {
            String searchTerm = Character.toString(c);
            fetchFromSearch(searchTerm, apps, limit);
        }
    }

    private void fetchFromSearch(String searchTerm, Map<String, App> apps, int limit) throws IOException {
        System.out.println("searchTerm:" + searchTerm);
        for (int i = 0; i < limit; i += APPS_PER_PAGE) {
            String googleResp = items(APPS_PER_PAGE, i, searchTerm, "extensions");
            if (!parseBasic(googleResp, apps)) {
                break;
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(random.nextInt(10) * 1000);
        } catch (InterruptedException e) {
        }
    }

    private void saveToCsv(Map<String, App> apps, File file) throws IOException {
        StringBuilder out = new StringBuilder();
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.EXCEL);
        apps.forEach((s, app) -> {
            try {
                app.print(csvPrinter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        csvPrinter.flush();
        FileUtils.writeStringToFile(file, out.toString(), "UTF-8");
    }

    private String items(int count, int start, String searchTerm, String category) throws IOException {
        StringBuilder builder = new StringBuilder()
                .append("https://chrome.google.com/webstore/ajax/item?hl=en-US&gl=US&pv=20170206&mce=atf%2Ceed%2Cpii%2Crtr%2Crlb%2Cgtc%2Chcn%2Csvp%2Cwtd%2Cc3d%2Cncr%2Cctm%2Cac%2Chot%2Cmac%2Cfcf%2Crma%2Crer%2Crae%2Cshr%2Cesl&")
                .append("count=").append(count).append("&")
                .append("token=").append(start).append("%40").append(start).append("&")
                .append("category=").append(category).append("&");
        if (!Strings.isNullOrEmpty(searchTerm)) {
            builder.append("searchTerm=").append(searchTerm).append("&");
        }
        builder.append("sortBy=0&container=CHROME&_reqid=1752501&rt=j");

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=UTF-8"),
                "");
        System.out.println(builder.toString());
        Request request = new Request.Builder()
                .url(builder.toString())
                .header("Origin", "https://chrome.google.com")
                .header("Referer", "https://chrome.google.com/")
                .header("X-Same-Domain", "1")
                .header("User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
                .post(requestBody)
                .build();
        return fetch(request);
    }

    private String detail(String id) throws IOException {
        StringBuilder builder = new StringBuilder()
                .append("https://chrome.google.com/webstore/ajax/detail?hl=en-US&gl=US&pv=20170206&mce=atf%2Ceed%2Cpii%2Crtr%2Crlb%2Cgtc%2Chcn%2Csvp%2Cwtd%2Cc3d%2Cncr%2Cctm%2Cac%2Chot%2Cmac%2Cfcf%2Crma%2Cpot%2Cevt%2Cigb&")
                .append("id=").append(id).append("&")
                .append("container=CHROME&_reqid=7345954&rt=j");

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=UTF-8"),
                "");
        Request request = new Request.Builder()
                .url(builder.toString())
                .header("Origin", "https://chrome.google.com")
                .header("Referer", "https://chrome.google.com/")
                .header("X-Same-Domain", "1")
                .header("User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
                .post(requestBody)
                .build();
        return fetch(request);
    }

    @NotNull
    private String fetch(Request request) throws IOException {
        IOException ex = new IOException();
        for (int i = 0; i < 3; i++) {
            try {
                Response response = client.newCall(request).execute();
                return response.body().string();
            } catch (IOException e) {
                ex = e;
            } finally {
                sleep();
            }
        }
        throw ex;
    }

    void updateApp(String detail, App app) throws IOException {
        String detailWithHeader = detail.substring(6);
        JsonNode rootNode = objectMapper.readTree(detailWithHeader);
        JsonNode getItemDetailResponse = rootNode.get(0).get(1);
        JsonNode detailNode = getItemDetailResponse.get(1).get(0);
        app.title = detailNode.get(1).asText();
        app.image = detailNode.get(4).asText();
        app.desc = detailNode.get(6).asText();
        app.updated = getItemDetailResponse.get(1).get(7).asText();
        app.languages = jsonArrayToString(getItemDetailResponse.get(1).get(8));
        app.detail = getItemDetailResponse.get(1).get(1).asText();

        app.screenshots = new ArrayList<>();
        JsonNode screenshotsNode = getItemDetailResponse.get(1).get(11);
        for (JsonNode screenshotNode : screenshotsNode) {
            String url = screenshotNode.get(17).asText();
            if (Strings.isNullOrEmpty(url)) {
                continue;
            }
            app.screenshots.add(url);
        }
    }

    private String jsonArrayToString(JsonNode jsonNode) {
        return StringUtils.join(jsonNode.iterator(), ",");
    }

    void updateAppByRegexp(String detail, App app) {
        Pattern basicPattern = Pattern.compile(
                "\\[\"getitemdetailresponse\",\\[\\[\"(\\w*)\",\"(.*)\",\"(.*)\",\"(.*)\",\"(.*)\",(null|\".*\"),\"(.*)\",\\[");
        Matcher matcher = basicPattern.matcher(detail);
        if (matcher.find()) {
            app.title = matcher.group(2);
            app.image = matcher.group(5).replace("\\u003d", "=");
            app.desc = matcher.group(7);
            System.out.println("update:id=" + app.id + ",title=" + app.title + ",image=" + app.image +
                    ",desc=" + app.desc);
        }

        Pattern detailPattern = Pattern.compile(",\"([\\w\\s]*,[\\d\\s]*)\",\\[(.*)\\]");
        matcher = detailPattern.matcher(detail);
        if (matcher.find()) {
            app.updated = matcher.group(1);
            app.languages = matcher.group(2);
            System.out.println("update:id=" + app.id + ",updated=" + app.updated + ",language=" + app.languages);
        }
    }

    boolean parseBasic(String googleResp, Map<String, App> apps) {
        Pattern pattern = Pattern.compile(
                "\"ext\\/(.*)\",\".*\"http:\\/\\/chrome\\.google\\.com.*\\?id\\\\u003d(\\w*)\\\",([\\d\\.]*)[,\\w*]*,(\\d*),\"([\\d,\\+]*)\",.*,\"(https:\\/\\/chrome.google.com\\/.*)\"");
        Matcher matcher = pattern.matcher(googleResp);
        int count = 0;
        while (matcher.find()) {
            String id = matcher.group(2);
            String url = matcher.group(6);
            double rating = Double.parseDouble(matcher.group(3));
            long ratingCount = Long.parseLong(matcher.group(4));
            long users = Long.parseLong(matcher.group(5).replace("+", "").replace(",", ""));
            String category = matcher.group(1);

            if (!apps.containsKey(id)) {
                apps.put(id, new App(id, url, rating, users, category, ratingCount));
            }

            System.out.println(url);
            count++;
        }

        System.out.println("parsed:" + count);
        return count != 0;
    }

    static class App {
        final String id;
        final String url;
        final double rating;
        final long users;
        final String category;
        final long ratingCount;
        String updated;
        String languages;
        String image;
        String desc;
        String title;
        String detail;
        List<String> screenshots;

        public App(String id,
                   String url,
                   double rating,
                   long users,
                   String category,
                   long ratingCount) {
            this.id = id;
            this.url = url;
            this.rating = rating;
            this.users = users;
            this.category = category;
            this.ratingCount = ratingCount;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("url", url)
                    .add("rating", rating)
                    .add("users", users)
                    .add("category", category)
                    .toString();
        }

        public void print(CSVPrinter csvPrinter) throws IOException {
            csvPrinter.print(users);
            csvPrinter.print(category);
            csvPrinter.print(id);
            csvPrinter.print(url);
            csvPrinter.print(rating);
            csvPrinter.print(ratingCount);
            csvPrinter.print(updated);
            csvPrinter.print(languages);
            csvPrinter.print(image);
            csvPrinter.print(desc);
            csvPrinter.print(title);
            csvPrinter.print(detail);
            csvPrinter.print(StringUtils.join(screenshots, ","));
            csvPrinter.println();
        }
    }
}
