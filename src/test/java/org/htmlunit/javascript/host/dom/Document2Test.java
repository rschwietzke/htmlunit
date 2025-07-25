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
package org.htmlunit.javascript.host.dom;

import java.net.URL;

import org.htmlunit.MockWebConnection;
import org.htmlunit.WebDriverTestCase;
import org.htmlunit.junit.annotation.Alerts;
import org.htmlunit.junit.annotation.HtmlUnitNYI;
import org.htmlunit.util.MimeType;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Tests for {@link Document}.
 *
 * @author Ronald Brill
 * @author Marc Guillemot
 * @author Frank Danek
 * @author Madis Pärn
 * @author Ahmed Ashour
 */
public class Document2Test extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("InvalidCharacterError/DOMException")
    public void createElementWithAngleBrackets() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      var select = document.createElement('<select>');\n"
            + "      log(select.add == undefined);\n"
            + "    }\n"
            + "    catch(e) { logEx(e) }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("InvalidCharacterError/DOMException")
    public void createElementWithHtml() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    try {\n"
            + "      log(document.createElement('<div>').tagName);\n"
            + "      var select = document.createElement(\"<select id='mySelect'><option>hello</option>\");\n"
            + "      log(select.add == undefined);\n"
            + "      log(select.id);\n"
            + "      log(select.childNodes.length);\n"
            + "      var option = document.createElement(\"<option id='myOption'>\");\n"
            + "      log(option.tagName);\n"
            + "      log(option.id);\n"
            + "      log(option.childNodes.length);\n"
            + "    }\n"
            + "    catch(e) { logEx(e) }\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * Dedicated test for 3410657.
     *
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("false")
    public void createElementPrototype() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  var HAS_EXTENDED_CREATE_ELEMENT_SYNTAX = (function() {\n"
            + "    try {\n"
            + "      var el = document.createElement('<input name=\"x\">');\n"
            + "      return el.tagName.toLowerCase() === 'input' && el.name === 'x';\n"
            + "    } catch (err) {\n"
            + "      return false;\n"
            + "    }\n"
            + "  })();\n"
            + "  log(HAS_EXTENDED_CREATE_ELEMENT_SYNTAX);\n"
            + "</script></head><body>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("true")
    public void appendChild() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  var span = document.createElement('SPAN');\n"
            + "  var div = document.getElementById('d');\n"
            + "  div.appendChild(span);\n"
            + "  log(span === div.childNodes[0]);\n"
            + "}\n"
            + "</script></head><body onload='test()'>\n"
            + "<div id='d'></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("1")
    public void getElementByTagNameNS_includesHtml() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function doTest() {\n"
            + "    log(document.getElementsByTagNameNS('*', 'html').length);\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='doTest()'>\n"
            + "  <p>hello world</p>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"div1", "null", "2", "1"})
    public void importNode_deep() throws Exception {
        importNode(true);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"div1", "null", "0"})
    public void importNode_notDeep() throws Exception {
        importNode(false);
    }

    private void importNode(final boolean deep) throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    var node = document.importNode(document.getElementById('div1'), " + deep + ");\n"
            + "    log(node.id);\n"
            + "    log(node.parentNode);\n"
            + "    log(node.childNodes.length);\n"
            + "    if (node.childNodes.length != 0)\n"
            + "      log(node.childNodes[0].childNodes.length);\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "  <div id='div1'><div id='div1_1'><div id='div1_1_1'></div></div><div id='div1_2'></div></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * Test for issue 3560821.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"parent", "child"})
    public void importNodeWithNamespace() throws Exception {
        final MockWebConnection conn = getMockWebConnection();
        conn.setDefaultResponse(
                "<?xml version=\"1.0\"?><html xmlns=\"http://www.w3.org/1999/xhtml\"><div id='child'> </div></html>",
                200, "OK", MimeType.TEXT_XML);

        final String html =
            "<html xmlns='http://www.w3.org/1999/xhtml'>\n"
            + "<head><script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  if (!document.evaluate) { log('evaluate not available'); return; }\n"
            + "  var xmlhttp = new XMLHttpRequest();\n"
            + "  xmlhttp.open(\"GET\",\"content.xhtml\",true);\n"
            + "  xmlhttp.send();\n"
            + "  xmlhttp.onreadystatechange = function() {\n"
            + "    if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {\n"
            + "      var child = document.importNode(xmlhttp.responseXML.getElementById(\"child\"), true);\n"
            + "      document.getElementById(\"parent\").appendChild(child);\n"
            + "      var found = document.evaluate(\"//div[@id='parent']\", document, null,"
            +                      "XPathResult.FIRST_ORDERED_NODE_TYPE, null);\n"
            + "      log(found.singleNodeValue.id);\n"
            + "      found = document.evaluate(\"//div[@id='child']\", document, null,"
            +                      "XPathResult.FIRST_ORDERED_NODE_TYPE, null);\n"
            + "      log(found.singleNodeValue.id);\n"
            + "    }\n"
            + " }\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'>\n"
            + "  <div id='parent'></div>\n"
            + "</body></html>\n";

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * Test for issue 3560821.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"parent", "child", "child3"})
    public void importNodesWithNamespace() throws Exception {
        final MockWebConnection conn = getMockWebConnection();
        conn.setDefaultResponse(
                "<?xml version=\"1.0\"?><html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                + "<div id='child'><div id='child2'><div id='child3'>-</div></div></div></html>",
                200, "OK", MimeType.TEXT_XML);

        final String html =
            "<html xmlns='http://www.w3.org/1999/xhtml'>\n"
            + "<head><script>\n"
            + LOG_TITLE_FUNCTION
            + "function test() {\n"
            + "  if (!document.evaluate) { log('evaluate not available'); return; }\n"
            + "  var xmlhttp = new XMLHttpRequest();\n"
            + "  xmlhttp.open(\"GET\",\"content.xhtml\",true);\n"
            + "  xmlhttp.send();\n"
            + "  xmlhttp.onreadystatechange = function() {\n"
            + "    if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {\n"
            + "      var child = document.importNode(xmlhttp.responseXML.getElementById(\"child\"), true);\n"
            + "      document.getElementById(\"parent\").appendChild(child);\n"
            + "      var found = document.evaluate(\"//div[@id='parent']\", document, null,"
            +                      "XPathResult.FIRST_ORDERED_NODE_TYPE, null);\n"
            + "      log(found.singleNodeValue.id);\n"
            + "      found = document.evaluate(\"//div[@id='child']\", document, null,"
            +                      "XPathResult.FIRST_ORDERED_NODE_TYPE, null);\n"
            + "      log(found.singleNodeValue.id);\n"
            + "      found = document.evaluate(\"//div[@id='child3']\", document, null,"
            +                      "XPathResult.FIRST_ORDERED_NODE_TYPE, null);\n"
            + "      log(found.singleNodeValue.id);\n"
            + "    }\n"
            + "  }\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='test()'>\n"
            + "  <div id='parent'></div>\n"
            + "</body></html>\n";

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"div1", "null", "null"})
    public void adoptNode() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    var newDoc = document.implementation.createHTMLDocument('something');\n"
            + "    var node = newDoc.adoptNode(document.getElementById('div1'));\n"
            + "    log(node.id);\n"
            + "    log(node.parentNode);\n"
            + "    log(document.getElementById('div1'));\n"
            + "  }\n"
            + "</script></head><body onload='test()'>\n"
            + "  <div id='div1'><div id='div1_1'></div></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"null", "text1"})
    public void activeElement() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><script>\n"
            + "  alert(document.activeElement);\n"
            + "  function test() {\n"
            + "    alert(document.activeElement.id);\n"
            + "  }\n"
            + "</script></head>\n"
            + "<body>\n"
            + "  <input id='text1' onclick='test()'>\n"
            + "</body></html>";
        getMockWebConnection().setDefaultResponse("Error: not found", 404, "Not Found", MimeType.TEXT_HTML);

        final WebDriver driver = loadPage2(html);
        verifyAlerts(driver, getExpectedAlerts()[0]);
        Thread.sleep(100);
        driver.findElement(By.id("text1")).click();
        verifyAlerts(driver, getExpectedAlerts()[1]);
    }

    /**
     * Regression test for issue 1568.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"[object HTMLBodyElement]", "§§URL§§#", "§§URL§§#"})
    public void activeElement_iframe() throws Exception {
        final String html = DOCTYPE_HTML
                + "<html>\n"
                + "<head></head>\n"
                + "<body>\n"

                + "  <a id='insert' "
                        + "onclick=\"insertText("
                        + "'<html><head></head><body>first frame text</body></html>');\" href=\"#\">\n"
                        + "insert text to frame</a>\n"
                + "  <a id= 'update' "
                        + "onclick=\"insertText("
                        + "'<html><head></head><body>another frame text</body></html>');\" href=\"#\">\n"
                        + "change frame text again</a><br>\n"
                + "  <iframe id='innerFrame' name='innerFrame' src='frame1.html'></iframe>\n"

                + "  <script>\n"
                + "    alert(document.activeElement);\n"

                + "    function insertText(text) {\n"
                + "      with (innerFrame.document) {\n"
                + "        open();\n"
                + "        writeln(text);\n"
                + "        close();\n"
                + "      }\n"
                + "      alert(document.activeElement);\n"
                + "    }\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>";
        getMockWebConnection().setDefaultResponse("Error: not found", 404, "Not Found", MimeType.TEXT_HTML);

        getMockWebConnection().setResponse(new URL("http://example.com/frame1.html"), "");

        final WebDriver driver = loadPage2(html);

        expandExpectedAlertsVariables(URL_FIRST);
        verifyAlerts(driver, getExpectedAlerts()[0]);

        driver.findElement(By.id("insert")).click();
        verifyAlerts(driver, getExpectedAlerts()[1]);

        driver.switchTo().frame(driver.findElement(By.id("innerFrame")));
        assertEquals("first frame text", driver.findElement(By.tagName("body")).getText());

        driver.switchTo().defaultContent();
        driver.findElement(By.id("update")).click();
        verifyAlerts(driver, getExpectedAlerts()[2]);

        driver.switchTo().frame(driver.findElement(By.id("innerFrame")));
        assertEquals("another frame text", driver.findElement(By.tagName("body")).getText());
    }

    /**
     * Verifies that when we create a text node and append it to an existing DOM node,
     * its <tt>outerHTML</tt>, <tt>innerHTML</tt> and <tt>innerText</tt> properties are
     * properly escaped.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"<p>a & b</p> &amp; \u0162 \" '",
             "<p>a & b</p> &amp; \u0162 \" '",
             "<div id=\"div\">&lt;p&gt;a &amp; b&lt;/p&gt; &amp;amp; \u0162 \" '</div>",
             "&lt;p&gt;a &amp; b&lt;/p&gt; &amp;amp; \u0162 \" '",
             "<p>a & b</p> &amp; \u0162 \" '"})
    public void createTextNodeWithHtml() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html>\n"
            + "<body onload='test()'>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function test() {\n"
            + "    var node = document.createTextNode('<p>a & b</p> &amp; \\u0162 \" \\'');\n"
            + "    log(node.data);\n"
            + "    log(node.nodeValue);\n"
            + "    var div = document.getElementById('div');\n"
            + "    div.appendChild(node);\n"
            + "    log(div.outerHTML);\n"
            + "    log(div.innerHTML);\n"
            + "    log(div.innerText);\n"
            + "  }\n"
            + "</script>\n"
            + "<div id='div'></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts({"true", "true"})
    public void queryCommandEnabled() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body onload='x()'><iframe name='f' id='f'></iframe>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function x() {\n"
            + "  var d = window.frames['f'].document;\n"
            + "  try { log(d.queryCommandEnabled('SelectAll')); } catch(e) { logEx(e); }\n"
            + "  try { log(d.queryCommandEnabled('sElectaLL')); } catch(e) { logEx(e); }\n"
            + "}\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }


    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts(DEFAULT = {"true", "true", "true"},
            FF = {"false", "false", "false"},
            FF_ESR = {"false", "false", "false"})
    @HtmlUnitNYI(FF = {"true", "true", "true"},
            FF_ESR = {"true", "true", "true"})
    public void queryCommandEnabledDesignMode() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body onload='x()'><iframe name='f' id='f'></iframe>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function x() {\n"
            + "  var d = window.frames['f'].document;\n"
            + "  d.designMode = 'on';\n"
            + "  log(d.queryCommandEnabled('SelectAll'));\n"
            + "  log(d.queryCommandEnabled('selectall'));\n"
            + "  log(d.queryCommandEnabled('SeLeCtALL'));\n"
            + "}\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"bar", "null", "null"})
    public void getElementById() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function doTest() {\n"
            + "  log(top.document.getElementById('input1').value);\n"
            + "  log(document.getElementById(''));\n"
            + "  log(document.getElementById('non existing'));\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "<form id='form1'>\n"
            + "<input id='input1' name='foo' type='text' value='bar' />\n"
            + "</form>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }


    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"zero", "udef"})
    public void getElementByIdNull() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function doTest() {\n"
            + "  log(top.document.getElementById(null).name);\n"
            + "  log(top.document.getElementById(undefined).name);\n"
            + "}\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "<form id='form1'>\n"
            + "<input id='undefined' name='udef' type='text' value='u' />\n"
            + "<input id='null' name='zero' type='text' value='n' />\n"
            + "</form>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"bar", "null"})
    public void getElementById_resetId() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function doTest() {\n"
            + "  var input1 = top.document.getElementById('input1');\n"
            + "  input1.id = 'newId';\n"
            + "  log(top.document.getElementById('newId').value);\n"
            + "  log(top.document.getElementById('input1'));\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "<form id='form1'>\n"
            + "<input id='input1' name='foo' type='text' value='bar' />\n"
            + "</form>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("bar")
    public void getElementById_setNewId() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function doTest() {\n"
            + "  var div1 = document.getElementById('div1');\n"
            + "  div1.firstChild.id = 'newId';\n"
            + "  log(document.getElementById('newId').value);\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "<form id='form1'>\n"
            + "<div id='div1'><input name='foo' type='text' value='bar'></div>\n"
            + "</form>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * Regression test for bug 740665.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("id1")
    public void getElementById_divId() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "function doTest() {\n"
            + "  var element = document.getElementById('id1');\n"
            + "  log(element.id);\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "<div id='id1'></div></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * Regression test for bug 740665.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("script1")
    public void getElementById_scriptId() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script id='script1'>\n"
            + LOG_TITLE_FUNCTION
            + "function doTest() {\n"
            + "  log(top.document.getElementById('script1').id);\n"
            + "}\n"
            + "</script></head><body onload='doTest()'>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"first", "newest"})
    public void getElementById_alwaysFirstOneInDocumentOrder() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>\n"
            + "<span id='it' class='first'></span>\n"
            + "<span id='it' class='second'></span>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "log(document.getElementById('it').className);\n"
            + "var s = document.createElement('span');\n"
            + "s.id = 'it';\n"
            + "s.className = 'newest';\n"
            + "document.body.insertBefore(s, document.body.firstChild);\n"
            + "log(document.getElementById('it').className);\n"
            + "</script></body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void createStyleSheet() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function doTest() {\n"
            + "    if (document.createStyleSheet) {\n"
            + "      document.createStyleSheet('style.css');\n"
            + "      for (var si = 0; si < document.styleSheets.length; si++) {\n"
            + "        var sheet = document.styleSheets[si];\n"
            + "        log(sheet.href);\n"
            + "        log(sheet.owningElement.tagName);\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "  <div id='id1'></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void createStyleSheet_emptyUrl() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + LOG_TITLE_FUNCTION
            + "<script>\n"
            + "  function doTest() {\n"
            + "    if (document.createStyleSheet) {\n"
            + "      document.createStyleSheet(null);\n"
            + "      document.createStyleSheet('');\n"
            + "      for (var si = 0; si < document.styleSheets.length; si++) {\n"
            + "        var sheet = document.styleSheets[si];\n"
            + "        log(sheet.href);\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "  <div id='id1'></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void createStyleSheet_insertAt() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "  function doTest() {\n"
            + "    if (document.createStyleSheet) {\n"
            + "      document.createStyleSheet('minus1.css', -1);\n"
            + "      document.createStyleSheet('zero.css', 0);\n"
            + "      document.createStyleSheet('dotseven.css', 0.7);\n"
            + "      document.createStyleSheet('seven.css', 7);\n"
            + "      document.createStyleSheet('none.css');\n"
            + "      for (var si = 0; si < document.styleSheets.length; si++) {\n"
            + "        var sheet = document.styleSheets[si];\n"
            + "        log(sheet.href);\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "</script></head>\n"
            + "<body onload='doTest()'>\n"
            + "  <div id='id1'></div>\n"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"Initial State:loading", "Changed:interactive", "Changed:complete"})
    public void readyStateEventListener() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "</script></head>\n"
            + "<body>\n"
            + "<script>\n"
            + "    log('Initial State:' + document.readyState);\n"
            + "    document.addEventListener('readystatechange', function() {\n"
            + "        log('Changed:' + document.readyState);\n"
            + "    });\r\n"
            + "</script>"
            + "</body></html>";

        loadPageVerifyTitle2(html);
    }
}
