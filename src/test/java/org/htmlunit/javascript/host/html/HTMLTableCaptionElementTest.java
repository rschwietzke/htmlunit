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
package org.htmlunit.javascript.host.html;

import org.htmlunit.WebDriverTestCase;
import org.htmlunit.junit.annotation.Alerts;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HTMLTableCaptionElement}.
 *
 * @author Daniel Gredler
 * @author Ronald Brill
 * @author Frank Danek
 */
public class HTMLTableCaptionElementTest extends WebDriverTestCase {

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts({"left", "right", "bottom", "top", "wrong", ""})
    public void getAlign() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>\n"
            + "  <table>\n"
            + "    <caption id='c1' align='left' ></caption>\n"
            + "    <caption id='c2' align='right' ></caption>\n"
            + "    <caption id='c3' align='bottom' ></caption>\n"
            + "    <caption id='c4' align='top' ></caption>\n"
            + "    <caption id='c5' align='wrong' ></caption>\n"
            + "    <caption id='c6' ></caption>\n"
            + "  </table>\n"

            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  for (var i = 1; i <= 6; i++) {\n"
            + "    log(document.getElementById('c' + i).align);\n"
            + "  }\n"
            + "</script>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts({"CenTer", "8", "foo", "left", "right", "bottom", "top"})
    public void setAlign() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>\n"
            + "  <table>\n"
            + "    <caption id='c1' align='left' ></caption>\n"
            + "  </table>\n"

            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function setAlign(elem, value) {\n"
            + "    try {\n"
            + "      elem.align = value;\n"
            + "    } catch(e) { logEx(e); }\n"
            + "    log(elem.align);\n"
            + "  }\n"

            + "  var elem = document.getElementById('c1');\n"
            + "  setAlign(elem, 'CenTer');\n"

            + "  setAlign(elem, '8');\n"
            + "  setAlign(elem, 'foo');\n"

            + "  setAlign(elem, 'left');\n"
            + "  setAlign(elem, 'right');\n"
            + "  setAlign(elem, 'bottom');\n"
            + "  setAlign(elem, 'top');\n"
            + "</script>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts({"undefined", "undefined", "undefined", "middle", "8", "BOTtom"})
    public void vAlign() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body><table>\n"
            + "  <caption id='c1' valign='top'>a</caption>\n"
            + "  <caption id='c2' valign='baseline'>b</caption>\n"
            + "  <caption id='c3' valign='3'>c</caption>\n"
            + "  <tr>\n"
            + "    <td>a</td>\n"
            + "    <td>b</td>\n"
            + "    <td>c</td>\n"
            + "  </tr>\n"
            + "</table>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function set(e, value) {\n"
            + "    try {\n"
            + "      e.vAlign = value;\n"
            + "    } catch(e) { logEx(e); }\n"
            + "  }\n"
            + "  var c1 = document.getElementById('c1');\n"
            + "  var c2 = document.getElementById('c2');\n"
            + "  var c3 = document.getElementById('c3');\n"
            + "  log(c1.vAlign);\n"
            + "  log(c2.vAlign);\n"
            + "  log(c3.vAlign);\n"
            + "  set(c1, 'middle');\n"
            + "  set(c2, 8);\n"
            + "  set(c3, 'BOTtom');\n"
            + "  log(c1.vAlign);\n"
            + "  log(c2.vAlign);\n"
            + "  log(c3.vAlign);\n"
            + "</script>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts({"<caption id=\"cap\">a</caption>", "new"})
    public void outerHTML() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html>\n"
            + "  <head>\n"
            + "    <script>\n"
            + LOG_TITLE_FUNCTION
            + "      function test() {\n"
            + "        log(document.getElementById('cap').outerHTML);\n"
            + "        document.getElementById('cap').outerHTML = '<div id=\"new\">text<div>';\n"
            + "        log(document.getElementById('new').id);\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='test()'>\n"
            + "  <table>\n"
            + "    <caption id='cap'>a</caption>\n"
            + "    <tr>\n"
            + "      <td>cell1</td>\n"
            + "    </tr>\n"
            + "  </table>\n"
            + "  </body>\n"
            + "</html>";

        loadPageVerifyTitle2(html);
    }
}
