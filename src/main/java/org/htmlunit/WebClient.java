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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.htmlunit.BrowserVersionFeatures.HTTP_HEADER_CH_UA;
import static org.htmlunit.BrowserVersionFeatures.HTTP_HEADER_PRIORITY;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.htmlunit.attachment.Attachment;
import org.htmlunit.attachment.AttachmentHandler;
import org.htmlunit.csp.Policy;
import org.htmlunit.csp.url.URI;
import org.htmlunit.css.ComputedCssStyleDeclaration;
import org.htmlunit.cssparser.parser.CSSErrorHandler;
import org.htmlunit.cssparser.parser.javacc.CSS3Parser;
import org.htmlunit.html.BaseFrameElement;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.FrameWindow;
import org.htmlunit.html.FrameWindow.PageDenied;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlInlineFrame;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.XHtmlPage;
import org.htmlunit.html.parser.HTMLParser;
import org.htmlunit.html.parser.HTMLParserListener;
import org.htmlunit.http.HttpStatus;
import org.htmlunit.http.HttpUtils;
import org.htmlunit.httpclient.HttpClientConverter;
import org.htmlunit.javascript.AbstractJavaScriptEngine;
import org.htmlunit.javascript.DefaultJavaScriptErrorListener;
import org.htmlunit.javascript.HtmlUnitScriptable;
import org.htmlunit.javascript.JavaScriptEngine;
import org.htmlunit.javascript.JavaScriptErrorListener;
import org.htmlunit.javascript.background.JavaScriptJobManager;
import org.htmlunit.javascript.host.Location;
import org.htmlunit.javascript.host.Window;
import org.htmlunit.javascript.host.dom.Node;
import org.htmlunit.javascript.host.event.Event;
import org.htmlunit.javascript.host.file.Blob;
import org.htmlunit.javascript.host.html.HTMLIFrameElement;
import org.htmlunit.protocol.data.DataURLConnection;
import org.htmlunit.util.Cookie;
import org.htmlunit.util.HeaderUtils;
import org.htmlunit.util.MimeType;
import org.htmlunit.util.NameValuePair;
import org.htmlunit.util.UrlUtils;
import org.htmlunit.websocket.JettyWebSocketAdapter.JettyWebSocketAdapterFactory;
import org.htmlunit.websocket.WebSocketAdapter;
import org.htmlunit.websocket.WebSocketAdapterFactory;
import org.htmlunit.websocket.WebSocketListener;
import org.htmlunit.webstart.WebStartHandler;

/**
 * The main starting point in HtmlUnit: this class simulates a web browser.
 * <p>
 * A standard usage of HtmlUnit will start with using the {@link #getPage(String)} method
 * (or {@link #getPage(URL)}) to load a first {@link Page}
 * and will continue with further processing on this page depending on its type.
 * </p>
 * <b>Example:</b><br>
 * <br>
 * <code>
 * final WebClient webClient = new WebClient();<br>
 * final {@link HtmlPage} startPage = webClient.getPage("http://htmlunit.sf.net");<br>
 * assertEquals("HtmlUnit - Welcome to HtmlUnit", startPage.{@link HtmlPage#getTitleText() getTitleText}());
 * </code>
 * <p>
 * Note: a {@link WebClient} instance is <b>not thread safe</b>. It is intended to be used from a single thread.
 * </p>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:gudujarlson@sf.net">Mike J. Bresnahan</a>
 * @author Dominique Broeglin
 * @author Noboru Sinohara
 * @author <a href="mailto:chen_jun@users.sourceforge.net">Chen Jun</a>
 * @author David K. Taylor
 * @author <a href="mailto:cse@dynabean.de">Christian Sell</a>
 * @author <a href="mailto:bcurren@esomnie.com">Ben Curren</a>
 * @author Marc Guillemot
 * @author Chris Erskine
 * @author Daniel Gredler
 * @author Sergey Gorelkin
 * @author Hans Donner
 * @author Paul King
 * @author Ahmed Ashour
 * @author Bruce Chapman
 * @author Sudhan Moghe
 * @author Martin Tamme
 * @author Amit Manjhi
 * @author Nicolas Belisle
 * @author Ronald Brill
 * @author Frank Danek
 * @author Joerg Werner
 * @author Anton Demydenko
 * @author Sergio Moreno
 * @author Lai Quang Duong
 * @author René Schwietzke
 * @author Sven Strickroth
 */
@SuppressWarnings("PMD.TooManyFields")
public class WebClient implements Serializable, AutoCloseable {

    /** Logging support. */
    private static final Log LOG = LogFactory.getLog(WebClient.class);

    /** Like the Firefox default value for {@code network.http.redirection-limit}. */
    private static final int ALLOWED_REDIRECTIONS_SAME_URL = 20;
    private static final WebResponseData RESPONSE_DATA_NO_HTTP_RESPONSE = new WebResponseData(
            0, "No HTTP Response", Collections.emptyList());

    /**
     * These response headers are not copied from a 304 response to the cached
     * response headers. This list is based on Chromium http_response_headers.cc
     */
    private static final String[] DISCARDING_304_RESPONSE_HEADER_NAMES = {
        "connection",
        "proxy-connection",
        "keep-alive",
        "www-authenticate",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade",
        "content-location",
        "content-md5",
        "etag",
        "content-encoding",
        "content-range",
        "content-type",
        "content-length",
        "x-frame-options",
        "x-xss-protection",
    };

    private static final String[] DISCARDING_304_HEADER_PREFIXES = {
        "x-content-",
        "x-webkit-"
    };

    private transient WebConnection webConnection_;
    private CredentialsProvider credentialsProvider_ = new DefaultCredentialsProvider();
    private CookieManager cookieManager_ = new CookieManager();
    private WebSocketAdapterFactory webSocketAdapterFactory_;
    private transient AbstractJavaScriptEngine<?> scriptEngine_;
    private transient List<LoadJob> loadQueue_;
    private final Map<String, String> requestHeaders_ = Collections.synchronizedMap(new HashMap<>(89));
    private IncorrectnessListener incorrectnessListener_ = new IncorrectnessListenerImpl();
    private WebConsole webConsole_;
    private transient ExecutorService executor_;

    private AlertHandler alertHandler_;
    private ConfirmHandler confirmHandler_;
    private PromptHandler promptHandler_;
    private StatusHandler statusHandler_;
    private AttachmentHandler attachmentHandler_;
    private ClipboardHandler clipboardHandler_;
    private PrintHandler printHandler_;
    private WebStartHandler webStartHandler_;
    private FrameContentHandler frameContentHandler_;

    private AjaxController ajaxController_ = new AjaxController();

    private final BrowserVersion browserVersion_;
    private PageCreator pageCreator_ = new DefaultPageCreator();

    // we need a separate one to be sure the one is always informed as first
    // one. Only then we can make sure our state is consistent when the others
    // are informed.
    private CurrentWindowTracker currentWindowTracker_;
    private final Set<WebWindowListener> webWindowListeners_ = new HashSet<>(5);

    private final List<TopLevelWindow> topLevelWindows_ =
            Collections.synchronizedList(new ArrayList<>()); // top-level windows
    private final List<WebWindow> windows_ = Collections.synchronizedList(new ArrayList<>()); // all windows
    private transient List<WeakReference<JavaScriptJobManager>> jobManagers_ =
            Collections.synchronizedList(new ArrayList<>());
    private WebWindow currentWindow_;

    private HTMLParserListener htmlParserListener_;
    private CSSErrorHandler cssErrorHandler_ = new DefaultCssErrorHandler();
    private OnbeforeunloadHandler onbeforeunloadHandler_;
    private Cache cache_ = new Cache();

    // mini pool to save resource when parsing CSS
    private transient CSS3ParserPool css3ParserPool_ = new CSS3ParserPool();

    /** target "_blank". */
    public static final String TARGET_BLANK = "_blank";

    /** target "_self". */
    public static final String TARGET_SELF = "_self";

    /** target "_parent". */
    private static final String TARGET_PARENT = "_parent";
    /** target "_top". */
    private static final String TARGET_TOP = "_top";

    private ScriptPreProcessor scriptPreProcessor_;

    private RefreshHandler refreshHandler_ = new NiceRefreshHandler(2);
    private JavaScriptErrorListener javaScriptErrorListener_ = new DefaultJavaScriptErrorListener();

    private final WebClientOptions options_ = new WebClientOptions();
    private final boolean javaScriptEngineEnabled_;
    private final StorageHolder storageHolder_ = new StorageHolder();

    /**
     * Creates a web client instance using the browser version returned by
     * {@link BrowserVersion#getDefault()}.
     */
    public WebClient() {
        this(BrowserVersion.getDefault());
    }

    /**
     * Creates a web client instance using the specified {@link BrowserVersion}.
     * @param browserVersion the browser version to simulate
     */
    public WebClient(final BrowserVersion browserVersion) {
        this(browserVersion, null, -1);
    }

    /**
     * Creates an instance that will use the specified {@link BrowserVersion} and proxy server.
     * @param browserVersion the browser version to simulate
     * @param proxyHost the server that will act as proxy or null for no proxy
     * @param proxyPort the port to use on the proxy server
     */
    public WebClient(final BrowserVersion browserVersion, final String proxyHost, final int proxyPort) {
        this(browserVersion, true, proxyHost, proxyPort, null);
    }

    /**
     * Creates an instance that will use the specified {@link BrowserVersion} and proxy server.
     * @param browserVersion the browser version to simulate
     * @param proxyHost the server that will act as proxy or null for no proxy
     * @param proxyPort the port to use on the proxy server
     * @param proxyScheme the scheme http/https
     */
    public WebClient(final BrowserVersion browserVersion,
            final String proxyHost, final int proxyPort, final String proxyScheme) {
        this(browserVersion, true, proxyHost, proxyPort, proxyScheme);
    }

    /**
     * Creates an instance that will use the specified {@link BrowserVersion} and proxy server.
     * @param browserVersion the browser version to simulate
     * @param javaScriptEngineEnabled set to false if the simulated browser should not support javaScript
     * @param proxyHost the server that will act as proxy or null for no proxy
     * @param proxyPort the port to use on the proxy server
     */
    public WebClient(final BrowserVersion browserVersion, final boolean javaScriptEngineEnabled,
            final String proxyHost, final int proxyPort) {
        this(browserVersion, javaScriptEngineEnabled, proxyHost, proxyPort, null);
    }

    /**
     * Creates an instance that will use the specified {@link BrowserVersion} and proxy server.
     * @param browserVersion the browser version to simulate
     * @param javaScriptEngineEnabled set to false if the simulated browser should not support javaScript
     * @param proxyHost the server that will act as proxy or null for no proxy
     * @param proxyPort the port to use on the proxy server
     * @param proxyScheme the scheme http/https
     */
    public WebClient(final BrowserVersion browserVersion, final boolean javaScriptEngineEnabled,
            final String proxyHost, final int proxyPort, final String proxyScheme) {
        WebAssert.notNull("browserVersion", browserVersion);

        browserVersion_ = browserVersion;
        javaScriptEngineEnabled_ = javaScriptEngineEnabled;

        if (proxyHost == null) {
            getOptions().setProxyConfig(new ProxyConfig());
        }
        else {
            getOptions().setProxyConfig(new ProxyConfig(proxyHost, proxyPort, proxyScheme));
        }

        webConnection_ = new HttpWebConnection(this); // this has to be done after the browser version was set
        if (javaScriptEngineEnabled_) {
            scriptEngine_ = new JavaScriptEngine(this);
        }
        loadQueue_ = new ArrayList<>();

        webSocketAdapterFactory_ = new JettyWebSocketAdapterFactory();

        // The window must be constructed AFTER the script engine.
        currentWindowTracker_ = new CurrentWindowTracker(this, true);
        currentWindow_ = new TopLevelWindow("", this);
    }

    /**
     * Our simple impl of a ThreadFactory (decorator) to be able to name
     * our threads.
     */
    private static final class ThreadNamingFactory implements ThreadFactory {
        private static int ID_ = 1;
        private final ThreadFactory baseFactory_;

        ThreadNamingFactory(final ThreadFactory aBaseFactory) {
            baseFactory_ = aBaseFactory;
        }

        @Override
        public Thread newThread(final Runnable aRunnable) {
            final Thread thread = baseFactory_.newThread(aRunnable);
            thread.setName("WebClient Thread " + ID_++);
            return thread;
        }
    }

    /**
     * Returns the object that will resolve all URL requests.
     *
     * @return the connection that will be used
     */
    public WebConnection getWebConnection() {
        return webConnection_;
    }

    /**
     * Sets the object that will resolve all URL requests.
     *
     * @param webConnection the new web connection
     */
    public void setWebConnection(final WebConnection webConnection) {
        WebAssert.notNull("webConnection", webConnection);
        webConnection_ = webConnection;
    }

