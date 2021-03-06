package com.github.aesteve.vertx.nubes.reflections;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import com.github.aesteve.vertx.nubes.Config;
import com.github.aesteve.vertx.nubes.annotations.sockjs.OnClose;
import com.github.aesteve.vertx.nubes.annotations.sockjs.OnMessage;
import com.github.aesteve.vertx.nubes.annotations.sockjs.OnOpen;
import com.github.aesteve.vertx.nubes.annotations.sockjs.SockJS;

public class SocketFactory extends AbstractInjectionFactory implements HandlerFactory {

	private static final Logger log = LoggerFactory.getLogger(SocketFactory.class);

	private Router router;

	public SocketFactory(Router router, Config config) {
		this.router = router;
		this.config = config;
	}

	public void createHandlers() {
		config.controllerPackages.forEach(controllerPackage -> {
			Reflections reflections = new Reflections(controllerPackage);
			Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(SockJS.class);
			controllers.forEach(controller -> {
				createSocketHandlers(controller);
			});
		});
	}

	private void createSocketHandlers(Class<?> controller) {
		SockJSHandler sockJSHandler = SockJSHandler.create(config.vertx, config.sockJSOptions);
		SockJS annot = controller.getAnnotation(SockJS.class);
		String path = annot.value();
		List<Method> openHandlers = new ArrayList<>();
		List<Method> messageHandlers = new ArrayList<>();
		List<Method> closeHandlers = new ArrayList<>();
		Object ctrlInstance = null;
		try {
			ctrlInstance = controller.newInstance();
			injectServicesIntoController(router, ctrlInstance);
		} catch (Exception e) {
			throw new RuntimeException("Could not instanciate socket controller : " + controller.getName(), e);
		}
		final Object instance = ctrlInstance;
		for (Method method : controller.getMethods()) {
			OnOpen openAnnot = method.getAnnotation(OnOpen.class);
			OnClose closeAnnot = method.getAnnotation(OnClose.class);
			OnMessage messageAnnot = method.getAnnotation(OnMessage.class);
			if (openAnnot != null) {
				openHandlers.add(method);
			}
			if (closeAnnot != null) {
				closeHandlers.add(method);
			}
			if (messageAnnot != null) {
				messageHandlers.add(method);
			}
		}
		sockJSHandler.socketHandler(ws -> {
			openHandlers.forEach(handler -> {
				tryToInvoke(instance, handler, ws, null);
			});
			ws.handler(buff -> {
				messageHandlers.forEach(messageHandler -> {
					tryToInvoke(instance, messageHandler, ws, buff);
				});
			});
			ws.endHandler(voidz -> {
				closeHandlers.forEach(closeHandler -> {
					tryToInvoke(instance, closeHandler, ws, null);
				});
			});
		});
		if (!path.endsWith("/*")) {
			if (path.endsWith("/")) {
				path += "*";
			} else {
				path += "/*";
			}
		}
		router.route(path).handler(sockJSHandler);
	}

	private void tryToInvoke(Object instance, Method method, SockJSSocket socket, Buffer msg) {
		List<Object> paramInstances = new ArrayList<>();
		for (Class<?> parameterClass : method.getParameterTypes()) {
			if (parameterClass.equals(SockJSSocket.class)) {
				paramInstances.add(socket);
			} else if (Buffer.class.isAssignableFrom(parameterClass)) {
				paramInstances.add(msg);
			} else if (parameterClass.equals(EventBus.class)) {
				paramInstances.add(config.vertx.eventBus());
			} else if (parameterClass.equals(Vertx.class)) {
				paramInstances.add(config.vertx);
			}
		}
		try {
			method.invoke(instance, paramInstances.toArray());
		} catch (Exception e) {
			log.error("Error while handling websocket", e);
			socket.close();
		}
	}
}
