package com.ruzbyte.speedrunner.ports;

import com.ruzbyte.speedrunner.core.PersonalBest;
import com.ruzbyte.speedrunner.core.Run;
import java.util.List;

/**
 * Port for persisting completed runs and loading the per-route reference for a game and category.
 * The core owns this interface; the JSON adapter implements it and is injected from {@code Main}.
 */
public interface SplitRepository {

  /**
   * Loads the personal-best reference for the given game and category.
   *
   * @param game the game to load
   * @param category the speedrun category to load
   * @return the stored personal best, or a layout-less personal best if the route is unknown
   */
  PersonalBest load(String game, String category);

  /**
   * Persists a completed run, improving the route's personal best where the run beats it.
   *
   * @param run the completed run to save
   */
  void save(Run run);

  /**
   * Persists a configured route (its game, category and split names) so it is available for
   * selection on the next launch, before any run has been completed against it.
   *
   * @param layout the route to persist
   */
  void saveLayout(PersonalBest layout);

  /**
   * Returns the configured routes (each with its game, category and split layout) for selection at
   * startup.
   *
   * @return the available routes
   */
  List<PersonalBest> layouts();
}
