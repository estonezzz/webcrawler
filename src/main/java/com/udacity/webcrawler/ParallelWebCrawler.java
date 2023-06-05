package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.concurrent.ForkJoinTask.invokeAll;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int maxDepth;
  private final int popularWordCount;
//  private final ForkJoinPool pool;
  private final List<Pattern> ignoredUrls;
  private final PageParserFactory parserFactory;


  @Inject
  ParallelWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.maxDepth = maxDepth;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.ignoredUrls = ignoredUrls;
//    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = new ConcurrentSkipListSet<>();

    // Create and invoke CrawlTasks for each starting URL
    List<CrawlTask> tasks = startingUrls.stream()
            .map(url -> new CrawlTask(url, maxDepth, deadline, parserFactory, counts, visitedUrls, ignoredUrls, clock))
            .collect(Collectors.toList());
    invokeAll(tasks);

    // Build and return the CrawlResult
    Map<String, Integer> sortedCounts = WordCounts.sort(counts, popularWordCount);
    return new CrawlResult.Builder()
            .setWordCounts(sortedCounts)
            .setUrlsVisited(visitedUrls.size())
            .build();
  }
  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }



}
