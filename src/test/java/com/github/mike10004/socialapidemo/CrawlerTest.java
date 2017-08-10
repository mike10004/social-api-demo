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
        CrawlerConfig crawlerConfig = DirectConfig.builder().assetProcessor(assetProcessor).build();
        Crawler<?, ?> crawler = new Crawler<TestClient, Exception>(new TestClient(), crawlerConfig) {
            @Override
            protected Iterator<Action<?, Exception>> getSeedGenerator() {
                @SuppressWarnings("unchecked")
                Stream<Action<?, Exception>> actions = seeds.stream().map(seed -> actionify(seed, branchifier));
                return actions.collect(ImmutableList.toImmutableList()).iterator();
            }
        };
        String expected = "abcdefghijk";
        crawler.crawl(1024); // enough capacity that queue is never full
        String actual = new String(Chars.toArray(assets));
        System.out.format("crawl result: %s%n", actual);
        assertEquals("assets", expected, actual);
        assets.clear();

        crawler.crawl(1); // capacity less than would be used
        expected = "abef";
        actual = new String(Chars.toArray(assets));
        System.out.format("crawl result: %s%n", actual);
        assertEquals("assets", expected, actual);

    }

    private static Crawler.Action actionify(Character seed, Function<Character, String> branchifier) {
        return new Crawler.Action() {

            @Override
            public Object call() throws Exception {
                return seed;
            }

            @Override
            public Iterable<Crawler.Action> findNextTargets(Object asset) throws Exception {
                String branches = branchifier.apply((Character) asset);
                List<Character> chars = Chars.asList(branches.toCharArray());
                List<Crawler.Action> actions = chars.stream().map(ch -> actionify(ch, branchifier)).collect(Collectors.toList());
                return actions;
            }
        };

    }

    private static class TestException extends Exception {}

    @Test
    public void limitErrors() throws Exception {
        Crawler.Action<Void, TestException> action = new Crawler.Action<Void, TestException>() {
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
            protected Iterator<Action<?, TestException>> getSeedGenerator() {
                //noinspection unchecked
                return (Iterator) repeat(action, numSeeds).iterator();
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