    /**
     * Send a request to a server and return a Page that represents the
     * response from the server. This page will be used to populate the provided window.
     * <p>
     * The returned {@link Page} will be created by the {@link PageCreator}
     * configured by {@link #setPageCreator(PageCreator)}, if any.
     * <p>
     * The {@link DefaultPageCreator} will create a {@link Page} depending on the content type of the HTTP response,
     * basically {@link HtmlPage} for HTML content, {@link org.htmlunit.xml.XmlPage} for XML content,
     * {@link TextPage} for other text content and {@link UnexpectedPage} for anything else.
     *
     * @param webWindow the WebWindow to load the result of the request into
     * @param webRequest the web request
     * @param <P> the page type
     * @return the page returned by the server when the specified request was made in the specified window
     * @throws IOException if an IO error occurs
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true
     *
     * @see WebRequest
     */
    public <P extends Page> P getPage(final WebWindow webWindow, final WebRequest webRequest)
            throws IOException, FailingHttpStatusCodeException {
        return getPage(webWindow, webRequest, true);
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Send a request to a server and return a Page that represents the
     * response from the server. This page will be used to populate the provided window.
     * <p>
     * The returned {@link Page} will be created by the {@link PageCreator}
     * configured by {@link #setPageCreator(PageCreator)}, if any.
     * <p>
     * The {@link DefaultPageCreator} will create a {@link Page} depending on the content type of the HTTP response,
     * basically {@link HtmlPage} for HTML content, {@link org.htmlunit.xml.XmlPage} for XML content,
     * {@link TextPage} for other text content and {@link UnexpectedPage} for anything else.
     *
     * @param webWindow the WebWindow to load the result of the request into
     * @param webRequest the web request
     * @param addToHistory true if the page should be part of the history
     * @param <P> the page type
     * @return the page returned by the server when the specified request was made in the specified window
     * @throws IOException if an IO error occurs
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true
     *
     * @see WebRequest
     */
    @SuppressWarnings("unchecked")
    <P extends Page> P getPage(final WebWindow webWindow, final WebRequest webRequest,
            final boolean addToHistory)
        throws IOException, FailingHttpStatusCodeException {

        final Page page = webWindow.getEnclosedPage();

        if (page != null) {
            final URL prev = page.getUrl();
            final URL current = webRequest.getUrl();
            if (UrlUtils.sameFile(current, prev)
                        && current.getRef() != null
                        && !Objects.equals(current.getRef(), prev.getRef())) {
                // We're just navigating to an anchor within the current page.
                page.getWebResponse().getWebRequest().setUrl(current);
                if (addToHistory) {
                    webWindow.getHistory().addPage(page);
                }

                // clear the cache because the anchors are now matched by
                // the target pseudo style
                if (page instanceof HtmlPage) {
                    ((HtmlPage) page).clearComputedStyles();
                }

                final Window window = webWindow.getScriptableObject();
                if (window != null) { // js enabled
                    window.getLocation().setHash(current.getRef());
                }
                return (P) page;
            }

            if (page.isHtmlPage()) {
                final HtmlPage htmlPage = (HtmlPage) page;
                if (!htmlPage.isOnbeforeunloadAccepted()) {
                    LOG.debug("The registered OnbeforeunloadHandler rejected to load a new page.");
                    return (P) page;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Get page for window named '" + webWindow.getName() + "', using " + webRequest);
        }

        WebResponse webResponse;
        final String protocol = webRequest.getUrl().getProtocol();
        if ("javascript".equals(protocol)) {
            webResponse = makeWebResponseForJavaScriptUrl(webWindow, webRequest.getUrl(), webRequest.getCharset());
            if (webWindow.getEnclosedPage() != null && webWindow.getEnclosedPage().getWebResponse() == webResponse) {
                // a javascript:... url with result of type undefined didn't changed the page
                return (P) webWindow.getEnclosedPage();
            }
        }
        else {
            try {
                webResponse = loadWebResponse(webRequest);
            }
            catch (final NoHttpResponseException e) {
                webResponse = new WebResponse(RESPONSE_DATA_NO_HTTP_RESPONSE, webRequest, 0);
            }
        }

        printContentIfNecessary(webResponse);
        loadWebResponseInto(webResponse, webWindow);

        // start execution here
        // note: we have to do this also if the server reports an error!
        //       e.g. if the server returns a 404 error page that includes javascript
        if (scriptEngine_ != null) {
            scriptEngine_.registerWindowAndMaybeStartEventLoop(webWindow);
        }

        // check and report problems if needed
        throwFailingHttpStatusCodeExceptionIfNecessary(webResponse);
        return (P) webWindow.getEnclosedPage();
    }

    /**
     * Convenient method to build a URL and load it into the current WebWindow as it would be done
     * by {@link #getPage(WebWindow, WebRequest)}.
     * @param url the URL of the new content; in contrast to real browsers plain file url's are not supported.
     *        You have to use the 'file', 'data', 'blob', 'http' or 'https' protocol.
     * @param <P> the page type
     * @return the new page
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true.
     * @throws IOException if an IO problem occurs
     * @throws MalformedURLException if no URL can be created from the provided string
     */
    public <P extends Page> P getPage(final String url) throws IOException, FailingHttpStatusCodeException,
        MalformedURLException {
        return getPage(UrlUtils.toUrlUnsafe(url));
    }

    /**
     * Convenient method to load a URL into the current top WebWindow as it would be done
     * by {@link #getPage(WebWindow, WebRequest)}.
     * @param url the URL of the new content; in contrast to real browsers plain file url's are not supported.
     *        You have to use the 'file', 'data', 'blob', 'http' or 'https' protocol.
     * @param <P> the page type
     * @return the new page
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true.
     * @throws IOException if an IO problem occurs
     */
    public <P extends Page> P getPage(final URL url) throws IOException, FailingHttpStatusCodeException {
        final WebRequest request = new WebRequest(url, getBrowserVersion().getHtmlAcceptHeader(),
                                                          getBrowserVersion().getAcceptEncodingHeader());
        request.setCharset(UTF_8);
        return getPage(getCurrentWindow().getTopWindow(), request);
    }

    /**
     * Convenient method to load a web request into the current top WebWindow.
     * @param request the request parameters
     * @param <P> the page type
     * @return the new page
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true.
     * @throws IOException if an IO problem occurs
     * @see #getPage(WebWindow,WebRequest)
     */
    public <P extends Page> P getPage(final WebRequest request) throws IOException,
        FailingHttpStatusCodeException {
        return getPage(getCurrentWindow().getTopWindow(), request);
    }

    /**
     * <p>Creates a page based on the specified response and inserts it into the specified window. All page
     * initialization and event notification is handled here.</p>
     *
     * <p>Note that if the page created is an attachment page, and an {@link AttachmentHandler} has been
     * registered with this client, the page is <b>not</b> loaded into the specified window; in this case,
     * the page is loaded into a new window, and attachment handling is delegated to the registered
     * <code>AttachmentHandler</code>.</p>
     *
     * @param webResponse the response that will be used to create the new page
     * @param webWindow the window that the new page will be placed within
     * @throws IOException if an IO error occurs
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true
     * @return the newly created page
     * @see #setAttachmentHandler(AttachmentHandler)
     */
    public Page loadWebResponseInto(final WebResponse webResponse, final WebWindow webWindow)
        throws IOException, FailingHttpStatusCodeException {
        return loadWebResponseInto(webResponse, webWindow, null);
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * <p>Creates a page based on the specified response and inserts it into the specified window. All page
     * initialization and event notification is handled here.</p>
     *
     * <p>Note that if the page created is an attachment page, and an {@link AttachmentHandler} has been
     * registered with this client, the page is <b>not</b> loaded into the specified window; in this case,
     * the page is loaded into a new window, and attachment handling is delegated to the registered
     * <code>AttachmentHandler</code>.</p>
     *
     * @param webResponse the response that will be used to create the new page
     * @param webWindow the window that the new page will be placed within
     * @param forceAttachmentWithFilename if not {@code null}, handle this as an attachment with the specified name
     *        or if an empty string ("") use the filename provided in the response
     * @throws IOException if an IO error occurs
     * @throws FailingHttpStatusCodeException if the server returns a failing status code AND the property
     *         {@link WebClientOptions#setThrowExceptionOnFailingStatusCode(boolean)} is set to true
     * @return the newly created page
     * @see #setAttachmentHandler(AttachmentHandler)
     */
    public Page loadWebResponseInto(final WebResponse webResponse, final WebWindow webWindow,
            String forceAttachmentWithFilename)
            throws IOException, FailingHttpStatusCodeException {
        WebAssert.notNull("webResponse", webResponse);
        WebAssert.notNull("webWindow", webWindow);

        if (webResponse.getStatusCode() == HttpStatus.NO_CONTENT_204) {
            return webWindow.getEnclosedPage();
        }

        if (webStartHandler_ != null && "application/x-java-jnlp-file".equals(webResponse.getContentType())) {
            webStartHandler_.handleJnlpResponse(webResponse);
            return webWindow.getEnclosedPage();
        }

        if (attachmentHandler_ != null
                && (forceAttachmentWithFilename != null || attachmentHandler_.isAttachment(webResponse))) {

            // check content disposition header for nothing provided
            if (StringUtils.isEmpty(forceAttachmentWithFilename)) {
                final String disp = webResponse.getResponseHeaderValue(HttpHeader.CONTENT_DISPOSITION);
                forceAttachmentWithFilename = Attachment.getSuggestedFilename(disp);
            }

            if (attachmentHandler_.handleAttachment(webResponse,
                        StringUtils.isEmpty(forceAttachmentWithFilename) ? null : forceAttachmentWithFilename)) {
                // the handling is done by the attachment handler;
                // do not open a new window
                return webWindow.getEnclosedPage();
            }

            final WebWindow w = openWindow(null, null, webWindow);
            final Page page = pageCreator_.createPage(webResponse, w);
            attachmentHandler_.handleAttachment(page,
                                StringUtils.isEmpty(forceAttachmentWithFilename) ? null : forceAttachmentWithFilename);
            return page;
        }

        final Page oldPage = webWindow.getEnclosedPage();
        if (oldPage != null) {
            // Remove the old page before create new one.
            oldPage.cleanUp();
        }

        Page newPage = null;
        FrameWindow.PageDenied pageDenied = PageDenied.NONE;
        if (windows_.contains(webWindow)) {
            if (webWindow instanceof FrameWindow) {
                final String contentSecurityPolicy =
                        webResponse.getResponseHeaderValue(HttpHeader.CONTENT_SECURIRY_POLICY);
                if (StringUtils.isNotBlank(contentSecurityPolicy)) {
                    final URL origin = UrlUtils.getUrlWithoutPathRefQuery(
                            ((FrameWindow) webWindow).getEnclosingPage().getUrl());
                    final URL source = UrlUtils.getUrlWithoutPathRefQuery(webResponse.getWebRequest().getUrl());
                    final Policy policy = Policy.parseSerializedCSP(contentSecurityPolicy,
                                                    Policy.PolicyErrorConsumer.ignored);
                    if (!policy.allowsFrameAncestor(
                            Optional.of(URI.parseURI(source.toExternalForm()).orElse(null)),
                            Optional.of(URI.parseURI(origin.toExternalForm()).orElse(null)))) {
                        pageDenied = PageDenied.BY_CONTENT_SECURIRY_POLICY;

                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Load denied by Content-Security-Policy: '" + contentSecurityPolicy + "' - "
                                    + webResponse.getWebRequest().getUrl() + "' does not permit framing.");
                        }
                    }
                }

                if (pageDenied == PageDenied.NONE) {
                    final String xFrameOptions = webResponse.getResponseHeaderValue(HttpHeader.X_FRAME_OPTIONS);
                    if ("DENY".equalsIgnoreCase(xFrameOptions)) {
                        pageDenied = PageDenied.BY_X_FRAME_OPTIONS;

                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Load denied by X-Frame-Options: DENY; - '"
                                    + webResponse.getWebRequest().getUrl() + "' does not permit framing.");
                        }
                    }
                }
            }

            if (pageDenied == PageDenied.NONE) {
                newPage = pageCreator_.createPage(webResponse, webWindow);
            }
            else {
                try {
                    final WebResponse aboutBlank = loadWebResponse(WebRequest.newAboutBlankRequest());
                    newPage = pageCreator_.createPage(aboutBlank, webWindow);
                    // TODO - maybe we have to attach to original request/response to the page

                    ((FrameWindow) webWindow).setPageDenied(pageDenied);
                }
                catch (final IOException ignored) {
                    // ignore
                }
            }

            if (windows_.contains(webWindow)) {
                fireWindowContentChanged(new WebWindowEvent(webWindow, WebWindowEvent.CHANGE, oldPage, newPage));

                // The page being loaded may already have been replaced by another page via JavaScript code.
                if (webWindow.getEnclosedPage() == newPage) {
                    newPage.initialize();
                    // hack: onload should be fired the same way for all type of pages
                    // here is a hack to handle non HTML pages
                    if (isJavaScriptEnabled()
                            && webWindow instanceof FrameWindow && !newPage.isHtmlPage()) {
                        final FrameWindow fw = (FrameWindow) webWindow;
                        final BaseFrameElement frame = fw.getFrameElement();
                        if (frame.hasEventHandlers("onload")) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Executing onload handler for " + frame);
                            }
                            final Event event = new Event(frame, Event.TYPE_LOAD);
                            ((Node) frame.getScriptableObject()).executeEventLocally(event);
                        }
                    }
                }
            }
        }
        return newPage;
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span>
     *
     * <p>Logs the response's content if its status code indicates a request failure and
     * {@link WebClientOptions#isPrintContentOnFailingStatusCode()} returns {@code true}.
     *
     * @param webResponse the response whose content may be logged
     */
    public void printContentIfNecessary(final WebResponse webResponse) {
        if (getOptions().isPrintContentOnFailingStatusCode()
                && !webResponse.isSuccess() && LOG.isInfoEnabled()) {
            final String contentType = webResponse.getContentType();
            LOG.info("statusCode=[" + webResponse.getStatusCode() + "] contentType=[" + contentType + "]");
            LOG.info(webResponse.getContentAsString());
        }
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span>
     *
     * <p>Throws a {@link FailingHttpStatusCodeException} if the request's status code indicates a request
     * failure and {@link WebClientOptions#isThrowExceptionOnFailingStatusCode()} returns {@code true}.
     *
     * @param webResponse the response which may trigger a {@link FailingHttpStatusCodeException}
     */
    public void throwFailingHttpStatusCodeExceptionIfNecessary(final WebResponse webResponse) {
        if (getOptions().isThrowExceptionOnFailingStatusCode() && !webResponse.isSuccessOrUseProxyOrNotModified()) {
            throw new FailingHttpStatusCodeException(webResponse);
        }
    }

    /**
     * Adds a header which will be sent with EVERY request from this client.
     * This list is empty per default; use this to add specific headers for your
     * case.
     * @param name the name of the header to add
     * @param value the value of the header to add
     * @see #removeRequestHeader(String)
     */
    public void addRequestHeader(final String name, final String value) {
        if (HttpHeader.COOKIE_LC.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Do not add 'Cookie' header, use .getCookieManager() instead");
        }
        requestHeaders_.put(name, value);
    }

    /**
     * Removes a header from being sent with EVERY request from this client.
     * This list is empty per default; use this method to remove specific headers
     * your have added using {{@link #addRequestHeader(String, String)} before.<br>
     * You can't use this to avoid sending standard headers like "Accept-Language"
     * or "Sec-Fetch-Dest".
     * @param name the name of the header to remove
     * @see #addRequestHeader
     */
    public void removeRequestHeader(final String name) {
        requestHeaders_.remove(name);
    }

    /**
     * Sets the credentials provider that will provide authentication information when
     * trying to access protected information on a web server. This information is
     * required when the server is using Basic HTTP authentication, NTLM authentication,
     * or Digest authentication.
     * @param credentialsProvider the new credentials provider to use to authenticate
     */
    public void setCredentialsProvider(final CredentialsProvider credentialsProvider) {
        WebAssert.notNull("credentialsProvider", credentialsProvider);
        credentialsProvider_ = credentialsProvider;
    }

    /**
     * Returns the credentials provider for this client instance. By default, this
     * method returns an instance of {@link DefaultCredentialsProvider}.
     * @return the credentials provider for this client instance
     */
    public CredentialsProvider getCredentialsProvider() {
        return credentialsProvider_;
    }

    /**
     * This method is intended for testing only - use at your own risk.
     * @return the current JavaScript engine (never {@code null})
     */
    public AbstractJavaScriptEngine<?> getJavaScriptEngine() {
        return scriptEngine_;
    }

    /**
     * This method is intended for testing only - use at your own risk.
     *
     * @param engine the new script engine to use
     */
    public void setJavaScriptEngine(final AbstractJavaScriptEngine<?> engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Can't set JavaScriptEngine to null");
        }
        scriptEngine_ = engine;
    }

    /**
     * Returns the cookie manager used by this web client.
     * @return the cookie manager used by this web client
     */
    public CookieManager getCookieManager() {
        return cookieManager_;
    }

    /**
     * Sets the cookie manager used by this web client.
     * @param cookieManager the cookie manager used by this web client
     */
    public void setCookieManager(final CookieManager cookieManager) {
        WebAssert.notNull("cookieManager", cookieManager);
        cookieManager_ = cookieManager;
    }

    /**
     * Sets the alert handler for this webclient.
     * @param alertHandler the new alerthandler or null if none is specified
     */
    public void setAlertHandler(final AlertHandler alertHandler) {
        alertHandler_ = alertHandler;
    }

    /**
     * Returns the alert handler for this webclient.
     * @return the alert handler or null if one hasn't been set
     */
    public AlertHandler getAlertHandler() {
        return alertHandler_;
    }

    /**
     * Sets the handler that will be executed when the JavaScript method Window.confirm() is called.
     * @param handler the new handler or null if no handler is to be used
     */
    public void setConfirmHandler(final ConfirmHandler handler) {
        confirmHandler_ = handler;
    }

    /**
     * Returns the confirm handler.
     * @return the confirm handler or null if one hasn't been set
     */
    public ConfirmHandler getConfirmHandler() {
        return confirmHandler_;
    }

    /**
     * Sets the handler that will be executed when the JavaScript method Window.prompt() is called.
     * @param handler the new handler or null if no handler is to be used
     */
    public void setPromptHandler(final PromptHandler handler) {
        promptHandler_ = handler;
    }

    /**
     * Returns the prompt handler.
     * @return the prompt handler or null if one hasn't been set
     */
    public PromptHandler getPromptHandler() {
        return promptHandler_;
    }

    /**
     * Sets the status handler for this webclient.
     * @param statusHandler the new status handler or null if none is specified
     */
    public void setStatusHandler(final StatusHandler statusHandler) {
        statusHandler_ = statusHandler;
    }

    /**
     * Returns the status handler for this {@link WebClient}.
     * @return the status handler or null if one hasn't been set
     */
    public StatusHandler getStatusHandler() {
        return statusHandler_;
    }

    /**
     * Returns the executor for this {@link WebClient}.
     * @return the executor
     */
    public synchronized Executor getExecutor() {
        if (executor_ == null) {
            final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            threadPoolExecutor.setThreadFactory(new ThreadNamingFactory(threadPoolExecutor.getThreadFactory()));
            // threadPoolExecutor.prestartAllCoreThreads();
            executor_ = threadPoolExecutor;
        }

        return executor_;
    }

    /**
     * Changes the ExecutorService for this {@link WebClient}.
     * You have to call this before the first use of the executor, otherwise
     * an IllegalStateExceptions is thrown.
     * @param executor the new Executor.
     */
    public synchronized void setExecutor(final ExecutorService executor) {
        if (executor_ != null) {
            throw new IllegalStateException("Can't change the executor after first use.");
        }

        executor_ = executor;
    }

    /**
     * Sets the javascript error listener for this {@link WebClient}.
     * When setting to null, the {@link DefaultJavaScriptErrorListener} is used.
     * @param javaScriptErrorListener the new JavaScriptErrorListener or null if none is specified
     */
    public void setJavaScriptErrorListener(final JavaScriptErrorListener javaScriptErrorListener) {
        if (javaScriptErrorListener == null) {
            javaScriptErrorListener_ = new DefaultJavaScriptErrorListener();
        }
        else {
            javaScriptErrorListener_ = javaScriptErrorListener;
        }
    }

    /**
     * Returns the javascript error listener for this {@link WebClient}.
     * @return the javascript error listener or null if one hasn't been set
     */
    public JavaScriptErrorListener getJavaScriptErrorListener() {
        return javaScriptErrorListener_;
    }

    /**
     * Returns the current browser version.
     * @return the current browser version
     */
    public BrowserVersion getBrowserVersion() {
        return browserVersion_;
    }

    /**
     * Returns the "current" window for this client. This window (or its top window) will be used
     * when <code>getPage(...)</code> is called without specifying a window.
     * @return the "current" window for this client
     */
    public WebWindow getCurrentWindow() {
        return currentWindow_;
    }

    /**
     * Sets the "current" window for this client. This is the window that will be used when
     * <code>getPage(...)</code> is called without specifying a window.
     * @param window the new "current" window for this client
     */
    public void setCurrentWindow(final WebWindow window) {
        WebAssert.notNull("window", window);
        if (currentWindow_ == window) {
            return;
        }
        // onBlur event is triggered for focused element of old current window
        if (currentWindow_ != null && !currentWindow_.isClosed()) {
            final Page enclosedPage = currentWindow_.getEnclosedPage();
            if (enclosedPage != null && enclosedPage.isHtmlPage()) {
                final DomElement focusedElement = ((HtmlPage) enclosedPage).getFocusedElement();
                if (focusedElement != null) {
                    focusedElement.fireEvent(Event.TYPE_BLUR);
                }
            }
        }
        currentWindow_ = window;

        // when marking an iframe window as current we have no need to move the focus
        final boolean isIFrame = currentWindow_ instanceof FrameWindow
                && ((FrameWindow) currentWindow_).getFrameElement() instanceof HtmlInlineFrame;
        if (!isIFrame) {
            //1. activeElement becomes focused element for new current window
            //2. onFocus event is triggered for focusedElement of new current window
            final Page enclosedPage = currentWindow_.getEnclosedPage();
            if (enclosedPage != null && enclosedPage.isHtmlPage()) {
                final HtmlPage enclosedHtmlPage = (HtmlPage) enclosedPage;
                final HtmlElement activeElement = enclosedHtmlPage.getActiveElement();
                if (activeElement != null) {
                    enclosedHtmlPage.setFocusedElement(activeElement, true);
                }
            }
        }
    }

    /**
     * Adds a listener for {@link WebWindowEvent}s. All events from all windows associated with this
     * client will be sent to the specified listener.
     * @param listener a listener
     */
    public void addWebWindowListener(final WebWindowListener listener) {
        WebAssert.notNull("listener", listener);
        webWindowListeners_.add(listener);
    }

    /**
     * Removes a listener for {@link WebWindowEvent}s.
     * @param listener a listener
     */
    public void removeWebWindowListener(final WebWindowListener listener) {
        WebAssert.notNull("listener", listener);
        webWindowListeners_.remove(listener);
    }

    private void fireWindowContentChanged(final WebWindowEvent event) {
        if (currentWindowTracker_ != null) {
            currentWindowTracker_.webWindowContentChanged(event);
        }
        for (final WebWindowListener listener : new ArrayList<>(webWindowListeners_)) {
            listener.webWindowContentChanged(event);
        }
    }

    private void fireWindowOpened(final WebWindowEvent event) {
        if (currentWindowTracker_ != null) {
            currentWindowTracker_.webWindowOpened(event);
        }
        for (final WebWindowListener listener : new ArrayList<>(webWindowListeners_)) {
            listener.webWindowOpened(event);
        }
    }

    private void fireWindowClosed(final WebWindowEvent event) {
        if (currentWindowTracker_ != null) {
            currentWindowTracker_.webWindowClosed(event);
        }

        for (final WebWindowListener listener : new ArrayList<>(webWindowListeners_)) {
            listener.webWindowClosed(event);
        }

        // to open a new top level window if all others are gone
        if (currentWindowTracker_ != null) {
            currentWindowTracker_.afterWebWindowClosedListenersProcessed(event);
        }
    }

    /**
     * Open a new window with the specified name. If the URL is non-null then attempt to load
     * a page from that location and put it in the new window.
     *
     * @param url the URL to load content from or null if no content is to be loaded
     * @param windowName the name of the new window
     * @return the new window
     */
    public WebWindow openWindow(final URL url, final String windowName) {
        WebAssert.notNull("windowName", windowName);
        return openWindow(url, windowName, getCurrentWindow());
    }

    /**
     * Open a new window with the specified name. If the URL is non-null then attempt to load
     * a page from that location and put it in the new window.
     *
     * @param url the URL to load content from or null if no content is to be loaded
     * @param windowName the name of the new window
     * @param opener the web window that is calling openWindow
     * @return the new window
     */
    public WebWindow openWindow(final URL url, final String windowName, final WebWindow opener) {
        final WebWindow window = openTargetWindow(opener, windowName, TARGET_BLANK);
        if (url == null) {
            initializeEmptyWindow(window, window.getEnclosedPage());
        }
        else {
            try {
                final WebRequest request = new WebRequest(url, getBrowserVersion().getHtmlAcceptHeader(),
                                                                getBrowserVersion().getAcceptEncodingHeader());
                request.setCharset(UTF_8);

                final Page openerPage = opener.getEnclosedPage();
                if (openerPage != null && openerPage.getUrl() != null) {
                    request.setRefererHeader(openerPage.getUrl());
                }
                getPage(window, request);
            }
            catch (final IOException e) {
                LOG.error("Error loading content into window", e);
            }
        }
        return window;
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Open the window with the specified name. The name may be a special
     * target name of _self, _parent, _top, or _blank. An empty or null
     * name is set to the default. The special target names are relative to
     * the opener window.
     *
     * @param opener the web window that is calling openWindow
     * @param windowName the name of the new window
     * @param defaultName the default target if no name is given
     * @return the new window
     */
    public WebWindow openTargetWindow(
            final WebWindow opener, final String windowName, final String defaultName) {

        WebAssert.notNull("opener", opener);
        WebAssert.notNull("defaultName", defaultName);

        String windowToOpen = windowName;
        if (windowToOpen == null || windowToOpen.isEmpty()) {
            windowToOpen = defaultName;
        }

        WebWindow webWindow = resolveWindow(opener, windowToOpen);

        if (webWindow == null) {
            if (TARGET_BLANK.equals(windowToOpen)) {
                windowToOpen = "";
            }
            webWindow = new TopLevelWindow(windowToOpen, this);
        }

        if (webWindow instanceof TopLevelWindow && webWindow != opener.getTopWindow()) {
            ((TopLevelWindow) webWindow).setOpener(opener);
        }

        return webWindow;
    }

    private WebWindow resolveWindow(final WebWindow opener, final String name) {
        if (name == null || name.isEmpty() || TARGET_SELF.equals(name)) {
            return opener;
        }

        if (TARGET_PARENT.equals(name)) {
            return opener.getParentWindow();
        }

        if (TARGET_TOP.equals(name)) {
            return opener.getTopWindow();
        }

        if (TARGET_BLANK.equals(name)) {
            return null;
        }

        // first search for frame windows inside our window hierarchy
        WebWindow window = opener;
        while (true) {
            final Page page = window.getEnclosedPage();
            if (page != null && page.isHtmlPage()) {
                try {
                    final FrameWindow frame = ((HtmlPage) page).getFrameByName(name);
                    final HtmlUnitScriptable scriptable = frame.getFrameElement().getScriptableObject();
                    if (scriptable instanceof HTMLIFrameElement) {
                        ((HTMLIFrameElement) scriptable).onRefresh();
                    }
                    return frame;
                }
                catch (final ElementNotFoundException expected) {
                    // Fall through
                }
            }

            if (window == window.getParentWindow()) {
                // TODO: should getParentWindow() return null on top windows?
                break;
            }
            window = window.getParentWindow();
        }

        try {
            return getWebWindowByName(name);
        }
        catch (final WebWindowNotFoundException expected) {
            // Fall through - a new window will be created below
        }
        return null;
    }

    /**
     * <p><span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span></p>
     *
     * Opens a new dialog window.
     * @param url the URL of the document to load and display
     * @param opener the web window that is opening the dialog
     * @param dialogArguments the object to make available inside the dialog via <code>window.dialogArguments</code>
     * @return the new dialog window
     * @throws IOException if there is an IO error
     */
    public DialogWindow openDialogWindow(final URL url, final WebWindow opener, final Object dialogArguments)
        throws IOException {

        WebAssert.notNull("url", url);
        WebAssert.notNull("opener", opener);

        final DialogWindow window = new DialogWindow(this, dialogArguments);

        final HtmlPage openerPage = (HtmlPage) opener.getEnclosedPage();
        final WebRequest request = new WebRequest(url, getBrowserVersion().getHtmlAcceptHeader(),
                                                        getBrowserVersion().getAcceptEncodingHeader());
        request.setCharset(UTF_8);

        if (openerPage != null) {
            request.setRefererHeader(openerPage.getUrl());
        }

        getPage(window, request);

        return window;
    }

    /**
     * Sets the object that will be used to create pages. Set this if you want
     * to customize the type of page that is returned for a given content type.
     *
     * @param pageCreator the new page creator
     */
    public void setPageCreator(final PageCreator pageCreator) {
        WebAssert.notNull("pageCreator", pageCreator);
        pageCreator_ = pageCreator;
    }

    /**
     * Returns the current page creator.
     *
     * @return the page creator
     */
    public PageCreator getPageCreator() {
        return pageCreator_;
    }

    /**
     * Returns the first {@link WebWindow} that matches the specified name.
     *
     * @param name the name to search for
     * @return the {@link WebWindow} with the specified name
     * @throws WebWindowNotFoundException if the {@link WebWindow} can't be found
     * @see #getWebWindows()
     * @see #getTopLevelWindows()
     */
    public WebWindow getWebWindowByName(final String name) throws WebWindowNotFoundException {
        WebAssert.notNull("name", name);

        for (final WebWindow webWindow : windows_) {
            if (name.equals(webWindow.getName())) {
                return webWindow;
            }
        }

        throw new WebWindowNotFoundException(name);
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Initializes a new web window for JavaScript.
     * @param webWindow the new WebWindow
     * @param page the page that will become the enclosing page
     */
    public void initialize(final WebWindow webWindow, final Page page) {
        WebAssert.notNull("webWindow", webWindow);

        if (isJavaScriptEngineEnabled()) {
            scriptEngine_.initialize(webWindow, page);
        }
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Initializes a new empty window for JavaScript.
     *
     * @param webWindow the new WebWindow
     * @param page the page that will become the enclosing page
     */
    public void initializeEmptyWindow(final WebWindow webWindow, final Page page) {
        WebAssert.notNull("webWindow", webWindow);

        if (isJavaScriptEngineEnabled()) {
            initialize(webWindow, page);
            ((Window) webWindow.getScriptableObject()).initialize();
        }
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Adds a new window to the list of available windows.
     *
     * @param webWindow the new WebWindow
     */
    public void registerWebWindow(final WebWindow webWindow) {
        WebAssert.notNull("webWindow", webWindow);
        if (windows_.add(webWindow)) {
            fireWindowOpened(new WebWindowEvent(webWindow, WebWindowEvent.OPEN, webWindow.getEnclosedPage(), null));
        }
        // register JobManager here but don't deregister in deregisterWebWindow as it can live longer
        jobManagers_.add(new WeakReference<>(webWindow.getJobManager()));
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Removes a window from the list of available windows.
     *
     * @param webWindow the window to remove
     */
    public void deregisterWebWindow(final WebWindow webWindow) {
        WebAssert.notNull("webWindow", webWindow);
        if (windows_.remove(webWindow)) {
            fireWindowClosed(new WebWindowEvent(webWindow, WebWindowEvent.CLOSE, webWindow.getEnclosedPage(), null));
        }
    }

    /**
     * Expands a relative URL relative to the specified base. In most situations
     * this is the same as <code>new URL(baseUrl, relativeUrl)</code> but
     * there are some cases that URL doesn't handle correctly. See
     * <a href="http://www.faqs.org/rfcs/rfc1808.html">RFC1808</a>
     * regarding Relative Uniform Resource Locators for more information.
     *
     * @param baseUrl the base URL
     * @param relativeUrl the relative URL
     * @return the expansion of the specified base and relative URLs
     * @throws MalformedURLException if an error occurred when creating a URL object
     */
    public static URL expandUrl(final URL baseUrl, final String relativeUrl) throws MalformedURLException {
        final String newUrl = UrlUtils.resolveUrl(baseUrl, relativeUrl);
        return UrlUtils.toUrlUnsafe(newUrl);
    }

    private WebResponse makeWebResponseForDataUrl(final WebRequest webRequest) throws IOException {
        final URL url = webRequest.getUrl();
        final DataURLConnection connection;
        connection = new DataURLConnection(url);

        final List<NameValuePair> responseHeaders = new ArrayList<>();
        responseHeaders.add(new NameValuePair(HttpHeader.CONTENT_TYPE_LC,
            connection.getMediaType() + ";charset=" + connection.getCharset()));

        try (InputStream is = connection.getInputStream()) {
            final DownloadedContent downloadedContent =
                    HttpWebConnection.downloadContent(is,
                            getOptions().getMaxInMemory(),
                            getOptions().getTempFileDirectory());
            final WebResponseData data = new WebResponseData(downloadedContent, 200, "OK", responseHeaders);
            return new WebResponse(data, url, webRequest.getHttpMethod(), 0);
        }
    }

    private static WebResponse makeWebResponseForAboutUrl(final WebRequest webRequest) throws MalformedURLException {
        final URL url = webRequest.getUrl();
        final String urlString = url.toExternalForm();
        if (UrlUtils.ABOUT_BLANK.equalsIgnoreCase(urlString)) {
            return new StringWebResponse("", UrlUtils.URL_ABOUT_BLANK);
        }

        final String urlWithoutQuery = StringUtils.substringBefore(urlString, "?");
        if (!"blank".equalsIgnoreCase(StringUtils.substringAfter(urlWithoutQuery, UrlUtils.ABOUT_SCHEME))) {
            throw new MalformedURLException(url + " is not supported, only about:blank is supported at the moment.");
        }
        return new StringWebResponse("", url);
    }

    /**
     * Builds a WebResponse for a file URL.
     * This first implementation is basic.
     * It assumes that the file contains an HTML page encoded with the specified encoding.
     * @param webRequest the request
     * @return the web response
     * @throws IOException if an IO problem occurs
     */
    private WebResponse makeWebResponseForFileUrl(final WebRequest webRequest) throws IOException {
        URL cleanUrl = webRequest.getUrl();
        if (cleanUrl.getQuery() != null) {
            // Get rid of the query portion before trying to load the file.
            cleanUrl = UrlUtils.getUrlWithNewQuery(cleanUrl, null);
        }
        if (cleanUrl.getRef() != null) {
            // Get rid of the ref portion before trying to load the file.
            cleanUrl = UrlUtils.getUrlWithNewRef(cleanUrl, null);
        }

        final WebResponse fromCache = getCache().getCachedResponse(webRequest);
        if (fromCache != null) {
            return new WebResponseFromCache(fromCache, webRequest);
        }

        String fileUrl = cleanUrl.toExternalForm();
        fileUrl = URLDecoder.decode(fileUrl, UTF_8.name());
        final File file = new File(fileUrl.substring(5));
        if (!file.exists()) {
            // construct 404
            final List<NameValuePair> compiledHeaders = new ArrayList<>();
            compiledHeaders.add(new NameValuePair(HttpHeader.CONTENT_TYPE, MimeType.TEXT_HTML));
            final WebResponseData responseData =
                new WebResponseData(
                        org.htmlunit.util.StringUtils
                            .toByteArray("File: " + file.getAbsolutePath(), UTF_8),
                    404, "Not Found", compiledHeaders);
            return new WebResponse(responseData, webRequest, 0);
        }

        final String contentType = guessContentType(file);

        final DownloadedContent content = new DownloadedContent.OnFile(file, false);
        final List<NameValuePair> compiledHeaders = new ArrayList<>();
        compiledHeaders.add(new NameValuePair(HttpHeader.CONTENT_TYPE, contentType));
        compiledHeaders.add(new NameValuePair(HttpHeader.LAST_MODIFIED,
                HttpUtils.formatDate(new Date(file.lastModified()))));
        final WebResponseData responseData = new WebResponseData(content, 200, "OK", compiledHeaders);
        final WebResponse webResponse = new WebResponse(responseData, webRequest, 0);
        getCache().cacheIfPossible(webRequest, webResponse, null);
        return webResponse;
    }

    private WebResponse makeWebResponseForBlobUrl(final WebRequest webRequest) {
        final Window window = getCurrentWindow().getScriptableObject();
        final Blob fileOrBlob = window.getDocument().resolveBlobUrl(webRequest.getUrl().toString());
        if (fileOrBlob == null) {
            throw JavaScriptEngine.typeError("Cannot load data from " + webRequest.getUrl());
        }

        final List<NameValuePair> headers = new ArrayList<>();
        final String type = fileOrBlob.getType();
        if (!StringUtils.isEmpty(type)) {
            headers.add(new NameValuePair(HttpHeader.CONTENT_TYPE, fileOrBlob.getType()));
        }
        if (fileOrBlob instanceof org.htmlunit.javascript.host.file.File) {
            final org.htmlunit.javascript.host.file.File file = (org.htmlunit.javascript.host.file.File) fileOrBlob;
            final String fileName = file.getName();
            if (!StringUtils.isEmpty(fileName)) {
                // https://datatracker.ietf.org/doc/html/rfc6266#autoid-10
                headers.add(new NameValuePair(HttpHeader.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\""));
            }
        }

        final DownloadedContent content = new DownloadedContent.InMemory(fileOrBlob.getBytes());
        final WebResponseData responseData = new WebResponseData(content, 200, "OK", headers);
        return new WebResponse(responseData, webRequest, 0);
    }

    /**
     * Tries to guess the content type of the file.<br>
     * This utility could be located in a helper class but we can compare this functionality
     * for instance with the "Helper Applications" settings of Mozilla and therefore see it as a
     * property of the "browser".
     * @param file the file
     * @return "application/octet-stream" if nothing could be guessed
     */
    public String guessContentType(final File file) {
        final String fileName = file.getName();
        final String fileNameLC = fileName.toLowerCase(Locale.ROOT);
        if (fileNameLC.endsWith(".xhtml")) {
            // Java's mime type map returns application/xml in JDK8.
            return MimeType.APPLICATION_XHTML;
        }

        // Java's mime type map does not know these in JDK8.
        if (fileNameLC.endsWith(".js")) {
            return MimeType.TEXT_JAVASCRIPT;
        }

        if (fileNameLC.endsWith(".css")) {
            return MimeType.TEXT_CSS;
        }

        String contentType = null;
        if (!fileNameLC.endsWith(".php")) {
            contentType = URLConnection.guessContentTypeFromName(fileName);
        }
        if (contentType == null) {
            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
                contentType = URLConnection.guessContentTypeFromStream(inputStream);
            }
            catch (final IOException ignored) {
                // Ignore silently.
            }
        }
        if (contentType == null) {
            contentType = MimeType.APPLICATION_OCTET_STREAM;
        }
        return contentType;
    }

    private WebResponse makeWebResponseForJavaScriptUrl(final WebWindow webWindow, final URL url,
        final Charset charset) throws FailingHttpStatusCodeException, IOException {

        HtmlPage page = null;
        if (webWindow instanceof FrameWindow) {
            final FrameWindow frameWindow = (FrameWindow) webWindow;
            page = (HtmlPage) frameWindow.getEnclosedPage();
        }
        else {
            final Page currentPage = webWindow.getEnclosedPage();
            if (currentPage instanceof HtmlPage) {
                page = (HtmlPage) currentPage;
            }
        }

        if (page == null) {
            page = getPage(webWindow, WebRequest.newAboutBlankRequest());
        }
        final ScriptResult r = page.executeJavaScript(url.toExternalForm(), "JavaScript URL", 1);
        if (r.getJavaScriptResult() == null || ScriptResult.isUndefined(r)) {
            // No new WebResponse to produce.
            return webWindow.getEnclosedPage().getWebResponse();
        }

        final String contentString = r.getJavaScriptResult().toString();
        final StringWebResponse response = new StringWebResponse(contentString, charset, url);
        response.setFromJavascript(true);
        return response;
    }

    /**
     * Loads a {@link WebResponse} from the server.
     * @param webRequest the request
     * @throws IOException if an IO problem occurs
     * @return the WebResponse
     */
    public WebResponse loadWebResponse(final WebRequest webRequest) throws IOException {
        final String protocol = webRequest.getUrl().getProtocol();
        switch (protocol) {
            case UrlUtils.ABOUT:
                return makeWebResponseForAboutUrl(webRequest);

            case "file":
                return makeWebResponseForFileUrl(webRequest);

            case "data":
                return makeWebResponseForDataUrl(webRequest);

            case "blob":
                return makeWebResponseForBlobUrl(webRequest);

            case "http":
            case "https":
                return loadWebResponseFromWebConnection(webRequest, ALLOWED_REDIRECTIONS_SAME_URL);

            default:
                throw new IOException("Unsupported protocol '" + protocol + "'");
        }
    }

    /**
     * Loads a {@link WebResponse} from the server through the WebConnection.
     * @param webRequest the request
     * @param allowedRedirects the number of allowed redirects remaining
     * @throws IOException if an IO problem occurs
     * @return the resultant {@link WebResponse}
     */
    private WebResponse loadWebResponseFromWebConnection(final WebRequest webRequest,
        final int allowedRedirects) throws IOException {

        URL url = webRequest.getUrl();
        final HttpMethod method = webRequest.getHttpMethod();
        final List<NameValuePair> parameters = webRequest.getRequestParameters();

        WebAssert.notNull("url", url);
        WebAssert.notNull("method", method);
        WebAssert.notNull("parameters", parameters);

        url = UrlUtils.encodeUrl(url, webRequest.getCharset());
        webRequest.setUrl(url);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Load response for " + method + " " + url.toExternalForm());
        }

        // If the request settings don't specify a custom proxy, use the default client proxy...
        if (webRequest.getProxyHost() == null) {
            final ProxyConfig proxyConfig = getOptions().getProxyConfig();
            if (proxyConfig.getProxyAutoConfigUrl() != null) {
                if (!UrlUtils.sameFile(new URL(proxyConfig.getProxyAutoConfigUrl()), url)) {
                    String content = proxyConfig.getProxyAutoConfigContent();
                    if (content == null) {
                        content = getPage(proxyConfig.getProxyAutoConfigUrl())
                            .getWebResponse().getContentAsString();
                        proxyConfig.setProxyAutoConfigContent(content);
                    }
                    final String allValue = JavaScriptEngine.evaluateProxyAutoConfig(getBrowserVersion(), content, url);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Proxy Auto-Config: value '" + allValue + "' for URL " + url);
                    }
                    String value = allValue.split(";")[0].trim();
                    if (value.startsWith("PROXY")) {
                        value = value.substring(6);
                        final int colonIndex = value.indexOf(':');
                        webRequest.setSocksProxy(false);
                        webRequest.setProxyHost(value.substring(0, colonIndex));
                        webRequest.setProxyPort(Integer.parseInt(value.substring(colonIndex + 1)));
                    }
                    else if (value.startsWith("SOCKS")) {
                        value = value.substring(6);
                        final int colonIndex = value.indexOf(':');
                        webRequest.setSocksProxy(true);
                        webRequest.setProxyHost(value.substring(0, colonIndex));
                        webRequest.setProxyPort(Integer.parseInt(value.substring(colonIndex + 1)));
                    }
                }
            }
            // ...unless the host needs to bypass the configured client proxy!
            else if (!proxyConfig.shouldBypassProxy(webRequest.getUrl().getHost())) {
                webRequest.setProxyHost(proxyConfig.getProxyHost());
                webRequest.setProxyPort(proxyConfig.getProxyPort());
                webRequest.setProxyScheme(proxyConfig.getProxyScheme());
                webRequest.setSocksProxy(proxyConfig.isSocksProxy());
            }
        }

        // Add the headers that are sent with every request.
        addDefaultHeaders(webRequest);

        // Retrieve the response, either from the cache or from the server.
        final WebResponse fromCache = getCache().getCachedResponse(webRequest);
        final WebResponse webResponse = getWebResponseOrUseCached(webRequest, fromCache);

        // Continue according to the HTTP status code.
        final int status = webResponse.getStatusCode();
        if (status == HttpStatus.USE_PROXY_305) {
            getIncorrectnessListener().notify("Ignoring HTTP status code [305] 'Use Proxy'", this);
        }
        else if (status >= HttpStatus.MOVED_PERMANENTLY_301
            && status <= HttpStatus.PERMANENT_REDIRECT_308
            && status != HttpStatus.NOT_MODIFIED_304
            && getOptions().isRedirectEnabled()) {

            final URL newUrl;
            String locationString = null;
            try {
                locationString = webResponse.getResponseHeaderValue("Location");
                if (locationString == null) {
                    return webResponse;
                }
                locationString = new String(locationString.getBytes(ISO_8859_1), UTF_8);
                newUrl = expandUrl(url, locationString);
            }
            catch (final MalformedURLException e) {
                getIncorrectnessListener().notify("Got a redirect status code [" + status + " "
                    + webResponse.getStatusMessage()
                    + "] but the location is not a valid URL [" + locationString
                    + "]. Skipping redirection processing.", this);
                return webResponse;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Got a redirect status code [" + status + "] new location = [" + locationString + "]");
            }

            if (allowedRedirects == 0) {
                throw new FailingHttpStatusCodeException("Too much redirect for "
                    + webResponse.getWebRequest().getUrl(), webResponse);
            }

            if (status == HttpStatus.MOVED_PERMANENTLY_301
                    || status == HttpStatus.FOUND_302
                    || status == HttpStatus.SEE_OTHER_303) {
                final WebRequest wrs = new WebRequest(newUrl, HttpMethod.GET);
                wrs.setCharset(webRequest.getCharset());

                if (HttpMethod.HEAD == webRequest.getHttpMethod()) {
                    wrs.setHttpMethod(HttpMethod.HEAD);
                }
                for (final Map.Entry<String, String> entry : webRequest.getAdditionalHeaders().entrySet()) {
                    wrs.setAdditionalHeader(entry.getKey(), entry.getValue());
                }
                return loadWebResponseFromWebConnection(wrs, allowedRedirects - 1);
            }
            else if (status == HttpStatus.TEMPORARY_REDIRECT_307
                        || status == HttpStatus.PERMANENT_REDIRECT_308) {
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/307
                // https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/308
                // reuse method and body
                final WebRequest wrs = new WebRequest(newUrl, webRequest.getHttpMethod());
                wrs.setCharset(webRequest.getCharset());
                if (webRequest.getRequestBody() != null) {
                    if (HttpMethod.POST == webRequest.getHttpMethod()
                            || HttpMethod.PUT == webRequest.getHttpMethod()
                            || HttpMethod.PATCH == webRequest.getHttpMethod()) {
                        wrs.setRequestBody(webRequest.getRequestBody());
                        wrs.setEncodingType(webRequest.getEncodingType());
                    }
                }
                else {
                    wrs.setRequestParameters(parameters);
                }

                for (final Map.Entry<String, String> entry : webRequest.getAdditionalHeaders().entrySet()) {
                    wrs.setAdditionalHeader(entry.getKey(), entry.getValue());
                }

                return loadWebResponseFromWebConnection(wrs, allowedRedirects - 1);
            }
        }

        if (fromCache == null) {
            getCache().cacheIfPossible(webRequest, webResponse, null);
        }
        return webResponse;
    }

    /**
     * Returns the cached response provided for the request if usable otherwise makes the
     * request and returns the response.
     * @param webRequest the request
     * @param cached a previous cached response for the request, or {@code null}
     */
    private WebResponse getWebResponseOrUseCached(
            final WebRequest webRequest, final WebResponse cached) throws IOException {
        if (cached == null) {
            return getWebConnection().getResponse(webRequest);
        }

        if (!HeaderUtils.containsNoCache(cached)) {
            return new WebResponseFromCache(cached, webRequest);
        }

        // implementation based on rfc9111 https://www.rfc-editor.org/rfc/rfc9111#name-validation
        if (HeaderUtils.containsETag(cached)) {
            webRequest.setAdditionalHeader(HttpHeader.IF_NONE_MATCH, cached.getResponseHeaderValue(HttpHeader.ETAG));
        }
        if (HeaderUtils.containsLastModified(cached)) {
            webRequest.setAdditionalHeader(HttpHeader.IF_MODIFIED_SINCE,
                    cached.getResponseHeaderValue(HttpHeader.LAST_MODIFIED));
        }

        final WebResponse webResponse = getWebConnection().getResponse(webRequest);

        if (webResponse.getStatusCode() >= HttpStatus.INTERNAL_SERVER_ERROR_500) {
            return new WebResponseFromCache(cached, webRequest);
        }

        if (webResponse.getStatusCode() == HttpStatus.NOT_MODIFIED_304) {
            final Map<String, NameValuePair> header2NameValuePair = new LinkedHashMap<>();
            for (final NameValuePair pair : cached.getResponseHeaders()) {
                header2NameValuePair.put(pair.getName(), pair);
            }
            for (final NameValuePair pair : webResponse.getResponseHeaders()) {
                if (preferHeaderFrom304Response(pair.getName())) {
                    header2NameValuePair.put(pair.getName(), pair);
                }
            }
            // WebResponse headers is unmodifiableList so we cannot update it directly
            // instead, create a new WebResponseFromCache with updated headers
            // then use it to replace the old cached value
            final WebResponse updatedCached =
                    new WebResponseFromCache(cached, new ArrayList<>(header2NameValuePair.values()), webRequest);
            getCache().cacheIfPossible(webRequest, updatedCached, null);
            return updatedCached;
        }

        getCache().cacheIfPossible(webRequest, webResponse, null);
        return webResponse;
    }

    /**
     * Returns true if the value of the specified header in a 304 Not Modified response should be
     * adopted over any previously cached value.
     */
    private static boolean preferHeaderFrom304Response(final String name) {
        final String lcName = name.toLowerCase(Locale.ROOT);
        for (final String header : DISCARDING_304_RESPONSE_HEADER_NAMES) {
            if (lcName.equals(header)) {
                return false;
            }
        }
        for (final String prefix : DISCARDING_304_HEADER_PREFIXES) {
            if (lcName.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds the headers that are sent with every request to the specified {@link WebRequest} instance.
     * @param wrs the <code>WebRequestSettings</code> instance to modify
     */
    private void addDefaultHeaders(final WebRequest wrs) {
        // Add user-specified headers to the web request if not present there yet.
        requestHeaders_.forEach((name, value) -> {
            if (!wrs.isAdditionalHeader(name)) {
                wrs.setAdditionalHeader(name, value);
            }
        });

        // Add standard HtmlUnit headers to the web request if still not present there yet.
        if (!wrs.isAdditionalHeader(HttpHeader.ACCEPT_LANGUAGE)) {
            wrs.setAdditionalHeader(HttpHeader.ACCEPT_LANGUAGE, getBrowserVersion().getAcceptLanguageHeader());
        }

        if (!wrs.isAdditionalHeader(HttpHeader.SEC_FETCH_DEST)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_FETCH_DEST, "document");
        }
        if (!wrs.isAdditionalHeader(HttpHeader.SEC_FETCH_MODE)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_FETCH_MODE, "navigate");
        }
        if (!wrs.isAdditionalHeader(HttpHeader.SEC_FETCH_SITE)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_FETCH_SITE, "same-origin");
        }
        if (!wrs.isAdditionalHeader(HttpHeader.SEC_FETCH_USER)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_FETCH_USER, "?1");
        }
        if (getBrowserVersion().hasFeature(HTTP_HEADER_PRIORITY)
                && !wrs.isAdditionalHeader(HttpHeader.PRIORITY)) {
            wrs.setAdditionalHeader(HttpHeader.PRIORITY, "u=0, i");
        }

        if (getBrowserVersion().hasFeature(HTTP_HEADER_CH_UA)
                && !wrs.isAdditionalHeader(HttpHeader.SEC_CH_UA)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_CH_UA, getBrowserVersion().getSecClientHintUserAgentHeader());
        }
        if (getBrowserVersion().hasFeature(HTTP_HEADER_CH_UA)
                && !wrs.isAdditionalHeader(HttpHeader.SEC_CH_UA_MOBILE)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_CH_UA_MOBILE, "?0");
        }
        if (getBrowserVersion().hasFeature(HTTP_HEADER_CH_UA)
                && !wrs.isAdditionalHeader(HttpHeader.SEC_CH_UA_PLATFORM)) {
            wrs.setAdditionalHeader(HttpHeader.SEC_CH_UA_PLATFORM,
                    getBrowserVersion().getSecClientHintUserAgentPlatformHeader());
        }

        if (!wrs.isAdditionalHeader(HttpHeader.UPGRADE_INSECURE_REQUESTS)) {
            wrs.setAdditionalHeader(HttpHeader.UPGRADE_INSECURE_REQUESTS, "1");
        }
    }

    /**
     * Returns an immutable list of open web windows (whether they are top level windows or not).
     * This is a snapshot; future changes are not reflected by this list.
     * <p>
     * The list is ordered by age, the oldest one first.
     *
     * @return an immutable list of open web windows (whether they are top level windows or not)
     * @see #getWebWindowByName(String)
     * @see #getTopLevelWindows()
     */
    public List<WebWindow> getWebWindows() {
        return Collections.unmodifiableList(new ArrayList<>(windows_));
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Returns true if the list of WebWindows contains the provided one.
     * This method is there to improve the performance of some internal checks because
     * calling getWebWindows().contains(.) creates some objects without any need.
     *
     * @param webWindow the window to check
     * @return true or false
     */
    public boolean containsWebWindow(final WebWindow webWindow) {
        return windows_.contains(webWindow);
    }

    /**
     * Returns an immutable list of open top level windows.
     * This is a snapshot; future changes are not reflected by this list.
     * <p>
     * The list is ordered by age, the oldest one first.
     *
     * @return an immutable list of open top level windows
     * @see #getWebWindowByName(String)
     * @see #getWebWindows()
     */
    public List<TopLevelWindow> getTopLevelWindows() {
        return Collections.unmodifiableList(new ArrayList<>(topLevelWindows_));
    }

    /**
     * Sets the handler to be used whenever a refresh is triggered. Refer
     * to the documentation for {@link RefreshHandler} for more details.
     * @param handler the new handler
     */
    public void setRefreshHandler(final RefreshHandler handler) {
        if (handler == null) {
            refreshHandler_ = new NiceRefreshHandler(2);
        }
        else {
            refreshHandler_ = handler;
        }
    }

    /**
     * Returns the current refresh handler.
     * The default refresh handler is a {@link NiceRefreshHandler NiceRefreshHandler(2)}.
     * @return the current RefreshHandler
     */
    public RefreshHandler getRefreshHandler() {
        return refreshHandler_;
    }

    /**
     * Sets the script pre processor for this {@link WebClient}.
     * @param scriptPreProcessor the new preprocessor or null if none is specified
     */
    public void setScriptPreProcessor(final ScriptPreProcessor scriptPreProcessor) {
        scriptPreProcessor_ = scriptPreProcessor;
    }

    /**
     * Returns the script pre processor for this {@link WebClient}.
     * @return the pre processor or null of one hasn't been set
     */
    public ScriptPreProcessor getScriptPreProcessor() {
        return scriptPreProcessor_;
    }

    /**
     * Sets the listener for messages generated by the HTML parser.
     * @param listener the new listener, {@code null} if messages should be totally ignored
     */
    public void setHTMLParserListener(final HTMLParserListener listener) {
        htmlParserListener_ = listener;
    }

    /**
     * Gets the configured listener for messages generated by the HTML parser.
     * @return {@code null} if no listener is defined (default value)
     */
    public HTMLParserListener getHTMLParserListener() {
        return htmlParserListener_;
    }

    /**
     * Returns the CSS error handler used by this web client when CSS problems are encountered.
     * @return the CSS error handler used by this web client when CSS problems are encountered
     * @see DefaultCssErrorHandler
     * @see SilentCssErrorHandler
     */
    public CSSErrorHandler getCssErrorHandler() {
        return cssErrorHandler_;
    }

    /**
     * Sets the CSS error handler used by this web client when CSS problems are encountered.
     * @param cssErrorHandler the CSS error handler used by this web client when CSS problems are encountered
     * @see DefaultCssErrorHandler
     * @see SilentCssErrorHandler
     */
    public void setCssErrorHandler(final CSSErrorHandler cssErrorHandler) {
        WebAssert.notNull("cssErrorHandler", cssErrorHandler);
        cssErrorHandler_ = cssErrorHandler;
    }

    /**
     * Sets the number of milliseconds that a script is allowed to execute before being terminated.
     * A value of 0 or less means no timeout.
     *
     * @param timeout the timeout value, in milliseconds
     */
    public void setJavaScriptTimeout(final long timeout) {
        scriptEngine_.setJavaScriptTimeout(timeout);
    }

    /**
     * Returns the number of milliseconds that a script is allowed to execute before being terminated.
     * A value of 0 or less means no timeout.
     *
     * @return the timeout value, in milliseconds
     */
    public long getJavaScriptTimeout() {
        return scriptEngine_.getJavaScriptTimeout();
    }

    /**
     * Gets the current listener for encountered incorrectness (except HTML parsing messages that
     * are handled by the HTML parser listener). Default value is an instance of
     * {@link IncorrectnessListenerImpl}.
     * @return the current listener (not {@code null})
     */
    public IncorrectnessListener getIncorrectnessListener() {
        return incorrectnessListener_;
    }

    /**
     * Returns the current HTML incorrectness listener.
     * @param listener the new value (not {@code null})
     */
    public void setIncorrectnessListener(final IncorrectnessListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Null is not a valid IncorrectnessListener");
        }
        incorrectnessListener_ = listener;
    }

    /**
     * Returns the WebConsole.
     * @return the web console
     */
    public WebConsole getWebConsole() {
        if (webConsole_ == null) {
            webConsole_ = new WebConsole();
        }
        return webConsole_;
    }

    /**
     * Gets the current AJAX controller.
     * @return the controller
     */
    public AjaxController getAjaxController() {
        return ajaxController_;
    }

    /**
     * Sets the current AJAX controller.
     * @param newValue the controller
     */
    public void setAjaxController(final AjaxController newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("Null is not a valid AjaxController");
        }
        ajaxController_ = newValue;
    }

    /**
     * Sets the attachment handler.
     * @param handler the new attachment handler
     */
    public void setAttachmentHandler(final AttachmentHandler handler) {
        attachmentHandler_ = handler;
    }

    /**
     * Returns the current attachment handler.
     * @return the current attachment handler
     */
    public AttachmentHandler getAttachmentHandler() {
        return attachmentHandler_;
    }

    /**
     * Sets the WebStart handler.
     * @param handler the new WebStart handler
     */
    public void setWebStartHandler(final WebStartHandler handler) {
        webStartHandler_ = handler;
    }

    /**
     * Returns the current WebStart handler.
     * @return the current WebStart handler
     */
    public WebStartHandler getWebStartHandler() {
        return webStartHandler_;
    }

    /**
     * Returns the current clipboard handler.
     * @return the current clipboard handler
     */
    public ClipboardHandler getClipboardHandler() {
        return clipboardHandler_;
    }

    /**
     * Sets the clipboard handler.
     * @param handler the new clipboard handler
     */
    public void setClipboardHandler(final ClipboardHandler handler) {
        clipboardHandler_ = handler;
    }

    /**
     * Returns the current {@link PrintHandler}.
     * @return the current {@link PrintHandler} or null if print
     *         requests are ignored
     */
    public PrintHandler getPrintHandler() {
        return printHandler_;
    }

    /**
     * Sets the {@link PrintHandler} to be used if Windoe.print() is called
     * (<a href="https://html.spec.whatwg.org/multipage/timers-and-user-prompts.html#printing">Printing Spec</a>).
     *
     * @param handler the new {@link PrintHandler} or null if you like to
     *        ignore print requests (default is null)
     */
    public void setPrintHandler(final PrintHandler handler) {
        printHandler_ = handler;
    }

    /**
     * Returns the current FrameContent handler.
     * @return the current FrameContent handler
     */
    public FrameContentHandler getFrameContentHandler() {
        return frameContentHandler_;
    }

    /**
     * Sets the FrameContent handler.
     * @param handler the new FrameContent handler
     */
    public void setFrameContentHandler(final FrameContentHandler handler) {
        frameContentHandler_ = handler;
    }

    /**
     * Sets the onbeforeunload handler for this {@link WebClient}.
     * @param onbeforeunloadHandler the new onbeforeunloadHandler or null if none is specified
     */
    public void setOnbeforeunloadHandler(final OnbeforeunloadHandler onbeforeunloadHandler) {
        onbeforeunloadHandler_ = onbeforeunloadHandler;
    }

    /**
     * Returns the onbeforeunload handler for this {@link WebClient}.
     * @return the onbeforeunload handler or null if one hasn't been set
     */
    public OnbeforeunloadHandler getOnbeforeunloadHandler() {
        return onbeforeunloadHandler_;
    }

    /**
     * Gets the cache currently being used.
     * @return the cache (may not be null)
     */
    public Cache getCache() {
        return cache_;
    }

    /**
     * Sets the cache to use.
     * @param cache the new cache (must not be {@code null})
     */
    public void setCache(final Cache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("cache should not be null!");
        }
        cache_ = cache;
    }

