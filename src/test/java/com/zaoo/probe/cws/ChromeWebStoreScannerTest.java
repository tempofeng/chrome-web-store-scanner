package com.zaoo.probe.cws;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ChromeWebStoreScannerTest {
    private ChromeWebStoreScanner chromeWebStoreScanner;

    @BeforeEach
    public void setUp() throws Exception {
        chromeWebStoreScanner = new ChromeWebStoreScanner();
    }

    @Test
    public void parse() throws Exception {
        String googleResp = FileUtils.readFileToString(new File("src/test/resources/testChromeWebStoreResp.txt"),
                "UTF-8");
        assertThat(googleResp).isNotEmpty();
        Map<String, ChromeWebStoreScanner.App> apps = new HashMap<>();
        chromeWebStoreScanner.parseBasic(googleResp, apps);
        apps.forEach((s, app) -> System.out.println(app));
        assertThat(apps.size()).isEqualTo(52);
    }

    @Test
    public void updateApp() throws Exception {
        String googleResp = FileUtils.readFileToString(new File("src/test/resources/testChromeWebStoreDetail.txt"),
                "UTF-8");
        assertThat(googleResp).isNotEmpty();
        ChromeWebStoreScanner.App app = new ChromeWebStoreScanner.App(null, null, 0d, 0l, null, 0l);
        chromeWebStoreScanner.updateApp(googleResp, app);

        assertThat(app.title).isEqualTo("FantasyLink");
        assertThat(app.updated).isEqualTo("August 13, 2016");
        assertThat(app.languages).isEqualTo("\"English\"");
        assertThat(app.image).isEqualTo(
                "https://lh4.googleusercontent.com/9SLKBOkJ7gzlLnz_rk4tJcZGBWXc4xg840CFB1aQ_PdYp3DUaUqXSn6xtWwu2OV7goksYuMB=s220-h140-e365");
        assertThat(app.desc).isEqualTo(
                "Adds FanGraphs, Baseball Reference, and Razzball integration into the ESPN, CBSSports, and Yahoo fantasy baseball websites.");
        assertThat(app.detail).isEqualTo(
                "FantasyLink: Helping you over-analyze you fantasy baseball team since 2012.\n" +
                        "\n" +
                        "What is FantasyLink?\n" +
                        "------\n" +
                        "\n" +
                        "FantasyLink is a Chrome browser extension that integrates FanGraphs, Baseball Reference, and Razzball directly into your fantasy baseball league website.\n" +
                        "\n" +
                        "Do I need it?\n" +
                        "------\n" +
                        "\n" +
                        "Do you play fantasy baseball? Is your league site run on CBSSports.com, ESPN.com, or Yahoo.com? Do you often find yourself unimpressed with the stats that those sites provide you? Do you think xFIP has more predictive value then ERA? If you answered yes to these questions, you probably spend a little to much time switching back and forth between your league website and more in depth baseball sites like FanGraphs, Baseball Reference, or Razzball. This extension is designed to cut down on that wasted time.\n" +
                        "How does it work?\n" +
                        "------\n" +
                        "\n" +
                        "FantasyLink scans through your league's team clubhouse, free agency, and trade pages looking in specific places for player names. Whenever a player is found, the extension inserts a link to that player's profile on FanGraphs, Baseball Reference, and/or Razzball (you can adjust the sites in the extension's setting).  Now looking up Mike Napoli's BABIP is as easy as clicking the little FanGraphs logo next to his name\n" +
                        "\n" +
                        "I'm sold, now how do I get it?\n" +
                        "------\n" +
                        "\n" +
                        "FantasyLink is available for download right here in the Chrome Store.  If you know javascript and want to tinker with the extension's code, you can fork the project on Github at https://github.com/sglantz/FantasyLink.\n");
        assertThat(app.screenshots.size()).isEqualTo(1);
        assertThat(app.screenshots.get(0)).isEqualTo("https://lh4.googleusercontent.com/OgQSsJ4h_WkeD5LkvrlQDsTMQlq5WXXatmOms0bmyKHo_rKWIYiHhMyn0ESRNWNRQj6xrS67Yw=s640-h400-e365");
    }
}