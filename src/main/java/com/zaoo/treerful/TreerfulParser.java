package com.zaoo.treerful;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TreerfulParser {
    public static void main(String[] args) throws Exception {
        ImmutableMap<String, String> rooms = ImmutableMap.<String, String>builder()
                .put("北車小樹屋-A", "plPDL")
                .put("大安小樹屋", "pg8wl")
                .put("古亭小樹屋-A", "A6Q4Z")
                .put("古亭小樹屋-B", "AYwQ9")
                .put("北車小樹屋-B", "KnPPX")
                .put("中正小樹屋", "AqgZm")
                .put("古亭和平小樹屋 - 301", "AYwDY")
                .put("古亭和平小樹屋 - 302", "pg85m")
                .put("古亭和平小樹屋 - 303", "KbLGd")
                .put("古亭和平小樹屋 - 304", "RGmal")
                .put("古亭和平小樹屋 - 305", "plPga")
                .put("忠孝復興小樹屋 - 201", "KxYbm")
                .put("忠孝復興小樹屋 - 202", "pwoYm")
                .put("南京復興小樹屋 - A", "KnmrX")
                .put("南京復興小樹屋 - B", "RWV1O")
                .put("科技大樓 - 601", "KbLw1")
                .put("科技大樓 - 602", "Km78e")
                .put("科技大樓 - 603", "RGZOW")
                .put("科技大樓 - 604", "plD1L")
                .put("科技大樓 - 605", "pgwjl")
                .build();
        for (Map.Entry<String, String> room : rooms.entrySet()) {
            new TreerfulParser().run(room.getKey(),
                    room.getValue(),
                    LocalDate.of(2017, 4, 1),
                    LocalDate.of(2017, 7, 3));
        }
    }

    private final Random random = new Random(System.currentTimeMillis());

    public void run(String roomName,
                    String rommId,
                    LocalDate startDate,
                    LocalDate endDate) throws IOException {
        File file = new File(String.format("treerful/treerful-%s.csv", roomName));
        StringBuilder out = new StringBuilder();
        CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.EXCEL);

        // header
        csvPrinter.print("date");
        csvPrinter.print("dayOfWeek");
        for (int i = 0; i < 24; i++) {
            csvPrinter.print(i + ":00");
            csvPrinter.print(i + ":30");
        }
        csvPrinter.println();

        // body
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            fetch(rommId, date, csvPrinter);
        }

        csvPrinter.flush();
        FileUtils.writeStringToFile(file, out.toString(), "UTF-8");
    }

    public void fetch(String roomId, LocalDate date, CSVPrinter csvPrinter) throws IOException {
        System.out.println("fetch:roomId=" + roomId + ",date=" + date);

        csvPrinter.print(DateTimeFormatter.ISO_DATE.format(date));
        csvPrinter.print(date.getDayOfWeek().name());
        String url = String.format("http://www.pickoneplace.com/book/time/%s?date=%s",
                roomId,
                DateTimeFormatter.ISO_DATE.format(date));
        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.select(".choiceTime");
        for (Element element : elements) {
            Set<String> classNames = element.classNames();
            if (!classNames.contains("ableTime")) {
                csvPrinter.print("1");
            } else {
                csvPrinter.print("");
            }
        }
        csvPrinter.println();

        try {
            Thread.sleep(random.nextInt(10) * 100);
        } catch (InterruptedException e) {
        }
    }
}
