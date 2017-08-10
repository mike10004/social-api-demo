package com.github.mike10004.socialapidemo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Chars;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CrawlerTest {

    private static class TestClient {}

    @Test
    public void crawl() throws Exception {

        Character seed1 = 'a', seed2 = 'e';
        List<Character> seeds = ImmutableList.of(seed1, seed2);
        Function<Character, String> branchifier = ch -> {
            switch (ch) {
                case 'a':
                    return "bcd";
                case 'e':
                    return "fgh";
                case 'g':
                    return "ijk";
                default:
                    return "";
            }
        };
        List<Character> assets = new ArrayList<>();
        AssetProcessor assetProcessor = (asset, lineage) -> assets.add((Character)asset);
        VisitRecorder.MemoryVisitRecorder visitRecorder = VisitRecorder.inMemory();
        CrawlerConfig crawlerConfig = new CrawlerConfig() {
            @Override
            protected AssetProcessor buildAssetProcessor() {
                return assetProcessor;
            }

            @Override
            protected VisitRecorder buildVisitRecorder() {
                return visitRecorder;
            }
        };
        Crawler<?, ?> crawler = new Crawler<TestClient, Exception>(new TestClient(), crawlerConfig) {
            @Override
            protected Iterator<CrawlNode<?, Exception>> getSeedGenerator() {
                @SuppressWarnings("unchecked")
                Stream<CrawlNode<?, Exception>> actions = seeds.stream().map(seed -> actionify(seed, branchifier));
                return actions.collect(ImmutableList.toImmutableList()).iterator();
            }
        };
        String expected = "abcdefghijk";
        crawler.crawl(1024); // enough capacity that queue is never full
        String actual = new String(Chars.toArray(assets));
        System.out.format("crawl result: %s%n", actual);
        assertEquals("assets", expected, actual);

        assets.clear();
        visitRecorder.reset();

        crawler.crawl(1); // capacity less than would be used
        expected = "abef";
        actual = new String(Chars.toArray(assets));
        System.out.format("crawl result: %s%n", actual);
        assertEquals("assets", expected, actual);

    }

    private static CrawlNode actionify(Character seed, Function<Character, String> branchifier) {
        return new CrawlNode() {

            @Override
            public Object call() throws Exception {
                return seed;
            }

            @Override
            public Iterable<CrawlNode> findNextTargets(Object asset) throws Exception {
                String branches = branchifier.apply((Character) asset);
                List<Character> chars = Chars.asList(branches.toCharArray());
                List<CrawlNode> crawlNodes = chars.stream().map(ch -> actionify(ch, branchifier)).collect(Collectors.toList());
                return crawlNodes;
            }
        };

    }

    private static class TestException extends Exception {}

    @Test
    public void limitErrors() throws Exception {
        CrawlNode<Void, TestException> crawlNode = new CrawlNode<Void, TestException>() {
            @Override
            public Void call() throws TestException {
                throw new TestException();
            }
        };
        int numSeeds = 10, errorLimit = 7;
        ErrorReactor.LimitedErrorReactor reactor = ErrorReactor.limiter(errorLimit);
        CrawlerConfig crawlerConfig = DirectConfig.builder()
                .errorReactor(reactor)
                .build();
        Crawler<?, ?> crawler = new Crawler<TestClient, TestException>(new TestClient(), crawlerConfig) {
            @Override
            protected Iterator<CrawlNode<?, TestException>> getSeedGenerator() {
                //noinspection unchecked
                return (Iterator) repeat(crawlNode, numSeeds).iterator();
            }
        };
        try {
            crawler.crawl();
            fail("expected TooManyErrorsException");
        } catch (ErrorReactor.LimitedErrorReactor.TooManyErrorsException ignore) {
        }
        int numExceptions = reactor.getRetainedExceptions().size();
        System.out.format("%d seeds and %d exceptions%n", numSeeds, numExceptions);
        assertEquals("num exceptions", errorLimit, numExceptions);
        assertTrue("all TestException", reactor.getRetainedExceptions().stream().allMatch(ex -> ex instanceof TestException));
    }

    private static <T> ImmutableList<T> repeat(T item, int n) {
        return ImmutableList.copyOf(Iterables.limit(Iterables.cycle(item), n));
    }

}