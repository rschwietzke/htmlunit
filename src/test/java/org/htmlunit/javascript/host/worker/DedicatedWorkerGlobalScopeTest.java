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
package org.htmlunit.javascript.host.worker;

import java.net.URL;

import org.htmlunit.WebDriverTestCase;
import org.htmlunit.junit.annotation.Alerts;
import org.htmlunit.util.MimeType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@code DedicatedWorkerGlobalScope}.
 *
 * @author Ronald Brill
 * @author Rural Hunter
 */
public class DedicatedWorkerGlobalScopeTest extends WebDriverTestCase {

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: Result = 15")
    public void onmessage() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage([5, 3]);}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "onmessage = function(e) {\n"
                + "  var workerResult = 'Result = ' + (e.data[0] * e.data[1]);\n"
                + "  postMessage(workerResult);\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: Result = 15")
    public void selfOnmessage() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body><script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage([5, 3]);}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "self.onmessage = function(e) {\n"
                + "  var workerResult = 'Result = ' + (e.data[0] * e.data[1]);\n"
                + "  postMessage(workerResult);\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: Result = 15")
    public void selfAddEventListener() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body><script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage([5, 3]);}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "self.addEventListener('message', (e) => {\n"
                + "  var workerResult = 'Result = ' + (e.data[0] * e.data[1]);\n"
                + "  postMessage(workerResult);\n"
                + "});\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: timeout")
    public void selfSetTimeout() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body><script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "self.setTimeout(function() {\n"
                + "  postMessage('timeout');\n"
                + "}, 10);\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: interval")
    public void selfSetInterval() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body><script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "var id = self.setInterval(function() {\n"
                + "  postMessage('interval');\n"
                + "  clearInterval(id);\n"
                + "}, 10);\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    @Alerts("Received: func=function addEventListener() { [native code] }")
    public void functionDefaultValue() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body><script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "postMessage('func='+self.addEventListener);";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts(DEFAULT = "Received: Result = 15",
            FF = {},
            FF_ESR = {})
    public void workerCodeWithWrongMimeType() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js');\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage([5, 3]);}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "onmessage = function(e) {\n"
                + "  var workerResult = 'Result = ' + (e.data[0] * e.data[1]);\n"
                + "  postMessage(workerResult);\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_HTML);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: Bob [GSCE] [undefined]")
    public void workerName() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js', { name: 'Bob'});\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage('Heho');}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "onmessage = function(e) {\n"
                + "  let desc = Object.getOwnPropertyDescriptor(this, 'name');\n"
                + "  property = name + ' [';\n"
                + "  if (desc.get != undefined) property += 'G';\n"
                + "  if (desc.set != undefined) property += 'S';\n"
                + "  if (desc.writable) property += 'W';\n"
                + "  if (desc.configurable) property += 'C';\n"
                + "  if (desc.enumerable) property += 'E';\n"
                + "  property += ']'\n"

                + "  desc = Object.getOwnPropertyDescriptor(this.constructor.prototype, 'name');\n"
                + "  property += ' [' + desc + ']';\n"

                + "  postMessage(property);\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: Bob the builder")
    public void workerNameSet() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js', { name: 'Bob'});\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage('Heho');}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "onmessage = function(e) {\n"
                + "  name = name + ' the builder';\n"
                + "  postMessage(name);\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: working")
    public void workerOptionsUndefined() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js', undefined);\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage('Heho');}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "onmessage = function(e) {\n"
                + "  postMessage('working');\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    @Alerts("Received: working")
    public void workerOptionsNull() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><body>"
            + "<script>\n"
            + LOG_TITLE_FUNCTION
            + "try {\n"
            + "  var myWorker = new Worker('worker.js', null);\n"
            + "  myWorker.onmessage = function(e) {\n"
            + "    log('Received: ' + e.data);\n"
            + "  };\n"
            + "  setTimeout(function() { myWorker.postMessage('Heho');}, 10);\n"
            + "} catch(e) { logEx(e); }\n"
            + "</script></body></html>\n";

        final String workerJs = "onmessage = function(e) {\n"
                + "  postMessage('working');\n"
                + "}\n";

        getMockWebConnection().setResponse(new URL(URL_FIRST, "worker.js"), workerJs, MimeType.TEXT_JAVASCRIPT);

        loadPage2(html);
        verifyTitle2(DEFAULT_WAIT_TIME, getWebDriver(), getExpectedAlerts());
    }
}
