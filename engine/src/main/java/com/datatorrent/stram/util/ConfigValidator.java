/**
 * Copyright (C) 2015 DataTorrent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.stram.util;

import java.util.regex.Pattern;

import org.apache.log4j.Level;

/**
 * <p>ConfigValidator class.</p>
 *
 * @since 1.0.2
 */
public class ConfigValidator
{
  private static Pattern LOGGERS_PATTERN = Pattern.compile("^(\\w+\\.?)+(\\*|\\w+)$");
  /**
   * Validates the logger pattern and the level.
   * @param pattern
   * @param level
   * @return
   */
  public static boolean validateLoggersLevel(String pattern, String level)
  {
    if (!LOGGERS_PATTERN.matcher(pattern).matches()) {
      return false;
    }
    if (Level.toLevel(level, null) == null) {
      return false;
    }
    return true;
  }
}
