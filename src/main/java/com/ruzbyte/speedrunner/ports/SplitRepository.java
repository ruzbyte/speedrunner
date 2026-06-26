package com.ruzbyte.speedrunner.ports;

import com.ruzbyte.speedrunner.core.PersonalBest;
import com.ruzbyte.speedrunner.core.Run;

/**
 * Port for persisting completed runs and loading the personal-best reference for a category. The
 * core owns this interface; the JSON adapter implements it and is injected from {@code Main}.
 */
public interface SplitRepository {

  /**
   * Loads the personal-best reference for the given category.
   *
   * @param category the speedrun category to load
   * @return the stored personal best for the category
   */
  PersonalBest load(String category);

  /**
   * Persists a completed run.
   *
   * @param run the completed run to save
   */
  void save(Run run);
}
