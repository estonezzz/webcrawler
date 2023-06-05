package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;

  private final Object delegate;
  private final Class<?> klass;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Object delegate, Class<?> klass, ProfilingState state, Clock clock) {
    this.delegate = Objects.requireNonNull(delegate);
    this.klass = Objects.requireNonNull(klass);
    this.state = Objects.requireNonNull(state);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
    // If the method belongs to the Object class, simply invoke it on the delegate and return the result.
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(delegate, args);
    }

    // Retrieve the corresponding method from the delegate's class.
    Method delegateMethod = klass.getMethod(method.getName(), method.getParameterTypes());

    // Check if the method has the @Profiled annotation.
    boolean isProfiled = delegateMethod.isAnnotationPresent(Profiled.class);
    Instant startTime = null;

    // If the method is profiled, record the start time.
    if (isProfiled) {
      startTime = clock.instant();
    }

    try {
      // Invoke the method on the delegate and store the result.
      Object result = method.invoke(delegate, args);
      return result;
    } catch (InvocationTargetException e) {
      // If an InvocationTargetException occurs, extract the cause and rethrow it.
      Throwable cause = e.getTargetException();
      throw (cause != null) ? cause : e;
    } catch (IllegalAccessException e) {
      // If an IllegalAccessException occurs, wrap it in a RuntimeException and rethrow.
      throw new RuntimeException("Failed to access method: " + method.getName(), e);
    }  finally {
      // If the method is profiled, record the duration and update the ProfilingState.
      if (isProfiled) {
        Duration duration = Duration.between(startTime, clock.instant());
        state.record(delegate.getClass(), method, duration);
      }
    }
  }
}
