package com.hellokoding.account.service;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.activiti.engine.IdentityService;
import org.activiti.engine.impl.identity.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		//set current login user to activiti
		String userName = SecurityContextHolder.getContext().getAuthentication().getName();
		Authentication.setAuthenticatedUserId(userName);
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {		
	}

}
