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
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HtmlOrderedList}.
 *
 * @author Ahmed Ashour
 * @author Marc Guillemot
 * @author Ronald Brill
 */
public class HtmlOrderedListTest extends SimpleWebTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void asNormalizedText() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "</head><body>\n"
            + "  <ol id='foo'>\n"
            + "    <li>first item</li>\n"
            + "    <li>second item</li>\n"
            + "something without li node\n"
            + "    <li>third item</li>\n"
            + "  </ol>\n"
            + "  </table>\n"
            + "</body></html>";

        final HtmlPage page = loadPage(html);
        final HtmlElement node = page.getHtmlElementById("foo");
        final String expectedText = "1. first item"
                            + "\n2. second item"
                            + "\nsomething without li node"
                            + "\n3. third item";

        assertEquals(expectedText, node.asNormalizedText());
        assertEquals(expectedText, page.asNormalizedText());
    }

    /**
     * Browsers ignore closing information in a self closing OL tag.
     * @throws Exception if the test fails
     */
    @Test
    public void asXml() throws Exception {
        final String content = DOCTYPE_HTML
            + "<html><head></head><body>\n"
            + "<ol id='myNode'></ol>\n"
            + "foo\n"
            + "</form></body></html>";
        final HtmlPage page = loadPage(content);
        final HtmlElement element = page.getHtmlElementById("myNode");

        // assertEquals("<ol id=\"myNode\"></ol>", element.asXml());
        assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n"
                + "<html>\r\n"
                + "  <head/>\r\n"
                + "  <body>\r\n"
                + "    <ol id=\"myNode\"></ol>\n"
                + "foo\n"
                + "</body>\r\n"
                + "</html>", page.asXml());
    }
}