    /**
     * Keeps track of the current window. Inspired by WebTest's logic to track the current response.
     */
    private static final class CurrentWindowTracker implements WebWindowListener, Serializable {
        private final WebClient webClient_;
        private final boolean ensureOneTopLevelWindow_;

        CurrentWindowTracker(final WebClient webClient, final boolean ensureOneTopLevelWindow) {
            webClient_ = webClient;
            ensureOneTopLevelWindow_ = ensureOneTopLevelWindow;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void webWindowClosed(final WebWindowEvent event) {
            final WebWindow window = event.getWebWindow();
            if (window instanceof TopLevelWindow) {
                webClient_.topLevelWindows_.remove(window);
                if (window == webClient_.getCurrentWindow()) {
                    if (!webClient_.topLevelWindows_.isEmpty()) {
                        // The current window is now the previous top-level window.
                        webClient_.setCurrentWindow(
                                webClient_.topLevelWindows_.get(webClient_.topLevelWindows_.size() - 1));
                    }
                }
            }
            else if (window == webClient_.getCurrentWindow()) {
                // The current window is now the last top-level window.
                if (webClient_.topLevelWindows_.isEmpty()) {
                    webClient_.setCurrentWindow(null);
                }
                else {
                    webClient_.setCurrentWindow(
                            webClient_.topLevelWindows_.get(webClient_.topLevelWindows_.size() - 1));
                }
            }
        }

        /**
         * Postprocessing to make sure we have always one top level window open.
         */
        public void afterWebWindowClosedListenersProcessed(final WebWindowEvent event) {
            if (!ensureOneTopLevelWindow_) {
                return;
            }

            if (webClient_.topLevelWindows_.isEmpty()) {
                // Must always have at least window, and there are no top-level windows left; must create one.
                final TopLevelWindow newWindow = new TopLevelWindow("", webClient_);
                webClient_.setCurrentWindow(newWindow);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void webWindowContentChanged(final WebWindowEvent event) {
            final WebWindow window = event.getWebWindow();
            boolean use = false;
            if (window instanceof DialogWindow) {
                use = true;
            }
            else if (window instanceof TopLevelWindow) {
                use = event.getOldPage() == null;
            }
            else if (window instanceof FrameWindow) {
                final FrameWindow fw = (FrameWindow) window;
                final String enclosingPageState = fw.getEnclosingPage().getDocumentElement().getReadyState();
                final URL frameUrl = fw.getEnclosedPage().getUrl();
                if (!DomNode.READY_STATE_COMPLETE.equals(enclosingPageState) || frameUrl == UrlUtils.URL_ABOUT_BLANK) {
                    return;
                }

                // now looks at the visibility of the frame window
                final BaseFrameElement frameElement = fw.getFrameElement();
                if (webClient_.isJavaScriptEnabled() && frameElement.isDisplayed()) {
                    final ComputedCssStyleDeclaration style = fw.getComputedStyle(frameElement, null);
                    use = style.getCalculatedWidth(false, false) != 0
                            && style.getCalculatedHeight(false, false) != 0;
                }
            }
            if (use) {
                webClient_.setCurrentWindow(window);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void webWindowOpened(final WebWindowEvent event) {
            final WebWindow window = event.getWebWindow();
            if (window instanceof TopLevelWindow) {
                final TopLevelWindow tlw = (TopLevelWindow) window;
                webClient_.topLevelWindows_.add(tlw);
            }
            // Page is not loaded yet, don't set it now as current window.
        }
    }

    /**
     * Closes all opened windows, stopping all background JavaScript processing.
     * The WebClient is not really usable after this - you have to create a new one or
     * use WebClient.reset() instead.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // avoid attaching new windows to the js engine
        if (scriptEngine_ != null) {
            scriptEngine_.prepareShutdown();
        }

        // stop the CurrentWindowTracker from making sure there is still one window available
        currentWindowTracker_ = new CurrentWindowTracker(this, false);

        // Hint: a new TopLevelWindow may be opened by some JS script while we are closing the others
        // but the prepareShutdown() call will prevent the new window form getting js support
        List<WebWindow> windows = new ArrayList<>(windows_);
        for (final WebWindow window : windows) {
            if (window instanceof TopLevelWindow) {
                final TopLevelWindow topLevelWindow = (TopLevelWindow) window;

                try {
                    topLevelWindow.close(true);
                }
                catch (final Exception e) {
                    LOG.error("Exception while closing a TopLevelWindow", e);
                }
            }
            else if (window instanceof DialogWindow) {
                final DialogWindow dialogWindow = (DialogWindow) window;

                try {
                    dialogWindow.close();
                }
                catch (final Exception e) {
                    LOG.error("Exception while closing a DialogWindow", e);
                }
            }
        }

        // second round, none of the remaining windows should be registered to
        // the js engine because of prepareShutdown()
        windows = new ArrayList<>(windows_);
        for (final WebWindow window : windows) {
            if (window instanceof TopLevelWindow) {
                final TopLevelWindow topLevelWindow = (TopLevelWindow) window;

                try {
                    topLevelWindow.close(true);
                }
                catch (final Exception e) {
                    LOG.error("Exception while closing a TopLevelWindow", e);
                }
            }
            else if (window instanceof DialogWindow) {
                final DialogWindow dialogWindow = (DialogWindow) window;

                try {
                    dialogWindow.close();
                }
                catch (final Exception e) {
                    LOG.error("Exception while closing a DialogWindow", e);
                }
            }
        }

        // now both lists have to be empty
        if (!topLevelWindows_.isEmpty()) {
            LOG.error("Sill " + topLevelWindows_.size() + " top level windows are open. Please report this error!");
            topLevelWindows_.clear();
        }

        if (!windows_.isEmpty()) {
            LOG.error("Sill " + windows_.size() + " windows are open. Please report this error!");
            windows_.clear();
        }
        currentWindow_ = null;

        ThreadDeath toThrow = null;
        if (scriptEngine_ != null) {
            try {
                scriptEngine_.shutdown();
            }
            catch (final ThreadDeath ex) {
                // make sure the following cleanup is performed to avoid resource leaks
                toThrow = ex;
            }
            catch (final Exception e) {
                LOG.error("Exception while shutdown the scriptEngine", e);
            }
        }
        scriptEngine_ = null;

        if (webConnection_ != null) {
            try {
                webConnection_.close();
            }
            catch (final Exception e) {
                LOG.error("Exception while closing the connection", e);
            }
        }
        webConnection_ = null;

        synchronized (this) {
            if (executor_ != null) {
                try {
                    executor_.shutdownNow();
                }
                catch (final Exception e) {
                    LOG.error("Exception while shutdown the executor service", e);
                }
            }
        }
        executor_ = null;

        cache_.clear();
        if (toThrow != null) {
            throw toThrow;
        }
    }

    /**
     * <p><span style="color:red">Experimental API: May be changed in next release
     * and may not yet work perfectly!</span></p>
     *
     * <p>This shuts down the whole client and restarts with a new empty window.
     * Cookies and other states are preserved.
     */
    public void reset() {
        close();

        // this has to be done after the browser version was set
        webConnection_ = new HttpWebConnection(this);
        if (javaScriptEngineEnabled_) {
            scriptEngine_ = new JavaScriptEngine(this);
        }

        // The window must be constructed AFTER the script engine.
        currentWindowTracker_ = new CurrentWindowTracker(this, true);
        currentWindow_ = new TopLevelWindow("", this);
    }

    /**
     * <p><span style="color:red">Experimental API: May be changed in next release
     * and may not yet work perfectly!</span></p>
     *
     * <p>This method blocks until all background JavaScript tasks have finished executing. Background
     * JavaScript tasks are JavaScript tasks scheduled for execution via <code>window.setTimeout</code>,
     * <code>window.setInterval</code> or asynchronous <code>XMLHttpRequest</code>.</p>
     *
     * <p>If a job is scheduled to begin executing after <code>(now + timeoutMillis)</code>, this method will
     * wait for <code>timeoutMillis</code> milliseconds and then return a value greater than <code>0</code>. This
     * method will never block longer than <code>timeoutMillis</code> milliseconds.</p>
     *
     * <p>Use this method instead of {@link #waitForBackgroundJavaScriptStartingBefore(long)} if you
     * don't know when your background JavaScript is supposed to start executing, but you're fairly sure
     * that you know how long it should take to finish executing.</p>
     *
     * @param timeoutMillis the maximum amount of time to wait (in milliseconds)
     * @return the number of background JavaScript jobs still executing or waiting to be executed when this
     *         method returns; will be <code>0</code> if there are no jobs left to execute
     */
    public int waitForBackgroundJavaScript(final long timeoutMillis) {
        int count = 0;
        final long endTime = System.currentTimeMillis() + timeoutMillis;
        for (Iterator<WeakReference<JavaScriptJobManager>> i = jobManagers_.iterator(); i.hasNext();) {
            final JavaScriptJobManager jobManager;
            final WeakReference<JavaScriptJobManager> reference;
            try {
                reference = i.next();
                jobManager = reference.get();
                if (jobManager == null) {
                    i.remove();
                    continue;
                }
            }
            catch (final ConcurrentModificationException e) {
                i = jobManagers_.iterator();
                count = 0;
                continue;
            }

            final long newTimeout = endTime - System.currentTimeMillis();
            count += jobManager.waitForJobs(newTimeout);
        }
        if (count != getAggregateJobCount()) {
            final long newTimeout = endTime - System.currentTimeMillis();
            return waitForBackgroundJavaScript(newTimeout);
        }
        return count;
    }

    /**
     * <p><span style="color:red">Experimental API: May be changed in next release
     * and may not yet work perfectly!</span></p>
     *
     * <p>This method blocks until all background JavaScript tasks scheduled to start executing before
     * <code>(now + delayMillis)</code> have finished executing. Background JavaScript tasks are JavaScript
     * tasks scheduled for execution via <code>window.setTimeout</code>, <code>window.setInterval</code> or
     * asynchronous <code>XMLHttpRequest</code>.</p>
     *
     * <p>If there is no background JavaScript task currently executing, and there is no background JavaScript
     * task scheduled to start executing within the specified time, this method returns immediately -- even
     * if there are tasks scheduled to be executed after <code>(now + delayMillis)</code>.</p>
     *
     * <p>Note that the total time spent executing a background JavaScript task is never known ahead of
     * time, so this method makes no guarantees as to how long it will block.</p>
     *
     * <p>Use this method instead of {@link #waitForBackgroundJavaScript(long)} if you know roughly when
     * your background JavaScript is supposed to start executing, but you're not necessarily sure how long
     * it will take to execute.</p>
     *
     * @param delayMillis the delay which determines the background tasks to wait for (in milliseconds)
     * @return the number of background JavaScript jobs still executing or waiting to be executed when this
     *         method returns; will be <code>0</code> if there are no jobs left to execute
     */
    public int waitForBackgroundJavaScriptStartingBefore(final long delayMillis) {
        return waitForBackgroundJavaScriptStartingBefore(delayMillis, -1);
    }

    /**
     * <p><span style="color:red">Experimental API: May be changed in next release
     * and may not yet work perfectly!</span></p>
     *
     * <p>This method blocks until all background JavaScript tasks scheduled to start executing before
     * <code>(now + delayMillis)</code> have finished executing. Background JavaScript tasks are JavaScript
     * tasks scheduled for execution via <code>window.setTimeout</code>, <code>window.setInterval</code> or
     * asynchronous <code>XMLHttpRequest</code>.</p>
     *
     * <p>If there is no background JavaScript task currently executing, and there is no background JavaScript
     * task scheduled to start executing within the specified time, this method returns immediately -- even
     * if there are tasks scheduled to be executed after <code>(now + delayMillis)</code>.</p>
     *
     * <p>Note that the total time spent executing a background JavaScript task is never known ahead of
     * time, so this method makes no guarantees as to how long it will block.</p>
     *
     * <p>Use this method instead of {@link #waitForBackgroundJavaScript(long)} if you know roughly when
     * your background JavaScript is supposed to start executing, but you're not necessarily sure how long
     * it will take to execute.</p>
     *
     * @param delayMillis the delay which determines the background tasks to wait for (in milliseconds)
     * @param timeoutMillis the maximum amount of time to wait (in milliseconds); this has to be larger than
     *        the delayMillis parameter, otherwise the timeout is ignored
     * @return the number of background JavaScript jobs still executing or waiting to be executed when this
     *         method returns; will be <code>0</code> if there are no jobs left to execute
     */
    public int waitForBackgroundJavaScriptStartingBefore(final long delayMillis, final long timeoutMillis) {
        int count = 0;
        long now = System.currentTimeMillis();
        final long endTime = now + delayMillis;
        long endTimeout = now + timeoutMillis;
        if (timeoutMillis < 0 || timeoutMillis < delayMillis) {
            endTimeout = -1;
        }

        for (Iterator<WeakReference<JavaScriptJobManager>> i = jobManagers_.iterator(); i.hasNext();) {
            final JavaScriptJobManager jobManager;
            final WeakReference<JavaScriptJobManager> reference;
            try {
                reference = i.next();
                jobManager = reference.get();
                if (jobManager == null) {
                    i.remove();
                    continue;
                }
            }
            catch (final ConcurrentModificationException e) {
                i = jobManagers_.iterator();
                count = 0;
                continue;
            }
            now = System.currentTimeMillis();
            final long newDelay = endTime - now;
            final long newTimeout = (endTimeout == -1) ? -1 : endTimeout - now;
            count += jobManager.waitForJobsStartingBefore(newDelay, newTimeout);
        }
        if (count != getAggregateJobCount()) {
            now = System.currentTimeMillis();
            final long newDelay = endTime - now;
            final long newTimeout = (endTimeout == -1) ? -1 : endTimeout - now;
            return waitForBackgroundJavaScriptStartingBefore(newDelay, newTimeout);
        }
        return count;
    }

    /**
     * Returns the aggregate background JavaScript job count across all windows.
     * @return the aggregate background JavaScript job count across all windows
     */
    private int getAggregateJobCount() {
        int count = 0;
        for (Iterator<WeakReference<JavaScriptJobManager>> i = jobManagers_.iterator(); i.hasNext();) {
            final JavaScriptJobManager jobManager;
            final WeakReference<JavaScriptJobManager> reference;
            try {
                reference = i.next();
                jobManager = reference.get();
                if (jobManager == null) {
                    i.remove();
                    continue;
                }
            }
            catch (final ConcurrentModificationException e) {
                i = jobManagers_.iterator();
                count = 0;
                continue;
            }
            final int jobCount = jobManager.getJobCount();
            count += jobCount;
        }
        return count;
    }

    /**
     * When we deserialize, re-initializie transient fields.
     * @param in the object input stream
     * @throws IOException if an error occurs
     * @throws ClassNotFoundException if an error occurs
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        webConnection_ = new HttpWebConnection(this);
        scriptEngine_ = new JavaScriptEngine(this);
        jobManagers_ = Collections.synchronizedList(new ArrayList<>());
        loadQueue_ = new ArrayList<>();
        css3ParserPool_ = new CSS3ParserPool();
    }

    private static class LoadJob {
        private final WebWindow requestingWindow_;
        private final String target_;
        private final WebResponse response_;
        private final WeakReference<Page> originalPage_;
        private final WebRequest request_;
        private final String forceAttachmentWithFilename_;

        // we can't us the WebRequest from the WebResponse because
        // we need the original request e.g. after a redirect
        LoadJob(final WebRequest request, final WebResponse response,
                final WebWindow requestingWindow, final String target, final String forceAttachmentWithFilename) {
            request_ = request;
            requestingWindow_ = requestingWindow;
            target_ = target;
            response_ = response;
            originalPage_ = new WeakReference<>(requestingWindow.getEnclosedPage());
            forceAttachmentWithFilename_ = forceAttachmentWithFilename;
        }

        public boolean isOutdated() {
            if (target_ != null && !target_.isEmpty()) {
                return false;
            }

            if (requestingWindow_.isClosed()) {
                return true;
            }

            if (requestingWindow_.getEnclosedPage() != originalPage_.get()) {
                return true;
            }

            return false;
        }
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Perform the downloads and stores it for loading later into a window.
     * In the future downloads should be performed in parallel in separated threads.
     * TODO: refactor it before next release.
     * @param requestingWindow the window from which the request comes
     * @param target the name of the target window
     * @param request the request to perform
     * @param checkHash if true check for hashChenage
     * @param forceAttachmentWithFilename if not {@code null} the AttachmentHandler isAttachment() method is not called,
     *        the response has to be handled as attachment in any case
     * @param description information about the origin of the request. Useful for debugging.
     */
    public void download(final WebWindow requestingWindow, final String target,
        final WebRequest request, final boolean checkHash,
        final String forceAttachmentWithFilename, final String description) {

        final WebWindow targetWindow = resolveWindow(requestingWindow, target);
        final URL url = request.getUrl();

        if (targetWindow != null && HttpMethod.POST != request.getHttpMethod()) {
            final Page page = targetWindow.getEnclosedPage();
            if (page != null) {
                if (page.isHtmlPage() && !((HtmlPage) page).isOnbeforeunloadAccepted()) {
                    return;
                }

                if (checkHash) {
                    final URL current = page.getUrl();
                    final boolean justHashJump =
                            HttpMethod.GET == request.getHttpMethod()
                            && UrlUtils.sameFile(url, current)
                            && null != url.getRef();

                    if (justHashJump) {
                        processOnlyHashChange(targetWindow, url);
                        return;
                    }
                }
            }
        }

        synchronized (loadQueue_) {
            // verify if this load job doesn't already exist
            for (final LoadJob otherLoadJob : loadQueue_) {
                if (otherLoadJob.response_ == null) {
                    continue;
                }
                final WebRequest otherRequest = otherLoadJob.request_;
                final URL otherUrl = otherRequest.getUrl();

                if (url.getPath().equals(otherUrl.getPath()) // fail fast
                    && url.toString().equals(otherUrl.toString())
                    && request.getRequestParameters().equals(otherRequest.getRequestParameters())
                    && Objects.equals(request.getRequestBody(), otherRequest.getRequestBody())) {
                    return; // skip it;
                }
            }
        }

        final LoadJob loadJob;
        try {
            WebResponse response;
            try {
                response = loadWebResponse(request);
            }
            catch (final NoHttpResponseException e) {
                LOG.error("NoHttpResponseException while downloading; generating a NoHttpResponse", e);
                response = new WebResponse(RESPONSE_DATA_NO_HTTP_RESPONSE, request, 0);
            }
            loadJob = new LoadJob(request, response, requestingWindow, target, forceAttachmentWithFilename);
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }

        synchronized (loadQueue_) {
            loadQueue_.add(loadJob);
        }
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * Loads downloaded responses into the corresponding windows.
     * TODO: refactor it before next release.
     * @throws IOException in case of exception
     * @throws FailingHttpStatusCodeException in case of exception
     */
    public void loadDownloadedResponses() throws FailingHttpStatusCodeException, IOException {
        final List<LoadJob> queue;

        // synchronize access to the loadQueue_,
        // to be sure no job is ignored
        synchronized (loadQueue_) {
            if (loadQueue_.isEmpty()) {
                return;
            }
            queue = new ArrayList<>(loadQueue_);
            loadQueue_.clear();
        }

        final HashSet<WebWindow> updatedWindows = new HashSet<>();
        for (int i = queue.size() - 1; i >= 0; --i) {
            final LoadJob loadJob = queue.get(i);
            if (loadJob.isOutdated()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("No usage of download: " + loadJob);
                }
                continue;
            }

            final WebWindow window = resolveWindow(loadJob.requestingWindow_, loadJob.target_);
            if (updatedWindows.contains(window)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("No usage of download: " + loadJob);
                }
                continue;
            }

            final WebWindow win = openTargetWindow(loadJob.requestingWindow_, loadJob.target_, TARGET_SELF);
            final Page pageBeforeLoad = win.getEnclosedPage();
            loadWebResponseInto(loadJob.response_, win, loadJob.forceAttachmentWithFilename_);

            // start execution here.
            if (scriptEngine_ != null) {
                scriptEngine_.registerWindowAndMaybeStartEventLoop(win);
            }

            if (pageBeforeLoad != win.getEnclosedPage()) {
                updatedWindows.add(win);
            }

            // check and report problems if needed
            throwFailingHttpStatusCodeExceptionIfNecessary(loadJob.response_);
        }
    }

    private static void processOnlyHashChange(final WebWindow window, final URL urlWithOnlyHashChange) {
        final Page page = window.getEnclosedPage();
        final String oldURL = page.getUrl().toExternalForm();

        // update request url
        final WebRequest req = page.getWebResponse().getWebRequest();
        req.setUrl(urlWithOnlyHashChange);

        // update location.hash
        final Window jsWindow = window.getScriptableObject();
        if (null != jsWindow) {
            final Location location = jsWindow.getLocation();
            location.setHash(oldURL, urlWithOnlyHashChange.getRef());
        }

        // add to history
        window.getHistory().addPage(page);
    }

    /**
     * Returns the options object of this WebClient.
     * @return the options object
     */
    public WebClientOptions getOptions() {
        return options_;
    }

    /**
     * Gets the holder for the different storages.
     * <p><span style="color:red">Experimental API: May be changed in next release!</span></p>
     * @return the holder
     */
    public StorageHolder getStorageHolder() {
        return storageHolder_;
    }

    /**
     * Returns the currently configured cookies applicable to the specified URL, in an unmodifiable set.
     * If disabled, this returns an empty set.
     * @param url the URL on which to filter the returned cookies
     * @return the currently configured cookies applicable to the specified URL, in an unmodifiable set
     */
    public synchronized Set<Cookie> getCookies(final URL url) {
        final CookieManager cookieManager = getCookieManager();

        if (!cookieManager.isCookiesEnabled()) {
            return Collections.emptySet();
        }

        final URL normalizedUrl = HttpClientConverter.replaceForCookieIfNecessary(url);

        final String host = normalizedUrl.getHost();
        // URLs like "about:blank" don't have cookies and we need to catch these
        // cases here before HttpClient complains
        if (host.isEmpty()) {
            return Collections.emptySet();
        }

        // discard expired cookies
        cookieManager.clearExpired(new Date());

        final Set<Cookie> matchingCookies = new LinkedHashSet<>();
        HttpClientConverter.addMatching(cookieManager.getCookies(), normalizedUrl,
                getBrowserVersion(), matchingCookies);
        return Collections.unmodifiableSet(matchingCookies);
    }

    /**
     * Parses the given cookie and adds this to our cookie store.
     * @param cookieString the string to parse
     * @param pageUrl the url of the page that likes to set the cookie
     * @param origin the requester
     */
    public void addCookie(final String cookieString, final URL pageUrl, final Object origin) {
        final CookieManager cookieManager = getCookieManager();
        if (!cookieManager.isCookiesEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipped adding cookie: '" + cookieString
                        + "' because cookies are not enabled for the CookieManager.");
            }
            return;
        }

        try {
            final List<Cookie> cookies = HttpClientConverter.parseCookie(cookieString, pageUrl, getBrowserVersion());

            for (final Cookie cookie : cookies) {
                cookieManager.addCookie(cookie);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Added cookie: '" + cookieString + "'");
                }
            }
        }
        catch (final MalformedCookieException e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Adding cookie '" + cookieString + "' failed.", e);
            }
            getIncorrectnessListener().notify("Adding cookie '" + cookieString
                        + "' failed; reason: '" + e.getMessage() + "'.", origin);
        }
    }

