/*
 * Copyright (c) 2002-2023 Gargoyle Software Inc.
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
package com.gargoylesoftware.htmlunit.javascript.host.event;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebDriverTestCase;
import com.gargoylesoftware.htmlunit.html.HtmlPageTest;
import com.gargoylesoftware.htmlunit.junit.BrowserRunner;
import com.gargoylesoftware.htmlunit.junit.BrowserRunner.Alerts;
import com.gargoylesoftware.htmlunit.junit.BrowserRunner.HtmlUnitNYI;

/**
 * Tests for {@link TransitionEvent}.
 *
 * @author Ronald Brill
 */
@RunWith(BrowserRunner.class)
public class TransitionEventTest extends WebDriverTestCase {

    private static final String DUMP_EVENT_FUNCTION = "  function dump(event) {\n"
            + "    log(event);\n"
            + "    log(event.type);\n"
            + "    log(event.bubbles);\n"
            + "    log(event.cancelable);\n"
            + "    log(event.composed);\n"
            + "  }\n";

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"[object TransitionEvent]", "transition", "false", "false", "false"},
            IE = "exception")
    public void create_ctor() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent('transition');\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("exception")
    @HtmlUnitNYI(CHROME = {"[object TransitionEvent]", "undefined", "false", "false", "false"},
                EDGE = {"[object TransitionEvent]", "undefined", "false", "false", "false"},
                FF = {"[object TransitionEvent]", "undefined", "false", "false", "false"},
                FF_ESR = {"[object TransitionEvent]", "undefined", "false", "false", "false"})
    public void create_ctorWithoutType() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent();\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"[object TransitionEvent]", "42", "false", "false", "false"},
            IE = "exception")
    public void create_ctorNumericType() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent(42);\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"[object TransitionEvent]", "null", "false", "false", "false"},
            IE = "exception")
    public void create_ctorNullType() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent(null);\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("exception")
    public void create_ctorUnknownType() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent(unknown);\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"[object TransitionEvent]", "HtmlUnitEvent", "false", "false", "false"},
            IE = "exception")
    public void create_ctorArbitraryType() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent('HtmlUnitEvent');\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"[object TransitionEvent]", "transition", "false", "false", "false"},
            IE = "exception")
    public void create_ctorAllDetails() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var debug = {hello: 'world'};\n"
            + "      var event = new TransitionEvent('transition', {\n"
            + "        'propertyName': 'aPropertyName',\n"
            + "        'elapsedTime': 0.123,\n"
            + "        'pseudoElement': 'aPseudoElementName'\n"
            + "      });\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = {"[object TransitionEvent]", "transition", "false", "false", "false"},
            IE = "exception")
    public void create_ctorAllDetailsMissingData() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent('transition', {\n"
            + "      });\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("exception")
    @HtmlUnitNYI(CHROME = {"[object TransitionEvent]", "transition", "false", "false", "false"},
                EDGE = {"[object TransitionEvent]", "transition", "false", "false", "false"},
                FF = {"[object TransitionEvent]", "transition", "false", "false", "false"},
                FF_ESR = {"[object TransitionEvent]", "transition", "false", "false", "false"})
    public void create_ctorAllDetailsWrongData() throws Exception {
        final String html = HtmlPageTest.STANDARDS_MODE_PREFIX_
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var event = new TransitionEvent('transition', {\n"
            + "        'elapsedTime': 'ten'\n"
            + "      });\n"
            + "      dump(event);\n"
            + "    } catch (e) { log('exception') }\n"
            + "  }\n"
            + DUMP_EVENT_FUNCTION
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("true")
    public void inWindow() throws Exception {
        final String html
            = "<html>\n"
            + "<head>\n"
            + "  <script>\n"
            + LOG_TITLE_FUNCTION
            + "    function test() {\n"
            + "      log('TransitionEvent' in window);\n"
            + "    }\n"
            + "  </script>\n"
            + "</head>\n"
            + "<body onload='test()'>\n"
            + "</body>\n"
            + "</html>";

        loadPageVerifyTitle2(html);
    }
}