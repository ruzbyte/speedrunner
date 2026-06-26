package com.ruzbyte.speedrunner.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ruzbyte.speedrunner.core.PersonalBest;
import com.ruzbyte.speedrunner.core.Run;
import com.ruzbyte.speedrunner.ports.SplitRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON-backed {@link SplitRepository} (Gson). Persists per-category personal bests and a history of
 * completed runs in a single file. Categories must be seeded in the file before they can be run (a
 * missing category loads as a layout-less personal best, which the timer rejects on start).
 *
 * <p>{@link #save(Run)} appends the run and improves the category's personal best via {@link
 * PersonalBest#improvedWith(Run)}.
 */
public final class JsonSplitRepository implements SplitRepository {

  private final Path filePath;
  private final Gson gson;

  /**
   * Creates a repository backed by the given JSON file.
   *
   * @param filePath the data file; need not exist yet
   */
  public JsonSplitRepository(final Path filePath) {
    this.filePath = filePath;
    this.gson =
        new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantAdapter().nullSafe())
            .registerTypeAdapter(Duration.class, new DurationAdapter().nullSafe())
            .setPrettyPrinting()
            .create();
  }

  @Override
  public PersonalBest load(final String category) {
    final PersonalBest stored = readStore().personalBests.get(category);
    return stored != null ? stored : new PersonalBest(category, List.of(), Duration.ZERO);
  }

  @Override
  public void save(final Run run) {
    final Store store = readStore();
    store.runs.add(run);
    final PersonalBest current = store.personalBests.get(run.category());
    final PersonalBest base =
        current != null ? current : new PersonalBest(run.category(), List.of(), Duration.ZERO);
    store.personalBests.put(run.category(), base.improvedWith(run));
    writeStore(store);
  }

  @Override
  public Set<String> categories() {
    return Set.copyOf(readStore().personalBests.keySet());
  }

  private Store readStore() {
    if (!Files.exists(filePath)) {
      return new Store();
    }
    try {
      final Store store = gson.fromJson(Files.readString(filePath), Store.class);
      return store != null ? store : new Store();
    } catch (final IOException e) {
      throw new UncheckedIOException("failed to read speedrun data from " + filePath, e);
    }
  }

  private void writeStore(final Store store) {
    try {
      Files.writeString(filePath, gson.toJson(store));
    } catch (final IOException e) {
      throw new UncheckedIOException("failed to write speedrun data to " + filePath, e);
    }
  }

  /** Root document persisted to JSON. Fields are populated reflectively by Gson. */
  private static final class Store {
    private Map<String, PersonalBest> personalBests = new LinkedHashMap<>();
    private List<Run> runs = new ArrayList<>();
  }

  /** Serialises {@link Instant} as an ISO-8601 string. */
  private static final class InstantAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(final JsonWriter out, final Instant value) throws IOException {
      out.value(value.toString());
    }

    @Override
    public Instant read(final JsonReader in) throws IOException {
      return Instant.parse(in.nextString());
    }
  }

  /** Serialises {@link Duration} as an ISO-8601 string. */
  private static final class DurationAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(final JsonWriter out, final Duration value) throws IOException {
      out.value(value.toString());
    }

    @Override
    public Duration read(final JsonReader in) throws IOException {
      return Duration.parse(in.nextString());
    }
  }
}