    /**
     * Returns true if the javaScript support is enabled.
     * To disable the javascript support (eg. temporary)
     * you have to use the {@link WebClientOptions#setJavaScriptEnabled(boolean)} setter.
     * @see #isJavaScriptEngineEnabled()
     * @see WebClientOptions#isJavaScriptEnabled()
     * @return true if the javaScript engine and the javaScript support is enabled.
     */
    public boolean isJavaScriptEnabled() {
        return javaScriptEngineEnabled_ && getOptions().isJavaScriptEnabled();
    }

    /**
     * Returns true if the javaScript engine is enabled.
     * To disable the javascript engine you have to use the
     * {@link WebClient#WebClient(BrowserVersion, boolean, String, int)} constructor.
     * @return true if the javaScript engine is enabled.
     */
    public boolean isJavaScriptEngineEnabled() {
        return javaScriptEngineEnabled_;
    }

    /**
     * Parses the given XHtml code string and loads the resulting XHtmlPage into
     * the current window.
     *
     * @param htmlCode the html code as string
     * @return the HtmlPage
     * @throws IOException in case of error
     */
    public HtmlPage loadHtmlCodeIntoCurrentWindow(final String htmlCode) throws IOException {
        final HTMLParser htmlParser = getPageCreator().getHtmlParser();
        final WebWindow webWindow = getCurrentWindow();

        final StringWebResponse webResponse =
                new StringWebResponse(htmlCode, new URL("https://www.htmlunit.org/dummy.html"));
        final HtmlPage page = new HtmlPage(webResponse, webWindow);
        webWindow.setEnclosedPage(page);

        htmlParser.parse(this, webResponse, page, false, false);
        return page;
    }

