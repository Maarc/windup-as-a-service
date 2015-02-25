package org.windup.examples.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

​import weblogic.servlet.annotation.WLServlet;
​ 
​@WLServlet (
​    name = "catalog",
​    runAs = "SuperEditor"
​    initParams = { 
​        @WLInitParam (name="catalog", value="spring"),
​        @WLInitParam (name="language", value="English")
​     },
​     mapping = {"/catalog/*"}
​)
​public class SampleWebLogicServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // noop
    }

}
