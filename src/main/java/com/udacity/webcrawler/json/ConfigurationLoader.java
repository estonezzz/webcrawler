package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
  public CrawlerConfiguration load() {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      return read(reader);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read JSON configuration file", e);
    }

  }

  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  public static CrawlerConfiguration  read(Reader reader) {
    ObjectMapper objectMapper = new ObjectMapper();
    // Disable auto-closing of the input source
    objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    try {
      // Deserialize the JSON content from the reader into a Builder object
      CrawlerConfiguration.Builder builder = objectMapper.readValue(reader, CrawlerConfiguration.Builder.class);

      // Build the CrawlerConfiguration object using the Builder
      return builder.build();
    } catch (IOException e) {
      // Handle the exception if the JSON parsing fails
      throw new RuntimeException("Failed to parse JSON configuration", e);
    }

  }
}