    /**
     * Parses the given XHtml code string and loads the resulting XHtmlPage into
     * the current window.
     *
     * @param xhtmlCode the xhtml code as string
     * @return the XHtmlPage
     * @throws IOException in case of error
     */
    public XHtmlPage loadXHtmlCodeIntoCurrentWindow(final String xhtmlCode) throws IOException {
        final HTMLParser htmlParser = getPageCreator().getHtmlParser();
        final WebWindow webWindow = getCurrentWindow();

        final StringWebResponse webResponse =
                new StringWebResponse(xhtmlCode, new URL("https://www.htmlunit.org/dummy.html"));
        final XHtmlPage page = new XHtmlPage(webResponse, webWindow);
        webWindow.setEnclosedPage(page);

        htmlParser.parse(this, webResponse, page, true, false);
        return page;
    }

    /**
     * Creates a new {@link WebSocketAdapter}.
     *
     * @param webSocketListener the {@link WebSocketListener}
     * @return a new {@link WebSocketAdapter}
     */
    public WebSocketAdapter buildWebSocketAdapter(final WebSocketListener webSocketListener) {
        return webSocketAdapterFactory_.buildWebSocketAdapter(this, webSocketListener);
    }

    /**
     * Defines a new factory method to create a new WebSocketAdapter.
     *
     * @param factory a {@link WebSocketAdapterFactory}
     */
    public void setWebSocketAdapter(final WebSocketAdapterFactory factory) {
        webSocketAdapterFactory_ = factory;
    }

