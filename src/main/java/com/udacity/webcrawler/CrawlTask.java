package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link RecursiveAction} that represents a web crawling task for a specific URL.
 * This class is responsible for visiting the URL, parsing its content, updating word counts,
 * and creating and invoking subtasks for each link found on the page.
 */
class CrawlTask extends RecursiveAction {
    /**
     * Constructs a new {@code CrawlTask}.
     *
     * @param url         the URL to crawl
     * @param maxDepth    the maximum depth of the crawling process
     * @param deadline    the deadline for the crawling process
     * @param parserFactory the {@link PageParserFactory} instance to create parsers for URLs
     * @param counts      a {@link ConcurrentMap} to store word counts
     * @param visitedUrls a {@link Set} to store visited URLs
     * @param ignoredUrls a {@link List} of {@link Pattern}s to ignore specific URLs
     */
    private final String url;
    private final List<Pattern> ignoredUrls;
    private final int maxDepth;
    private final Instant deadline;
    private final PageParserFactory parserFactory;
    private final ConcurrentMap<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final Clock clock;


    public CrawlTask(String url, int maxDepth, Instant deadline,
                     PageParserFactory parserFactory, ConcurrentMap<String, Integer> counts,
                     Set<String> visitedUrls,List<Pattern> ignoredUrls, Clock clock) {
        this.url = url;
        this.maxDepth = maxDepth;
        this.deadline = deadline;
        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
    }

    @Override
    protected void compute() {
        // Check if the maximum depth has been reached or the deadline has passed
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }

        // Check if the URL is in the ignoredUrls list
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        // Check if the URL has already been visited
        if (!visitedUrls.add(url)) {
            return;
        }

        // Parse the page
        PageParser.Result result = parserFactory.get(url).parse();

        // Update word counts
        result.getWordCounts().forEach((word, count) -> {
            counts.merge(word, count, Integer::sum);
        });

        // Create and invoke subtasks for each link found on the page
        List<CrawlTask> subtasks = result.getLinks().stream()
                .map(link -> new CrawlTask(link, maxDepth - 1, deadline, parserFactory, counts, visitedUrls,
                        ignoredUrls, clock))
                .collect(Collectors.toList());
        invokeAll(subtasks);
    }

}