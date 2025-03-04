/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2021 the original author or authors.
 */
package org.assertj.core.internal.files;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.util.Arrays.array;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.assertj.core.internal.Diff;
import org.assertj.core.util.Files;
import org.assertj.core.util.TextFileWriter;
import org.assertj.core.util.diff.Delta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for <code>{@link Diff#diff(File, File)}</code>.
 * 
 * @author Yvonne Wang
 */
class Diff_diff_File_Test {

  private static Diff diff;
  private static TextFileWriter writer;

  @BeforeAll
  static void setUpOnce() {
    diff = new Diff();
    writer = TextFileWriter.instance();
  }

  private File actual;
  private File expected;

  @BeforeEach
  void setUp() {
    actual = Files.newTemporaryFile();
    actual.deleteOnExit();
    expected = Files.newTemporaryFile();
    expected.deleteOnExit();
  }

  @Test
  void should_return_empty_diff_list_if_files_have_equal_content() throws IOException {
    // GIVEN
    String[] content = array("line0", "line1");
    writer.write(actual, content);
    writer.write(expected, content);
    // WHEN
    List<Delta<String>> diffs = diff.diff(actual, defaultCharset(), expected, defaultCharset());
    // THEN
    then(diffs).isEmpty();
  }

  @Test
  void should_return_diffs_if_files_do_not_have_equal_content() throws IOException {
    // GIVEN
    writer.write(actual, "line_0", "line_1");
    writer.write(expected, "line0", "line1");
    // WHEN
    List<Delta<String>> diffs = diff.diff(actual, defaultCharset(), expected, defaultCharset());
    // THEN
    then(diffs).singleElement().hasToString(format("Changed content at line 1:%n"
                                                   + "expecting:%n"
                                                   + "  [\"line0\",%n"
                                                   + "   \"line1\"]%n"
                                                   + "but was:%n"
                                                   + "  [\"line_0\",%n"
                                                   + "   \"line_1\"]%n"));
  }

  @Test
  void should_return_multiple_diffs_if_files_contain_multiple_differences() throws IOException {
    // GIVEN
    writer.write(actual, "line_0", "line1", "line_2");
    writer.write(expected, "line0", "line1", "line2");
    // WHEN
    List<Delta<String>> diffs = diff.diff(actual, defaultCharset(), expected, defaultCharset());
    // THEN
    then(diffs).hasSize(2);
    then(diffs.get(0)).hasToString(format("Changed content at line 1:%n"
                                          + "expecting:%n"
                                          + "  [\"line0\"]%n"
                                          + "but was:%n"
                                          + "  [\"line_0\"]%n"));
    then(diffs.get(1)).hasToString(format("Changed content at line 3:%n"
                                          + "expecting:%n"
                                          + "  [\"line2\"]%n"
                                          + "but was:%n"
                                          + "  [\"line_2\"]%n"));
  }

  @Test
  void should_be_able_to_detect_mixed_differences() throws IOException {
    // GIVEN
    // @format:off
    writer.write(actual,   "line1",                     "line2", "line3", "line4", "line5", "line 9", "line 10", "line 11");
    writer.write(expected, "line1", "line1a", "line1b", "line2", "line3", "line7", "line5");
    // @format:on
    // WHEN
    List<Delta<String>> diffs = diff.diff(actual, defaultCharset(), expected, defaultCharset());
    // THEN
    then(diffs).hasSize(3);
    then(diffs.get(0)).hasToString(format("Missing content at line 2:%n"
                                          + "  [\"line1a\",%n"
                                          + "   \"line1b\"]%n"));
    then(diffs.get(1)).hasToString(format("Changed content at line 6:%n"
                                          + "expecting:%n"
                                          + "  [\"line7\"]%n"
                                          + "but was:%n"
                                          + "  [\"line4\"]%n"));
    then(diffs.get(2)).hasToString(format("Extra content at line 8:%n"
                                          + "  [\"line 9\",%n"
                                          + "   \"line 10\",%n"
                                          + "   \"line 11\"]%n"));
  }

  @Test
  void should_return_diffs_if_content_of_actual_is_shorter_than_content_of_expected() throws IOException {
    // GIVEN
    writer.write(actual, "line_0");
    writer.write(expected, "line_0", "line_1");
    // WHEN
    List<Delta<String>> diffs = diff.diff(actual, defaultCharset(), expected, defaultCharset());
    // THEN
    then(diffs).singleElement().hasToString(format("Missing content at line 2:%n"
                                                   + "  [\"line_1\"]%n"));
  }

  @Test
  void should_return_diffs_if_content_of_actual_is_longer_than_content_of_expected() throws IOException {
    // GIVEN
    writer.write(actual, "line_0", "line_1");
    writer.write(expected, "line_0");
    // WHEN
    List<Delta<String>> diffs = diff.diff(actual, defaultCharset(), expected, defaultCharset());
    // THEN
    then(diffs).singleElement().hasToString(format("Extra content at line 2:%n"
                                                   + "  [\"line_1\"]%n"));
  }
}