    /**
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     *
     * @return a CSS3Parser that will return to an internal pool for reuse if closed using the
     *         try-with-resource concept
     */
    public PooledCSS3Parser getCSS3Parser() {
        return this.css3ParserPool_.get();
    }

    /**
     * Our pool of CSS3Parsers. If you need a parser, get it from here and use the AutoCloseable
     * functionality with a try-with-resource block. If you don't want to do that at all, continue
     * to build CSS3Parsers the old fashioned way.
     *
     * Fetching a parser is thread safe. This API is built to minimize synchronization overhead,
     * hence it is possible to miss a returned parser from another thread under heavy pressure,
     * but because that is unlikely, we keep it simple and efficient. Caches are not supposed
     * to give cutting-edge guarantees.
     *
     * This concept avoids a resource leak when someone does not close the fetched
     * parser because the pool does not know anything about the parser unless
     * it returns. We are not running a checkout-checkin concept.
     *
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     */
    static class CSS3ParserPool {
        /*
         * Our pool. We only hold data when it is available. In addition, synchronization against
         * this deque is cheap.
         */
        private final ConcurrentLinkedDeque<PooledCSS3Parser> parsers_ = new ConcurrentLinkedDeque<>();

        /**
         * Fetch a new or recycled CSS3parser. Make sure you use the try-with-resource concept
         * to automatically return it after use because a parser creation is expensive.
         * We won't get a leak, if you don't do so, but that will remove the advantage.
         *
         * @return a parser
         */
        public PooledCSS3Parser get() {
            // see if we have one, LIFO
            final PooledCSS3Parser parser = parsers_.pollLast();

            // if we don't have one, get us one
            return parser != null ? parser.markInUse(this) : new PooledCSS3Parser(this);
        }

