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
package org.htmlunit;

import static org.apache.http.client.utils.DateUtils.formatDate;
import static org.htmlunit.HttpHeader.CACHE_CONTROL;
import static org.htmlunit.HttpHeader.ETAG;
import static org.htmlunit.HttpHeader.EXPIRES;
import static org.htmlunit.HttpHeader.IF_MODIFIED_SINCE;
import static org.htmlunit.HttpHeader.IF_NONE_MATCH;
import static org.htmlunit.HttpHeader.LAST_MODIFIED;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.http.HttpStatus;
import org.htmlunit.junit.annotation.Alerts;
import org.htmlunit.util.MimeType;
import org.htmlunit.util.NameValuePair;
import org.htmlunit.util.mocks.WebResponseMock;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Cache}.
 *
 * @author Marc Guillemot
 * @author Ahmed Ashour
 * @author Frank Danek
 * @author Anton Demydenko
 * @author Ronald Brill
 * @author Ashley Frieze
 * @author Lai Quang Duong
 */
public class CacheTest extends SimpleWebTestCase {

    private static final long ONE_MINUTE = 60_000L;
    private static final long ONE_HOUR = ONE_MINUTE * 60;

    private final long now_ = new Date().getTime();
    private final String tomorrow_ = formatDate(DateUtils.addDays(new Date(), 1));

