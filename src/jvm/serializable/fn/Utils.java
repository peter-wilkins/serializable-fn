package serializable.fn;

import clojure.lang.IFn;
import clojure.lang.MultiFn;
import clojure.lang.RT;
import clojure.lang.Var;
import serializable.fn.kryo.ClojureKryoInstantiator;
import com.twitter.chill.KryoPool;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

  final static Logger LOG = LoggerFactory.getLogger(Utils.class);

  static final Var require = RT.var("clojure.core", "require");
  static final Var symbol = RT.var("clojure.core", "symbol");
  static KryoPool _kpool = new ClojureKryoInstantiator().defaultPool();

  public static byte[] serialize(Object obj) throws IOException {
    return _kpool.toBytesWithClass(obj);
  }

  public static Object deserialize(byte[] serialized) throws IOException {
    return _kpool.fromBytes(serialized);
  }

  public static Throwable getRootCause(Throwable e) {
    Throwable rootCause = e;
    Throwable nextCause = rootCause.getCause();

    while (nextCause != null) {
      rootCause = nextCause;
      nextCause = rootCause.getCause();
    }
    return rootCause;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public static synchronized void tryRequire(String ns_name) {
    try {
      require.invoke(symbol.invoke(ns_name));
    } catch (Exception e) {

      //if playing from the repl and defining functions, file won't exist
      Throwable rootCause = getRootCause(e);

      boolean fileNotFound = (rootCause instanceof FileNotFoundException);
      boolean nsFileMissing = e.getMessage().contains(ns_name + ".clj on classpath");

      if (!(fileNotFound && nsFileMissing))
        throw new RuntimeException(e);
    }
  }

  public static synchronized IFn bootSimpleFn(String ns_name, String fn_name) {
    LOG.trace("bootSimpleFn: " + ns_name + "/" + fn_name);
    return (IFn) bootSimpleVar(ns_name, fn_name).deref();
  }

  public static synchronized MultiFn bootSimpleMultifn(String ns_name, String fn_name) {
    LOG.trace("bootSimpleMultiFn: " + ns_name + "/" + fn_name);
    return (MultiFn) bootSimpleVar(ns_name, fn_name).deref();
  }

  public static synchronized Var bootSimpleVar(String ns_name, String fn_name) {
    LOG.trace("bootSimpleVar: " + ns_name + "/" + fn_name);
    tryRequire(ns_name);
    return RT.var(ns_name, fn_name);
  }

  public static synchronized IFn deserializeFn(byte[] fnSpec) {
    return (IFn) bootSimpleFn("serializable.fn", "deserialize").invoke(fnSpec);
  }

  public static synchronized byte[] serializeFn(IFn fn) {
    return (byte[]) bootSimpleFn("serializable.fn", "serialize").invoke(fn);
  }
}
