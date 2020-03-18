/*
 * Copyright (c) 2016-2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SnoopServlet.java

import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class SnoopServlet extends HttpServlet
{

    public SnoopServlet()
    {
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<HTML><HEAD><TITLE>Snoop Servlet</TITLE></HEAD><BODY BGCOLOR=\"#FFFFEE\">");
        out.println("<h1>Snoop Servlet - Request/Client Information...Hahahahahah</h1>");
        out.println("<h1>My SNOOPYYYY Test </h1>");
        out.println("<h2>Requested URL:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
        out.println("<tr><td>" + escapeChar(HttpUtils.getRequestURL(req).toString()) + "</td></tr></table><BR><BR>");
        out.println("<h2>Servlet Name:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
        out.println("<tr><td>" + escapeChar(getServletConfig().getServletName()) + "</td></tr></table><BR><BR>");
        Enumeration vEnum = getServletConfig().getInitParameterNames();
        if(vEnum != null && vEnum.hasMoreElements())
        {
            boolean first = true;
            String param;
            for(; vEnum.hasMoreElements(); out.println("<tr><td>" + escapeChar(param) + "</td><td>" + escapeChar(getInitParameter(param)) + "</td></tr>"))
            {
                if(first)
                {
                    out.println("<h2>Servlet Initialization Parameters</h2>");
                    out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
                    first = false;
                }
                param = (String)vEnum.nextElement();
            }

            out.println("</table><BR><BR>");
        }
        vEnum = getServletConfig().getServletContext().getInitParameterNames();
        if(vEnum != null && vEnum.hasMoreElements())
        {
            boolean first = true;
            String param;
            for(; vEnum.hasMoreElements(); out.println("<tr><td>" + escapeChar(param) + "</td><td>" + escapeChar(getServletConfig().getServletContext().getInitParameter(param)) + "</td></tr>"))
            {
                if(first)
                {
                    out.println("<h2>Servlet Context Initialization Parameters</h2>");
                    out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
                    first = false;
                }
                param = (String)vEnum.nextElement();
            }

            out.println("</table><BR><BR>");
        }
        out.println("<h2>Request Information:</h2>");
        out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
        print(out, "Request method", req.getMethod());
        print(out, "Request URI", req.getRequestURI());
        print(out, "Request protocol", req.getProtocol());
        print(out, "Servlet path", req.getServletPath());
        print(out, "Path info", req.getPathInfo());
        print(out, "Path translated", req.getPathTranslated());
        print(out, "Character encoding", req.getCharacterEncoding());
        print(out, "Query string", req.getQueryString());
        print(out, "Content length", req.getContentLength());
        print(out, "Content type", req.getContentType());
        print(out, "Server name", req.getServerName());
        print(out, "Server port", req.getServerPort());
        print(out, "Remote user", req.getRemoteUser());
        print(out, "Remote address", req.getRemoteAddr());
        print(out, "Remote host", req.getRemoteHost());
        print(out, "Remote port", req.getRemotePort());
        print(out, "Local address", req.getLocalAddr());
        print(out, "Local host", req.getLocalName());
        print(out, "Local port", req.getLocalPort());
        print(out, "Authorization scheme", req.getAuthType());
        if(req.getLocale() != null)
            print(out, "Preferred Client Locale", req.getLocale().toString());
        else
            print(out, "Preferred Client Locale", "none");
        for(Enumeration ee = req.getLocales(); ee.hasMoreElements();)
        {
            Locale cLocale = (Locale)ee.nextElement();
            if(cLocale != null)
                print(out, "All Client Locales", cLocale.toString());
            else
                print(out, "All Client Locales", "none");
        }

        print(out, "Context Path", escapeChar(req.getContextPath()));
        if(req.getUserPrincipal() != null)
            print(out, "User Principal", escapeChar(req.getUserPrincipal().getName()));
        else
            print(out, "User Principal", "none");
        out.println("</table><BR><BR>");
        Enumeration e = req.getHeaderNames();
        if(e.hasMoreElements())
        {
            out.println("<h2>Request headers:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            String name;
            for(; e.hasMoreElements(); out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(req.getHeader(name)) + "</td></tr>"))
                name = (String)e.nextElement();

            out.println("</table><BR><BR>");
        }
        e = req.getParameterNames();
        if(e.hasMoreElements())
        {
            out.println("<h2>Servlet parameters (Single Value style):</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            String name;
            for(; e.hasMoreElements(); out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(req.getParameter(name)) + "</td></tr>"))
                name = (String)e.nextElement();

            out.println("</table><BR><BR>");
        }
        e = req.getParameterNames();
        if(e.hasMoreElements())
        {
            out.println("<h2>Servlet parameters (Multiple Value style):</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            do
            {
                if(!e.hasMoreElements())
                    break;
                String name = (String)e.nextElement();
                String vals[] = (String[])req.getParameterValues(name);
                if(vals != null)
                {
                    out.print("<tr><td>" + escapeChar(name) + "</td><td>");
                    out.print(escapeChar(vals[0]));
                    for(int i = 1; i < vals.length; i++)
                        out.print(", " + escapeChar(vals[i]));

                    out.println("</td></tr>");
                }
            } while(true);
            out.println("</table><BR><BR>");
        }
        String cipherSuite = (String)req.getAttribute("javax.net.ssl.cipher_suite");
        if(cipherSuite != null)
        {
            X509Certificate certChain[] = (X509Certificate[])(X509Certificate[])req.getAttribute("javax.net.ssl.peer_certificates");
            out.println("<h2>HTTPS Information:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            out.println("<tr><td>Cipher Suite</td><td>" + escapeChar(cipherSuite) + "</td></tr>");
            if(certChain != null)
            {
                for(int i = 0; i < certChain.length; i++)
                    out.println("client cert chain [" + i + "] = " + escapeChar(certChain[i].toString()));

            }
            out.println("</table><BR><BR>");
        }
        Cookie cookies[] = req.getCookies();
        if(cookies != null && cookies.length > 0)
        {
            out.println("<H2>Client cookies</H2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            for(int i = 0; i < cookies.length; i++)
                out.println("<tr><td>" + escapeChar(cookies[i].getName()) + "</td><td>" + escapeChar(cookies[i].getValue()) + "</td></tr>");

            out.println("</table><BR><BR>");
        }
        e = req.getAttributeNames();
        if(e.hasMoreElements())
        {
            out.println("<h2>Request attributes:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            String name;
            for(; e.hasMoreElements(); out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(req.getAttribute(name).toString()) + "</td></tr>"))
                name = (String)e.nextElement();

            out.println("</table><BR><BR>");
        }
        e = getServletContext().getAttributeNames();
        if(e.hasMoreElements())
        {
            out.println("<h2>ServletContext attributes:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            String name;
            for(; e.hasMoreElements(); out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(getServletContext().getAttribute(name).toString()) + "</td></tr>"))
                name = (String)e.nextElement();

            out.println("</table><BR><BR>");
        }
        HttpSession session = req.getSession(false);
        if(session != null)
        {
            out.println("<h2>Session information:</h2>");
            out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
            print(out, "Session ID", session.getId());
            print(out, "Last accessed time", (new Date(session.getLastAccessedTime())).toString());
            print(out, "Creation time", (new Date(session.getCreationTime())).toString());
            String mechanism = "unknown";
            if(req.isRequestedSessionIdFromCookie())
                mechanism = "cookie";
            else
            if(req.isRequestedSessionIdFromURL())
                mechanism = "url-encoding";
            print(out, "Session-tracking mechanism", mechanism);
            out.println("</table><BR><BR>");
            Enumeration vals = session.getAttributeNames();
            if(vals.hasMoreElements())
            {
                out.println("<h2>Session values</h2>");
                out.println("<TABLE Border=\"2\" WIDTH=\"65%\" BGCOLOR=\"#DDDDFF\">");
                String name;
                for(; vals.hasMoreElements(); out.println("<tr><td>" + escapeChar(name) + "</td><td>" + escapeChar(session.getAttribute(name).toString()) + "</td></tr>"))
                    name = (String)vals.nextElement();

                out.println("</table><BR><BR>");
            }
        }
        out.println("</body></html>");
    }

    private void print(PrintWriter out, String name, String value)
    {
        out.println("<tr><td>" + name + "</td><td>" + (value != null ? escapeChar(value) : "&lt;none&gt;") + "</td></tr>");
    }

    private void print(PrintWriter out, String name, int value)
    {
        out.print("<tr><td>" + name + "</td><td>");
        if(value == -1)
            out.print("&lt;none&gt;");
        else
            out.print(value);
        out.println("</td></tr>");
    }

    private String escapeChar(String str)
    {
        char src[] = str.toCharArray();
        int len = src.length;
        for(int i = 0; i < src.length; i++)
            switch(src[i])
            {
            case 60: // '<'
                len += 3;
                break;

            case 62: // '>'
                len += 3;
                break;

            case 38: // '&'
                len += 4;
                break;
            }

        char ret[] = new char[len];
        int j = 0;
        for(int i = 0; i < src.length; i++)
            switch(src[i])
            {
            case 60: // '<'
                ret[j++] = '&';
                ret[j++] = 'l';
                ret[j++] = 't';
                ret[j++] = ';';
                break;

            case 62: // '>'
                ret[j++] = '&';
                ret[j++] = 'g';
                ret[j++] = 't';
                ret[j++] = ';';
                break;

            case 38: // '&'
                ret[j++] = '&';
                ret[j++] = 'a';
                ret[j++] = 'm';
                ret[j++] = 'p';
                ret[j++] = ';';
                break;

            default:
                ret[j++] = src[i];
                break;
            }

        return new String(ret);
    }
}
