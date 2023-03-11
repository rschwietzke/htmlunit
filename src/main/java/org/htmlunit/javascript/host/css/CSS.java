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
package org.htmlunit.javascript.host.css;

import static org.htmlunit.BrowserVersionFeatures.JS_CSS_OBJECT;
import static org.htmlunit.javascript.configuration.SupportedBrowser.CHROME;
import static org.htmlunit.javascript.configuration.SupportedBrowser.EDGE;
import static org.htmlunit.javascript.configuration.SupportedBrowser.FF;
import static org.htmlunit.javascript.configuration.SupportedBrowser.FF_ESR;

import org.htmlunit.javascript.HtmlUnitScriptable;
import org.htmlunit.javascript.configuration.JsxClass;
import org.htmlunit.javascript.configuration.JsxStaticFunction;

import net.sourceforge.htmlunit.corejs.javascript.Scriptable;

/**
 * A JavaScript object for {@code CSS}.
 *
 * @author Ahmed Ashour
 * @author Ronald Brill
 */
@JsxClass({CHROME, EDGE, FF, FF_ESR})
public class CSS extends HtmlUnitScriptable {

    /**
     * Default constructor.
     */
    public CSS() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start) {
        if ("prototype".equals(name)) {
            return NOT_FOUND;
        }
        return super.get(name, start);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDefaultValue(final Class<?> hint) {
        if (getBrowserVersion().hasFeature(JS_CSS_OBJECT)
                && (String.class.equals(hint) || hint == null)) {
            return "[object Object]";
        }
        return super.getDefaultValue(hint);
    }

    /**
     * @return a Boolean value indicating if the browser supports a given CSS feature, or not
     */
    @JsxStaticFunction
    public static boolean supports() {
        // for the moment we support everything :-)
        return true;
    }
}