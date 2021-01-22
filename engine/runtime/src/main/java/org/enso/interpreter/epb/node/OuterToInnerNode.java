package org.enso.interpreter.epb.node;

import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import org.enso.interpreter.epb.runtime.PolyglotProxy;

@GenerateUncached
public abstract class OuterToInnerNode extends Node {
  public abstract Object execute(Object object, TruffleContext inner, TruffleContext outer);

  @Specialization
  long doLong(long l, TruffleContext inner, TruffleContext outer) {
    return l;
  }

  @Specialization
  Object doObject(Object o, TruffleContext inner, TruffleContext outer) {
    return new PolyglotProxy(o, outer, inner, outer);
  }
}
