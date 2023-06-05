package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);
    // Check if the class contains a method with the @Profiled annotation
    boolean hasProfiledMethod = Arrays.stream(klass.getDeclaredMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));

    if (!hasProfiledMethod) {
      throw new IllegalArgumentException("The wrapped interface does not contain a @Profiled method");
    }
    return (T) Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class[]{klass},
            new ProfilingMethodInterceptor(delegate, klass, state,clock)
    );
  }

  @Override
  public void writeData(Path path) {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write profiling data to file", e);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
