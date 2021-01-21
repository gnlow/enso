package org.enso.interpreter.epb.node;

import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;

public abstract class InnerToOuterNode extends Node {
  public abstract Object execute(Object value, TruffleContext inner);

  @Specialization(guards = {"numbers.isNumber(value)", "numbers.fitsInLong(value)"})
  long doLong(
      Object value, TruffleContext inner, @CachedLibrary(limit = "3") InteropLibrary numbers) {
    try {
      return numbers.asLong(value);
    } catch (UnsupportedMessageException e) {
      throw new IllegalStateException("Impossible");
    }
  }

  @Specialization
  Object doOther(Object value, TruffleContext inner) {
    return value;
  }
}
