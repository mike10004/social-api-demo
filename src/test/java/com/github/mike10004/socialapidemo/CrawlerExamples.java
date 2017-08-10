package com.github.mike10004.socialapidemo;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class CrawlerExamples {

    public static class T {
        public static void main(String[] args) throws Exception {
            File configFile = new File(System.getProperty("user.dir"), "example-config.json");
            Path assetsDir = java.nio.file.Files.createTempDirectory("CrawlerExamples");
            System.out.format("%s is assets directory%n", assetsDir);
            Program.main(new String[]{
                    "--debug",
                    "--max-errors", "8",
                    "--max-assets", "64",
                    "--config-file", configFile.getAbsolutePath(),
                    "--processor", assetsDir.toFile().toURI().toString(),
                    "DIRECT",
                    Program.Mode.crawl.name(),
                    Program.Sns.twitter.name()
            }); // JVM exits here, so don't add code below
        }
    }

    public static class LoggingConfigExample {
        public static void main(String[] args) throws Exception {
            Program.logVerbosely(System.out);
            org.slf4j.Logger log = LoggerFactory.getLogger(Crawler.class);
            log.debug("debug");
            log.info("info");
            log.warn("warn");
        }
    }
}
