package com.power.oj.core.handler;

import com.jfinal.handler.Handler;
import com.power.oj.core.OjConfig;
import com.power.oj.core.OjConstants;
import jodd.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseUrlHandler extends Handler {

    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, boolean[] isHandled) {
        if (StringUtil.isBlank(OjConfig.getBaseURL())) {
            StringBuilder sb = new StringBuilder();
            sb.append(request.getScheme()).append("://").append(request.getServerName());

            if (request.getServerPort() != 80) {
                sb.append(':').append(request.getServerPort());
            }
            sb.append(request.getContextPath());

            OjConfig.setBaseURL(sb.toString());
        }

        request.setAttribute(OjConstants.BASE_URL, request.getContextPath());
        next.handle(target, request, response, isHandled);
    }

}