        /**
         * Return a parser. Normally you don't have to use that method explicitly.
         * Prefer to user the AutoCloseable interface of the PooledParser by
         * using a try-with-resource statement.
         *
         * @param parser the parser to recycle
         */
        protected void recycle(final PooledCSS3Parser parser) {
            parsers_.addLast(parser);
        }
    }

    /**
     * This is a poolable CSS3Parser which can be reused automatically when closed.
     * A regular CSS3Parser is not thread-safe, hence also our pooled parser
     * is not thread-safe.
     *
     * <span style="color:red">INTERNAL API - SUBJECT TO CHANGE AT ANY TIME - USE AT YOUR OWN RISK.</span><br>
     */
    public static class PooledCSS3Parser extends CSS3Parser implements AutoCloseable {
        /**
         * The pool we want to return us to. Because multiple threads can use this, we
         * have to ensure that we see the action here.
         */
        private CSS3ParserPool pool_;

        /**
         * Create a new poolable parser.
         *
         * @param pool the pool the parser should return to when it is closed
         */
        protected PooledCSS3Parser(final CSS3ParserPool pool) {
            super();
            this.pool_ = pool;
        }

        /**
         * Resets the parser's pool state so it can be safely returned again.
         *
         * @param pool the pool the parser should return to when it is closed
         * @return this parser for fluid programming
         */
        protected PooledCSS3Parser markInUse(final CSS3ParserPool pool) {
            // ensure we detect programming mistakes
            if (this.pool_ == null) {
                this.pool_ = pool;
            }
            else {
                throw new IllegalStateException("This PooledParser was not returned to the pool properly");
            }

            return this;
        }

        /**
         * Implements the AutoClosable interface. The return method ensures that
         * we are notified when we incorrectly close it twice which indicates a
         * programming flow defect.
         *
         * @throws IllegalStateException in case the parser is closed several times
         */
        @Override
        public void close() {
            if (this.pool_ != null) {
                final CSS3ParserPool oldPool = this.pool_;
                // set null first and recycle later to avoid exposing a broken state
                // volatile guarantees visibility
                this.pool_ = null;

                // return
                oldPool.recycle(this);
            }
            else {
                throw new IllegalStateException("This PooledParser was returned already");
            }
        }
    }
}
