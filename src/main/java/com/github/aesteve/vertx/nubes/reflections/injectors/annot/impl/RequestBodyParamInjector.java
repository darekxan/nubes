package com.github.aesteve.vertx.nubes.reflections.injectors.annot.impl;

import java.util.Map;

import com.github.aesteve.vertx.nubes.annotations.params.RequestBody;
import com.github.aesteve.vertx.nubes.handlers.impl.ContentTypeProcessor;
import com.github.aesteve.vertx.nubes.marshallers.PayloadMarshaller;
import com.github.aesteve.vertx.nubes.reflections.injectors.annot.AnnotatedParamInjector;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class RequestBodyParamInjector implements AnnotatedParamInjector<RequestBody> {

	private static final Logger log = LoggerFactory.getLogger(RequestBodyParamInjector.class);

	private Map<String, PayloadMarshaller> marshallers;

	public RequestBodyParamInjector(Map<String, PayloadMarshaller> marshallers) {
		this.marshallers = marshallers;
	}

	@Override
	public Object resolve(RoutingContext context, RequestBody annotation, String paramName, Class<?> resultClass) {
		String body = context.getBodyAsString();
		if (resultClass.equals(String.class)) {
			return body;
		}
		String contentType = ContentTypeProcessor.getContentType(context);
		if (contentType == null) {
			log.error("No suitable Content-Type found, request body can't be read");
			return null;
		}
		PayloadMarshaller marshaller = marshallers.get(contentType);
		if (marshaller == null) {
			log.error("No marshaller found for Content-Type : " + contentType + ", request body can't be read");
			return null;
		}
		try {
			return marshaller.unmarshallPayload(body, resultClass);
		} catch(Exception e) {
			context.fail(400);
			return null;
		}
	}

}
