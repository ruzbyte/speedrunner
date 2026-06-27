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
import java.util.List;

/**
 * JSON-backed {@link SplitRepository} (Gson). Persists the configured routes (per-game,
 * per-category personal bests with their split layout) and a history of completed runs in a single
 * file.
 *
 * <p>Routes are keyed by the {@code (game, category)} pair. {@link #saveLayout(PersonalBest)}
 * upserts a route so a freshly configured one is selectable on the next launch; {@link #save(Run)}
 * appends the run and improves its route's personal best via {@link
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
  public PersonalBest load(final String game, final String category) {
    final PersonalBest stored = find(readStore().personalBests, game, category);
    return stored != null
        ? stored
        : new PersonalBest(game, category, List.of(), List.of(), Duration.ZERO);
  }

  @Override
  public void save(final Run run) {
    final Store store = readStore();
    store.runs.add(run);
    final PersonalBest current = find(store.personalBests, run.game(), run.category());
    final PersonalBest base =
        current != null
            ? current
            : new PersonalBest(
                run.game(), run.category(), splitNamesOf(run), List.of(), Duration.ZERO);
    upsert(store.personalBests, base.improvedWith(run));
    writeStore(store);
  }

  @Override
  public void saveLayout(final PersonalBest layout) {
    final Store store = readStore();
    upsert(store.personalBests, layout);
    writeStore(store);
  }

  @Override
  public List<PersonalBest> layouts() {
    return List.copyOf(readStore().personalBests);
  }

  /** Returns the stored route matching the game and category, or {@code null} if none. */
  private static PersonalBest find(
      final List<PersonalBest> routes, final String game, final String category) {
    for (final PersonalBest route : routes) {
      if (route.game().equals(game) && route.category().equals(category)) {
        return route;
      }
    }
    return null;
  }

  /** Replaces the route with the same game and category, or appends it when new. */
  private static void upsert(final List<PersonalBest> routes, final PersonalBest route) {
    for (int i = 0; i < routes.size(); i++) {
      final PersonalBest existing = routes.get(i);
      if (existing.game().equals(route.game()) && existing.category().equals(route.category())) {
        routes.set(i, route);
        return;
      }
    }
    routes.add(route);
  }

  /** Derives the ordered split names from a run, for seeding a brand-new route's layout. */
  private static List<String> splitNamesOf(final Run run) {
    final List<String> names = new ArrayList<>(run.splits().size());
    run.splits().forEach(split -> names.add(split.name()));
    return names;
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
    private List<PersonalBest> personalBests = new ArrayList<>();
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
