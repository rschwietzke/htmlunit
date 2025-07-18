/*
 * Copyright (c) 2002-2025 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.htmlunit.javascript.regexp.mozilla.js1_2;

import org.htmlunit.WebDriverTestCase;
import org.htmlunit.junit.annotation.Alerts;
import org.junit.jupiter.api.Test;

/**
 * Tests originally in '/js/src/tests/js1_2/regexp/backslash.js'.
 *
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
public class BackslashTest extends WebDriverTestCase {

    /**
     * Tests 'abcde'.match(new RegExp('\e')).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("e")
    public void test1() throws Exception {
        test("'abcde'.match(new RegExp('\\e'))");
    }

    /**
     * Tests 'ab\\cde'.match(new RegExp('\\\\')).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("\\")
    public void test2() throws Exception {
        test("'ab\\\\cde'.match(new RegExp('\\\\\\\\'))");
    }

    /**
     * Tests 'ab\\cde'.match(/\\/).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("\\")
    public void test3() throws Exception {
        test("'ab\\\\cde'.match(/\\\\/)");
    }

    /**
     * Tests 'before ^$*+?.()|{}[] after'.match(new RegExp('\\^\\$\\*\\+\\?\\.\\(\\)\\|\\{\\}\\[\\]')).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("^$*+?.()|{}[]")
    public void test4() throws Exception {
        test("'before ^$*+?.()|{}[] after'.match("
                + "new RegExp('\\\\^\\\\$\\\\*\\\\+\\\\?\\\\.\\\\(\\\\)\\\\|\\\\{\\\\}\\\\[\\\\]'))");
    }

    /**
     * Tests 'before ^$*+?.()|{}[] after'.match(/\^\$\*\+\?\.\(\)\|\{\}\[\]/).
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("^$*+?.()|{}[]")
    public void test5() throws Exception {
        test("'before ^$*+?.()|{}[] after'.match(/\\^\\$\\*\\+\\?\\.\\(\\)\\|\\{\\}\\[\\]/)");
    }

    private void test(final String script) throws Exception {
        final String html = "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  log(" + script + ");\n"
            + "</script></head><body>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }
}
