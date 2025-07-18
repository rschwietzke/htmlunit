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
package org.htmlunit.javascript;

import org.htmlunit.WebDriverTestCase;
import org.htmlunit.junit.annotation.Alerts;
import org.junit.jupiter.api.Test;

/**
 * Tests for org.mozilla.javascript.Arguments, which is the object for "function.arguments".
 *
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Ronald Brill
 */
public class ArgumentsTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"0", "0", "1", "0"})
    public void arguments() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  log(test.arguments.length);\n"
            + "  test1('hi');\n"
            + "}\n"
            + "function test1(hello) {\n"
            + "  log(test.arguments.length);\n"
            + "  log(test1.arguments.length);\n"
            + "  log(arguments.callee.caller.arguments.length);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"null", "null"})
    public void argumentsShouldBeNullOutsideFunction() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "}\n"
            + "log(test.arguments);\n"
            + "test();\n"
            + "log(test.arguments);\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("callee,length")
    public void argumentsPropertyNames() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  let p = Object.getOwnPropertyNames(arguments);\n"
            + "  p.sort();\n"
            + "  log(p);"
            + "}\n"
            + "test();"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }


    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("callee,length")
    public void argumentsPropertyNamesStrict() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + "'use strict';"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  let p = Object.getOwnPropertyNames(arguments);\n"
            + "  p.sort();\n"
            + "  log(p);"
            + "}\n"
            + "test();"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("2")
    public void passedCountDifferentFromDeclared() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  test1('hi', 'there');\n"
            + "}\n"
            + "function test1() {\n"
            + "  log(test1.arguments.length);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"2", "world", "undefined", "undefined"})
    public void readOnlyWhenAccessedThroughFunction() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  test1('hello', 'world');\n"
            + "}\n"
            + "function test1() {\n"
            + "  test1.arguments[1] = 'hi';\n"
            + "  test1.arguments[3] = 'you';\n"
            + "  log(test1.arguments.length);\n"
            + "  log(test1.arguments[1]);\n"
            + "  log(test1.arguments[2]);\n"
            + "  log(test1.arguments[3]);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"2", "hi", "undefined", "you"})
    public void writableWithinFunction() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test1() {\n"
            + "  arguments[1] = 'hi';\n"
            + "  arguments[3] = 'you';\n"
            + "  log(arguments.length);\n"
            + "  log(arguments[1]);\n"
            + "  log(arguments[2]);\n"
            + "  log(arguments[3]);\n"
            + "}\n"
            + "test1('hello', 'world');\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("false")
    public void argumentsEqualsFnArguments() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test1() {\n"
            + "  log(arguments == test1.arguments);\n"
            + "}\n"
            + "test1('hello', 'world');\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("hi")
    public void argumentsAsParameter() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test1(arguments) {\n"
            + "  log(arguments);\n"
            + "}\n"
            + "test1('hi');\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"function/", "function/", "true"})
    public void argumentsCallee() throws Exception {
        final String html = DOCTYPE_HTML
                + "<html><body>"
                + "<script>\n"
                + "  'use strict';\n"
                + LOG_TITLE_FUNCTION
                + "function foo() {\n"
                + "  let desc = Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                + "  log(typeof desc.get + '/' + desc.get.name);\n"
                + "  log(typeof desc.set + '/' + desc.set.name);\n"
                + "  log(desc.get === desc.set);\n"
                + "}\n"
                + "foo();\n"
                + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"true", "true"})
    public void argumentsCalleeDifferentFunctions() throws Exception {
        final String html = DOCTYPE_HTML
                + "<html><body>"
                + "<script>\n"
                + "  'use strict';\n"
                + LOG_TITLE_FUNCTION
                + "function foo1() {\n"
                + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                + "}\n"
                + "function foo2() {\n"
                + "  return Object.getOwnPropertyDescriptor(arguments, 'callee');\n"
                + "}\n"
                + "let desc1 = foo1();\n"
                + "let desc2 = foo2();\n"
                + "log(desc1.get === desc2.get);\n"
                + "log(desc1.set === desc2.set);\n"
                + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }
}
