package io.vertx.mvc.reflections.injectors.annot.impl;

import io.vertx.ext.apex.Cookie;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.mvc.annotations.cookies.CookieValue;
import io.vertx.mvc.exceptions.BadRequestException;
import io.vertx.mvc.reflections.injectors.annot.AnnotatedParamInjector;

public class CookieParamInjector implements AnnotatedParamInjector<CookieValue>{

	@Override
	public Object resolve(RoutingContext context, CookieValue annotation, Class<?> resultClass) throws BadRequestException {
		Cookie cookie = context.getCookie(annotation.value());
		if (resultClass.equals(Cookie.class) && cookie != null) {
			return cookie;
		} else if (resultClass.equals(String.class) && cookie != null) {
			return cookie.getValue();
		} else if (cookie == null) {
			throw new BadRequestException("Cookie : "+annotation.value()+ " must be set");
		}
		return null;
	}

}