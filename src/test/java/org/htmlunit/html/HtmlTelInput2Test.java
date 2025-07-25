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
package org.htmlunit.html;

import org.htmlunit.SimpleWebTestCase;
import org.htmlunit.junit.annotation.Alerts;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HtmlTelInput}.
 *
 * @author Ronald Brill
 * @author Anton Demydenko
 */
public class HtmlTelInput2Test extends SimpleWebTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void typingAndClone() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='tel' id='foo'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        HtmlTelInput input = (HtmlTelInput) page.getElementById("foo");
        input = (HtmlTelInput) input.cloneNode(true);
        input.type("4711");
        assertEquals("", input.getValueAttribute());
        assertEquals("4711", input.getValue());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void typingAndReset() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='tel' id='foo'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        final HtmlTelInput input = (HtmlTelInput) page.getElementById("foo");

        input.type("4711");
        input.reset();
        input.type("0815");

        assertEquals("", input.getValueAttribute());
        assertEquals("0815", input.getValue());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void typingAndSetValueAttribute() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='tel' id='foo'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        final HtmlTelInput input = (HtmlTelInput) page.getElementById("foo");

        input.type("4711");
        input.setValueAttribute("");
        input.type("0815");

        assertEquals("", input.getValueAttribute());
        assertEquals("47110815", input.getValue());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void typingAndSetValue() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='tel' id='foo'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        final HtmlTelInput input = (HtmlTelInput) page.getElementById("foo");

        input.type("4711");
        input.setValue("");
        input.type("0815");

        assertEquals("", input.getValueAttribute());
        assertEquals("0815", input.getValue());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void patternValidation() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='tel' pattern='[0-9]{3}-[0-9]{3}-[0-9]{4}' id='foo'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        final HtmlTelInput input = (HtmlTelInput) page.getElementById("foo");

        // empty
        assertTrue(input.isValid());
        // invalid
        input.setValue("123-456-78901");
        assertFalse(input.isValid());
        // valid
        input.setValue("123-456-7890");
        assertTrue(input.isValid());
    }

    /**
     * @throws Exception
     *         if the test fails
     */
    @Test
    @Alerts({"true", "true", "true", "", "12345"})
    public void maxLengthValidation() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='tel' id='foo' maxLength='5'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        final HtmlInput input = (HtmlInput) page.getElementById("foo");
        assertEquals(getExpectedAlerts()[0], Boolean.toString(input.isValid()));
        input.type("12345");
        assertEquals(getExpectedAlerts()[1], Boolean.toString(input.isValid()));
        input.type("67890");
        assertEquals(getExpectedAlerts()[2], Boolean.toString(input.isValid()));
        assertEquals(getExpectedAlerts()[3], input.getValueAttribute());
        assertEquals(getExpectedAlerts()[4], input.getValue());
    }

    /**
     * @throws Exception
     *         if the test fails
     */
    @Test
    @Alerts({"true", "false", "true", "", "1234567890"})
    public void minLengthValidation() throws Exception {
        final String htmlContent = DOCTYPE_HTML
            + "<html>\n"
            + "<head></head>\n"
            + "<body>\n"
            + "<form id='form1'>\n"
            + "  <input type='text' id='foo' minLength='5'>\n"
            + "</form>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(htmlContent);

        final HtmlInput input = (HtmlInput) page.getElementById("foo");
        assertEquals(getExpectedAlerts()[0], Boolean.toString(input.isValid()));
        input.type("1234");
        assertEquals(getExpectedAlerts()[1], Boolean.toString(input.isValid()));
        input.type("567890");
        assertEquals(getExpectedAlerts()[2], Boolean.toString(input.isValid()));
        assertEquals(getExpectedAlerts()[3], input.getValueAttribute());
        assertEquals(getExpectedAlerts()[4], input.getValue());
    }
}
