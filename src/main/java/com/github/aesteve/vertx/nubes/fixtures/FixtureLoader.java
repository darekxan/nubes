package com.github.aesteve.vertx.nubes.fixtures;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import com.github.aesteve.vertx.nubes.Config;
import com.github.aesteve.vertx.nubes.services.ServiceRegistry;
import com.github.aesteve.vertx.nubes.utils.async.AsyncUtils;

public class FixtureLoader {

	public Vertx vertx;
	public Config config;
	public ServiceRegistry serviceRegistry;
	private Set<Fixture> fixtures;

	public Comparator<? extends Fixture> fixtureComparator = (f1, f2) -> {
		return Integer.compare(f1.executionOrder(), f2.executionOrder());
	};

	public FixtureLoader(Vertx vertx, Config config, ServiceRegistry serviceRegistry) {
		this.vertx = vertx;
		this.config = config;
		this.serviceRegistry = serviceRegistry;
		fixtures = new HashSet<>();
	}

	public void setUp(Future<Void> future) {
		instanciateFixtures(future);
		if (future.failed()) {
			return;
		}
		exec(future, "startUp");
	}

	public void tearDown(Future<Void> future) {
		exec(future, "tearDown");
	}

	private void exec(Future<Void> rootFuture, String methodName) {
		if (fixtures.isEmpty()) {
			rootFuture.complete();
			return;
		}
		List<Handler<Future<Void>>> list = new ArrayList<>();
		fixtures.stream().sorted().forEach(fixture -> {
			switch (methodName) {
				case "startUp":
					list.add(fut -> {
						fixture.startUp(vertx, fut);
					});
					break;
				case "tearDown":
					list.add(fut -> {
						fixture.tearDown(vertx, fut);
					});
					break;
				default:
					throw new IllegalArgumentException("Unknown method : " + methodName);
			}
		});
		AsyncUtils.chainHandlers(rootFuture, list);
	}

	private void injectServicesIntoFixture(Fixture fixture) throws IllegalAccessException {
		for (Field field : fixture.getClass().getDeclaredFields()) {
			Object service = serviceRegistry.get(field);
			if (service != null) {
				field.setAccessible(true);
				field.set(fixture, service);
			}
		}
	}

	private void instanciateFixtures(Future<Void> future) {
		if (config.fixturePackages == null || config.fixturePackages.isEmpty()) {
			return;
		}
		for (String fixturePackage : config.fixturePackages) {
			Reflections reflections = new Reflections(fixturePackage);
			Set<Class<? extends Fixture>> fixtureClasses = reflections.getSubTypesOf(Fixture.class);
			for (Class<? extends Fixture> fixtureClass : fixtureClasses) {
				try {
					Fixture fixture = fixtureClass.newInstance();
					injectServicesIntoFixture(fixture);
					fixtures.add(fixture);
				} catch (Exception e) {
					future.fail(e);
					return;
				}
			}
		}

	}

}
