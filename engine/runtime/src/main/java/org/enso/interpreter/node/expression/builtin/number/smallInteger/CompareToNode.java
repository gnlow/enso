package org.enso.interpreter.node.expression.builtin.number.smallInteger;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import org.enso.interpreter.Language;
import org.enso.interpreter.dsl.BuiltinMethod;
import org.enso.interpreter.node.expression.builtin.number.utils.BigIntegerOps;
import org.enso.interpreter.runtime.Context;
import org.enso.interpreter.runtime.builtin.Ordering;
import org.enso.interpreter.runtime.callable.atom.Atom;
import org.enso.interpreter.runtime.error.PanicException;
import org.enso.interpreter.runtime.number.EnsoBigInteger;

@BuiltinMethod(
    type = "Small_Integer",
    name = "compare_to",
    description = "Comparison for small integers.")
public abstract class CompareToNode extends Node {

  static CompareToNode build() {
    return CompareToNodeGen.create();
  }

  abstract Atom execute(long _this, Object that);

  @Specialization
  Atom doLong(
      long _this,
      long that,
      @CachedContext(Language.class) ContextReference<Context> ctxRef,
      @Cached("getOrdering(ctxRef)") Ordering ordering) {
    if (_this == that) {
      return ordering.newEqual();
    } else if (_this > that) {
      return ordering.newGreater();
    } else {
      return ordering.newLess();
    }
  }

  @Specialization
  Atom doBigInt(
      long _this,
      EnsoBigInteger that,
      @CachedContext(Language.class) ContextReference<Context> ctxRef,
      @Cached("getOrdering(ctxRef)") Ordering ordering) {
    return ordering.fromJava(BigIntegerOps.compareTo(_this, that.getValue()));
  }

  @Specialization
  Atom doDecimal(
      long _this,
      double that,
      @CachedContext(Language.class) ContextReference<Context> ctxRef,
      @Cached("getOrdering(ctxRef)") Ordering ordering) {
    if (_this == that) {
      return ordering.newEqual();
    } else if (_this > that) {
      return ordering.newGreater();
    } else {
      return ordering.newLess();
    }
  }

  @Specialization
  Atom doOther(
      long _this, Object that, @CachedContext(Language.class) ContextReference<Context> ctxRef) {
    CompilerDirectives.transferToInterpreter();
    var number = ctxRef.get().getBuiltins().number().getNumber().newInstance();
    var typeError = ctxRef.get().getBuiltins().error().makeTypeError(that, number);
    throw new PanicException(typeError, this);
  }

  Ordering getOrdering(ContextReference<Context> ctxRef) {
    return ctxRef.get().getBuiltins().ordering();
  }
}