    /**
     * Composite test of {@link Cache#isCacheableContent(WebResponse)}.
     */
    @Test
    public void isCacheableContent() {
        final Cache cache = new Cache();
        final Map<String, String> headers = new HashMap<>();
        final WebResponse response = new WebResponseMock(null, headers);

        assertFalse(cache.isCacheableContent(response));

        headers.put(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT");
        assertTrue(cache.isCacheableContent(response));

        headers.put(LAST_MODIFIED, formatDate(DateUtils.addMinutes(new Date(), -5)));
        assertTrue(cache.isCacheableContent(response));

        headers.put(LAST_MODIFIED, formatDate(new Date()));
        assertFalse(cache.isCacheableContent(response));

        headers.put(LAST_MODIFIED, formatDate(DateUtils.addMinutes(new Date(), 10)));
        assertFalse(cache.isCacheableContent(response));

        headers.put(EXPIRES, formatDate(DateUtils.addMinutes(new Date(), 5)));
        assertFalse(cache.isCacheableContent(response));

        headers.put(EXPIRES, formatDate(DateUtils.addHours(new Date(), 1)));
        assertTrue(cache.isCacheableContent(response));

        headers.remove(LAST_MODIFIED);
        assertTrue(cache.isCacheableContent(response));

        headers.put(EXPIRES, "0");
        assertFalse(cache.isCacheableContent(response));

        headers.put(EXPIRES, "-1");
        assertFalse(cache.isCacheableContent(response));

        headers.put(CACHE_CONTROL, "no-store");
        assertFalse(cache.isCacheableContent(response));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithNoHeadersIsNotCached() {
        assertFalse(Cache.isWithinCacheWindow(new WebResponseMock(null, null), now_, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithExpiryDateIsCached() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(EXPIRES, tomorrow_);

        assertTrue(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithExpiryDateInFutureButShortMaxAgeIsNotInCacheWindow() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(EXPIRES, tomorrow_);
        // max age is 1 second, so will have expired after a minute
        headers.put(CACHE_CONTROL, "some-other-value, max-age=1");

        assertFalse(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_ + ONE_MINUTE, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithExpiryDateInFutureButShortSMaxAgeIsNotInCacheWindow() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(EXPIRES, tomorrow_);
        // s max age is 1 second, so will have expired after a minute
        headers.put(CACHE_CONTROL, "some-other-value, s-maxage=1");

        assertFalse(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_ + ONE_MINUTE, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithBothMaxAgeAndSMaxUsesSMaxAsPriority() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(CACHE_CONTROL, "some-other-value, max-age=1200, s-maxage=1");

        assertFalse(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_ + ONE_MINUTE, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithMaxAgeInFutureWillBeCached() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(CACHE_CONTROL, "some-other-value, max-age=1200");

        assertTrue(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_, now_));

        headers.clear();
        headers.put(CACHE_CONTROL, "some-other-value, max-age=1200");

        assertTrue(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_ + ONE_MINUTE, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithLongLastModifiedTimeComparedToNowIsCachedOnDownload() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(LAST_MODIFIED, formatDate(DateUtils.addDays(new Date(), -1)));

        assertTrue(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithLastModifiedTimeIsCachedAfterAFewPercentOfCreationAge() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(LAST_MODIFIED, formatDate(DateUtils.addDays(new Date(), -1)));

        assertTrue(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_ + ONE_HOUR, now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void contentWithLastModifiedTimeIsNotCachedAfterALongerPeriod() {
        final Map<String, String> headers = new HashMap<>();
        headers.put(LAST_MODIFIED, formatDate(DateUtils.addDays(new Date(), -1)));

        assertFalse(Cache.isWithinCacheWindow(new WebResponseMock(null, headers), now_ + (ONE_HOUR * 5), now_));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void usage() throws Exception {
        final String content = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<script src='foo1.js'></script>\n"
            + "<script src='foo2.js'></script>\n"
            + "</head><body>\n"
            + "<a href='page2.html'>to page 2</a>\n"
            + "</body></html>";

        final String content2 = DOCTYPE_HTML
            + "<html><head><title>page 2</title>\n"
            + "<script src='foo2.js'></script>\n"
            + "</head><body>\n"
            + "<a href='page1.html'>to page 1</a>\n"
            + "</body></html>";

        final String script1 = "alert('in foo1');";
        final String script2 = "alert('in foo2');";

        final WebClient webClient = getWebClient();
        final MockWebConnection connection = new MockWebConnection();
        webClient.setWebConnection(connection);

        final URL urlPage1 = new URL(URL_FIRST, "page1.html");
        connection.setResponse(urlPage1, content);
        final URL urlPage2 = new URL(URL_FIRST, "page2.html");
        connection.setResponse(urlPage2, content2);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        connection.setResponse(new URL(URL_FIRST, "foo1.js"), script1, 200, "ok",
                MimeType.TEXT_JAVASCRIPT, headers);
        connection.setResponse(new URL(URL_FIRST, "foo2.js"), script2, 200, "ok",
                MimeType.TEXT_JAVASCRIPT, headers);

        final List<String> collectedAlerts = new ArrayList<>();
        webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

        final HtmlPage page1 = webClient.getPage(urlPage1);
        final String[] expectedAlerts = {"in foo1", "in foo2"};
        assertEquals(expectedAlerts, collectedAlerts);

        collectedAlerts.clear();
        page1.getAnchors().get(0).click();

        assertEquals(new String[] {"in foo2"}, collectedAlerts);
        assertEquals("no request for scripts should have been performed",
                urlPage2, connection.getLastWebRequest().getUrl());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void jsUrlEncoded() throws Exception {
        final String content = DOCTYPE_HTML
            + "<html>\n"
            + "<head>\n"
            + "  <title>page 1</title>\n"
            + "  <script src='foo1.js'></script>\n"
            + "  <script src='foo2.js?foo[1]=bar/baz'></script>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <a href='page2.html'>to page 2</a>\n"
            + "</body>\n"
            + "</html>";

        final String content2 = DOCTYPE_HTML
            + "<html>\n"
            + "<head>\n"
            + "  <title>page 2</title>\n"
            + "  <script src='foo2.js?foo[1]=bar/baz'></script>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <a href='page1.html'>to page 1</a>\n"
            + "</body>\n"
            + "</html>";

        final String script1 = "alert('in foo1');";
        final String script2 = "alert('in foo2');";

        final URL urlPage1 = new URL(URL_FIRST, "page1.html");
        getMockWebConnection().setResponse(urlPage1, content);
        final URL urlPage2 = new URL(URL_FIRST, "page2.html");
        getMockWebConnection().setResponse(urlPage2, content2);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        getMockWebConnection().setResponse(new URL(URL_FIRST, "foo1.js"), script1,
                200, "ok", MimeType.TEXT_JAVASCRIPT, headers);
        getMockWebConnection().setDefaultResponse(script2, 200, "ok", MimeType.TEXT_JAVASCRIPT, headers);

        final WebClient webClient = getWebClientWithMockWebConnection();

        final List<String> collectedAlerts = new ArrayList<>();
        webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

        final HtmlPage page1 = webClient.getPage(urlPage1);
        final String[] expectedAlerts = {"in foo1", "in foo2"};
        assertEquals(expectedAlerts, collectedAlerts);

        collectedAlerts.clear();
        page1.getAnchors().get(0).click();

        assertEquals(new String[] {"in foo2"}, collectedAlerts);
        assertEquals("no request for scripts should have been performed",
                urlPage2, getMockWebConnection().getLastWebRequest().getUrl());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void cssUrlEncoded() throws Exception {
        final String content = DOCTYPE_HTML
            + "<html>\n"
            + "<head>\n"
            + "  <title>page 1</title>\n"
            + "  <link href='foo1.css' type='text/css' rel='stylesheet'>\n"
            + "  <link href='foo2.js?foo[1]=bar/baz' type='text/css' rel='stylesheet'>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <a href='page2.html'>to page 2</a>\n"
            + "  <script>\n"
            + "    var sheets = document.styleSheets;\n"
            + "    alert(sheets.length);\n"
            + "    var rules = sheets[0].cssRules || sheets[0].rules;\n"
            + "    alert(rules.length);\n"
            + "    rules = sheets[1].cssRules || sheets[1].rules;\n"
            + "    alert(rules.length);\n"
            + "  </script>\n"
            + "</body>\n"
            + "</html>";

        final String content2 = DOCTYPE_HTML
            + "<html>\n"
            + "<head>\n"
            + "  <title>page 2</title>\n"
            + "  <link href='foo2.js?foo[1]=bar/baz' type='text/css' rel='stylesheet'>\n"
            + "</head>\n"
            + "<body>\n"
            + "  <a href='page1.html'>to page 1</a>\n"
            + "  <script>\n"
            + "    var sheets = document.styleSheets;\n"
            + "    alert(sheets.length);\n"
            + "    var rules = sheets[0].cssRules || sheets[0].rules;\n"
            + "    alert(rules.length);\n"
            + "  </script>\n"
            + "</body>\n"
            + "</html>";

        final URL urlPage1 = new URL(URL_FIRST, "page1.html");
        getMockWebConnection().setResponse(urlPage1, content);
        final URL urlPage2 = new URL(URL_FIRST, "page2.html");
        getMockWebConnection().setResponse(urlPage2, content2);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        getMockWebConnection().setResponse(new URL(URL_FIRST, "foo1.js"), "",
                200, "ok", MimeType.TEXT_CSS, headers);
        getMockWebConnection().setDefaultResponse("", 200, "ok", MimeType.TEXT_CSS, headers);

        final WebClient webClient = getWebClientWithMockWebConnection();

        final List<String> collectedAlerts = new ArrayList<>();
        webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));

        final HtmlPage page1 = webClient.getPage(urlPage1);
        final String[] expectedAlerts = {"2", "0", "0"};
        assertEquals(expectedAlerts, collectedAlerts);
        assertEquals(3, getMockWebConnection().getRequestCount());

        collectedAlerts.clear();
        page1.getAnchors().get(0).click();

        assertEquals(new String[] {"1", "0"}, collectedAlerts);
        assertEquals(4, getMockWebConnection().getRequestCount());
        assertEquals("no request for scripts should have been performed",
                urlPage2, getMockWebConnection().getLastWebRequest().getUrl());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void maxSizeMaintained() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<script src='foo1.js' type='text/javascript'/>\n"
            + "<script src='foo2.js' type='text/javascript'/>\n"
            + "</head><body>abc</body></html>";

        final WebClient client = getWebClient();
        client.getCache().setMaxSize(1);

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html);

        final List<NameValuePair> headers =
            Collections.singletonList(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        connection.setResponse(new URL(URL_FIRST, "foo1.js"), ";", 200, "ok", MimeType.TEXT_JAVASCRIPT, headers);
        connection.setResponse(new URL(URL_FIRST, "foo2.js"), ";", 200, "ok", MimeType.TEXT_JAVASCRIPT, headers);

        client.getPage(pageUrl);
        assertEquals(1, client.getCache().getSize());

        client.getCache().clear();
        assertEquals(0, client.getCache().getSize());
    }

    /**
     * TODO: improve CSS caching to cache a COPY of the object as stylesheet objects can be modified dynamically.
     * @throws Exception if the test fails
     */
    @Test
    public void cssIsCached() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<style>.x { color: red; }</style>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body onload='document.styleSheets.item(0); document.styleSheets.item(1);'>x</body>\n"
            + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html);

        final List<NameValuePair> headers =
            Collections.singletonList(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        connection.setResponse(new URL(URL_FIRST, "foo.css"), "", 200, "OK", MimeType.TEXT_CSS, headers);

        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
    }

    /**
     * Check for correct caching if the css request gets redirected.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void cssIsCachedIfUrlWasRedirected() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body onload='document.styleSheets.item(0); document.styleSheets.item(1);'>x</body>\n"
            + "</html>";

        final String css = ".x { color: red; }";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html);

        final URL cssUrl = new URL(URL_FIRST, "foo.css");
        final URL redirectUrl = new URL(URL_FIRST, "fooContent.css");

        List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair("Location", redirectUrl.toExternalForm()));
        connection.setResponse(cssUrl, "", 301, "Redirect", null, headers);

        headers = Collections.singletonList(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        connection.setResponse(redirectUrl, css, 200, "OK", MimeType.TEXT_CSS, headers);

        client.getPage(pageUrl);
        client.getPage(pageUrl);

        // page1.html - foo.css - fooContent.css - page1.html
        assertEquals(4, connection.getRequestCount());
        // foo.css - fooContent.css
        assertEquals(2, client.getCache().getSize());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void cssFromCacheIsUsed() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>x</body>\n"
            + "</html>";

        final String css = ".x { color: red; }";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html);

        final URL cssUrl = new URL(URL_FIRST, "foo.css");
        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        connection.setResponse(cssUrl, css, 200, "OK", MimeType.TEXT_CSS, headers);

        client.getPage(pageUrl);

        assertEquals(2, connection.getRequestCount());
        assertEquals(1, client.getCache().getSize());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void cssManuallyAddeToCache() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>\n"
            + "abc <div class='test'>def</div>\n"
            + "</body>\n"
            + "</html>";

        final String css = ".test { visibility: hidden; }";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html);

        final URL cssUrl = new URL(URL_FIRST, "foo.css");
        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        final WebRequest request = new WebRequest(cssUrl);
        final WebResponseData data = new WebResponseData(css.getBytes("UTF-8"),
                HttpStatus.OK_200, HttpStatus.OK_200_MSG, headers);
        final WebResponse response = new WebResponse(data, request, 100);
        client.getCache().cacheIfPossible(new WebRequest(cssUrl), response, headers);

        final HtmlPage page = client.getPage(pageUrl);
        assertEquals("abc", page.asNormalizedText());

        assertEquals(1, connection.getRequestCount());
        assertEquals(1, client.getCache().getSize());
    }

    /**
     * Test that content retrieved with XHR is cached when right headers are here.
     * @throws Exception if the test fails
     */
    @Test
    @Alerts({"hello", "hello"})
    public void xhrContentCached() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<script>\n"
            + "  function doTest() {\n"
            + "    var xhr = new XMLHttpRequest();\n"
            + "    xhr.open('GET', 'foo.txt', false);\n"
            + "    xhr.send('');\n"
            + "    alert(xhr.responseText);\n"
            + "    xhr.send('');\n"
            + "    alert(xhr.responseText);\n"
            + "  }\n"
            + "</script>\n"
            + "</head>\n"
            + "<body onload='doTest()'>x</body>\n"
            + "</html>";

        final MockWebConnection connection = getMockWebConnection();

        final List<NameValuePair> headers =
            Collections.singletonList(new NameValuePair(LAST_MODIFIED, "Sun, 15 Jul 2007 20:46:27 GMT"));
        connection.setResponse(new URL(URL_FIRST, "foo.txt"), "hello", 200, "OK", MimeType.TEXT_PLAIN, headers);

        loadPageWithAlerts(html);

        assertEquals(2, connection.getRequestCount());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void testNoStoreCacheControl() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>x</body>\n"
            + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(CACHE_CONTROL, "some-other-value, no-store"));

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers);
        connection.setResponse(new URL(URL_FIRST, "foo.css"), "", 200, "OK", MimeType.TEXT_JAVASCRIPT, headers);

        client.getPage(pageUrl);
        assertEquals(0, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());

        client.getPage(pageUrl);
        assertEquals(0, client.getCache().getSize());
        assertEquals(4, connection.getRequestCount());
    }

    /**
     * @throws Exception if an error occurs
     */
    @Test
    public void testNoCacheCacheControl() throws Exception {
        final String html = DOCTYPE_HTML
                + "<html><head><title>page 1</title>\n"
                + "</head>\n"
                + "<body>x</body>\n"
                + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final String date = "Thu, 02 Mar 2023 02:00:00 GMT";
        final String etag = "foo";
        final String lastModified = "Wed, 01 Mar 2023 01:00:00 GMT";

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair("Date", date));
        headers.add(new NameValuePair(CACHE_CONTROL, "some-other-value, no-cache"));
        headers.add(new NameValuePair(ETAG, etag));
        headers.add(new NameValuePair(LAST_MODIFIED, lastModified));

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers);

        client.getPage(pageUrl);
        assertEquals(1, client.getCache().getSize());

        final String updatedDate = "Thu, 02 Mar 2023 02:00:10 GMT";
        final List<NameValuePair> headers2 = new ArrayList<>();
        headers2.add(new NameValuePair("Date", updatedDate));
        headers2.add(new NameValuePair("Proxy-Authorization", "Basic YWxhZGRpbjpvcGVuc2VzYW1l"));
        headers2.add(new NameValuePair("X-Content-Type-Options", "nosniff"));
        connection.setResponse(pageUrl, html, 304, "Not Modified", "text/html;charset=ISO-8859-1", headers2);

        client.getPage(pageUrl);
        assertEquals(2, connection.getRequestCount());

        final WebRequest lastRequest = connection.getLastWebRequest();
        assertEquals(etag, lastRequest.getAdditionalHeader(IF_NONE_MATCH));
        assertEquals(lastModified, lastRequest.getAdditionalHeader(IF_MODIFIED_SINCE));
        assertEquals(1, client.getCache().getSize());

        WebResponse cached = client.getCache().getCachedResponse(connection.getLastWebRequest());
        assertEquals(updatedDate, cached.getResponseHeaderValue("Date"));
        assertEquals(null, cached.getResponseHeaderValue("Proxy-Authorization"));
        assertEquals(null, cached.getResponseHeaderValue("X-Content-Type-Options"));

        final String updatedEtag = "bar";
        final String updatedLastModified = "Wed, 01 Mar 2023 02:00:00 GMT";

        final List<NameValuePair> headers3 = new ArrayList<>();
        headers3.add(new NameValuePair(CACHE_CONTROL, "some-other-value, no-cache"));
        headers3.add(new NameValuePair(ETAG, updatedEtag));
        headers3.add(new NameValuePair(LAST_MODIFIED, updatedLastModified));
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers3);

        client.getPage(pageUrl);
        assertEquals(3, connection.getRequestCount());
        assertEquals(1, client.getCache().getSize());

        cached = client.getCache().getCachedResponse(connection.getLastWebRequest());
        assertEquals(null, cached.getResponseHeaderValue("Date"));
        assertEquals(updatedEtag, cached.getResponseHeaderValue(ETAG));
        assertEquals(updatedLastModified, cached.getResponseHeaderValue(LAST_MODIFIED));
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void testMaxAgeCacheControl() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>x</body>\n"
            + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Tue, 20 Feb 2018 10:00:00 GMT"));
        headers.add(new NameValuePair(CACHE_CONTROL, "some-other-value, max-age=1"));

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers);
        connection.setResponse(new URL(URL_FIRST, "foo.css"), "", 200, "OK", MimeType.TEXT_JAVASCRIPT, headers);

        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // resources should be still in cache
        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // wait for max-age seconds + 1 for recache
        Thread.sleep(2 * 1000);
        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(4, connection.getRequestCount());

        // wait for max-age seconds + 1 for recache
        Thread.sleep(2 * 1000);
        client.getCache().clearOutdated();
        assertEquals(0, client.getCache().getSize());
        assertEquals(4, connection.getRequestCount());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void testSMaxageCacheControl() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>x</body>\n"
            + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Tue, 20 Feb 2018 10:00:00 GMT"));
        headers.add(new NameValuePair(CACHE_CONTROL, "public, s-maxage=1, some-other-value, max-age=10"));

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers);
        connection.setResponse(new URL(URL_FIRST, "foo.css"), "", 200, "OK", MimeType.TEXT_JAVASCRIPT, headers);

        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // resources should be still in cache
        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // wait for s-maxage seconds + 1 for recache
        Thread.sleep(2 * 1000);
        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(4, connection.getRequestCount());

        // wait for s-maxage seconds + 1 for recache
        Thread.sleep(2 * 1000);
        client.getCache().clearOutdated();
        assertEquals(0, client.getCache().getSize());
        assertEquals(4, connection.getRequestCount());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void testExpiresCacheControl() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>x</body>\n"
            + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Tue, 20 Feb 2018 10:00:00 GMT"));
        final Date expi = new Date(System.currentTimeMillis() + 2 * 1000 + 10 * DateUtils.MILLIS_PER_MINUTE);
        headers.add(new NameValuePair(EXPIRES, new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").format(expi)));
        headers.add(new NameValuePair(CACHE_CONTROL, "public, some-other-value"));

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers);
        connection.setResponse(new URL(URL_FIRST, "foo.css"), "", 200, "OK", MimeType.TEXT_JAVASCRIPT, headers);

        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // resources should be still in cache
        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // wait for expires
        Thread.sleep(2 * 1000);
        client.getPage(pageUrl);
        assertEquals(0, client.getCache().getSize());
        assertEquals(4, connection.getRequestCount());
    }

    /**
     * @throws Exception if the test fails
     */
    @Test
    public void testMaxAgeOverrulesExpiresCacheControl() throws Exception {
        final String html = DOCTYPE_HTML
            + "<html><head><title>page 1</title>\n"
            + "<link rel='stylesheet' type='text/css' href='foo.css' />\n"
            + "</head>\n"
            + "<body>x</body>\n"
            + "</html>";

        final WebClient client = getWebClient();

        final MockWebConnection connection = new MockWebConnection();
        client.setWebConnection(connection);

        final List<NameValuePair> headers = new ArrayList<>();
        headers.add(new NameValuePair(LAST_MODIFIED, "Tue, 20 Feb 2018 10:00:00 GMT"));
        headers.add(new NameValuePair(EXPIRES, "0"));
        headers.add(new NameValuePair(CACHE_CONTROL, "max-age=20"));

        final URL pageUrl = new URL(URL_FIRST, "page1.html");
        connection.setResponse(pageUrl, html, 200, "OK", "text/html;charset=ISO-8859-1", headers);
        connection.setResponse(new URL(URL_FIRST, "foo.css"), "", 200, "OK", MimeType.TEXT_JAVASCRIPT, headers);

        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
        // resources should be still in cache
        client.getPage(pageUrl);
        assertEquals(2, client.getCache().getSize());
        assertEquals(2, connection.getRequestCount());
    }

    /**
     * Ensures {@link WebResponse#cleanUp()} is called for overflow deleted entries.
     * @throws Exception if the test fails
     */
    @Test
    public void cleanUpOverflow() throws Exception {
        final WebRequest request1 = new WebRequest(URL_FIRST, HttpMethod.GET);

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeader.EXPIRES, formatDate(DateUtils.addHours(new Date(), 1)));

        final WebResponseMock response1 = new WebResponseMock(request1, headers);

        final WebRequest request2 = new WebRequest(URL_SECOND, HttpMethod.GET);
        final WebResponseMock response2 = new WebResponseMock(request2, headers);

        final Cache cache = new Cache();
        cache.setMaxSize(1);
        cache.cacheIfPossible(request1, response1, null);
        assertEquals(0, response1.getCallCount("cleanUp"));
        assertEquals(0, response2.getCallCount("cleanUp"));
        assertEquals(6, response1.getCallCount("getResponseHeaderValue"));
        assertEquals(0, response2.getCallCount("getResponseHeaderValue"));

        Thread.sleep(10);
        cache.cacheIfPossible(request2, response2, null);
        assertEquals(1, response1.getCallCount("cleanUp"));
        assertEquals(0, response2.getCallCount("cleanUp"));
        assertEquals(6, response1.getCallCount("getResponseHeaderValue"));
        assertEquals(6, response2.getCallCount("getResponseHeaderValue"));
    }

    /**
     * Ensures {@link WebResponse#cleanUp()} is called on calling {@link Cache#clear()}.
     */
    @Test
    public void cleanUpOnClear() {
        final WebRequest request1 = new WebRequest(URL_FIRST, HttpMethod.GET);

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeader.EXPIRES, formatDate(DateUtils.addHours(new Date(), 1)));

        final WebResponseMock response1 = new WebResponseMock(request1, headers);

        final Cache cache = new Cache();
        cache.cacheIfPossible(request1, response1, null);
        assertEquals(0, response1.getCallCount("cleanUp"));
        assertEquals(6, response1.getCallCount("getResponseHeaderValue"));

        cache.clear();

        assertEquals(1, response1.getCallCount("cleanUp"));
        assertEquals(6, response1.getCallCount("getResponseHeaderValue"));
    }
}
