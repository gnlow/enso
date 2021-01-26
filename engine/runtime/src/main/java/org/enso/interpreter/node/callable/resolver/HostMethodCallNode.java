package org.enso.interpreter.node.callable.resolver;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import org.enso.interpreter.Language;
import org.enso.interpreter.node.BaseNode;
import org.enso.interpreter.runtime.Context;
import org.enso.interpreter.runtime.callable.UnresolvedSymbol;
import org.enso.interpreter.runtime.callable.function.Function;
import org.enso.interpreter.runtime.error.PanicException;

@GenerateUncached
@ReportPolymorphism
public abstract class HostMethodCallNode extends Node {
  public abstract Object execute(UnresolvedSymbol symbol, Object _this, Object[] args);

  static final int LIB_LIMIT = 3;

  @Specialization(guards = {"members.isMemberInvocable(_this, symbol.getName())"})
  Object resolveHostMethod(
      UnresolvedSymbol symbol,
      Object _this,
      Object[] args,
      @CachedLibrary(limit = "LIB_LIMIT") InteropLibrary members,
      @CachedContext(Language.class) Context context) {
    try {
      return members.invokeMember(_this, symbol.getName(), args);
    } catch (UnsupportedMessageException | UnknownIdentifierException e) {
      throw new IllegalStateException(
          "Impossible to reach here. The member is checked to be invocable.");
    } catch (ArityException e) {
      throw new PanicException(
          // Adding 1 to both numbers, to account for `this` being counted as an argument in Enso,
          // but not Java
          context
              .getBuiltins()
              .error()
              .makeArityError(e.getExpectedArity() + 1, e.getActualArity() + 1),
          this);
    } catch (UnsupportedTypeException e) {
      throw new PanicException(
          context.getBuiltins().error().makeUnsupportedArgumentsError(e.getSuppliedValues()), this);
    }
  }

  @Specialization(
      guards = {
        "args.length == 0",
        "!members.isMemberInvocable(_this, symbol.getName())",
        "members.isMemberReadable(_this, symbol.getName())"
      })
  Object resolveHostField(
      UnresolvedSymbol symbol,
      Object _this,
      Object[] args,
      @CachedLibrary(limit = "LIB_LIMIT") InteropLibrary members,
      @CachedContext(Language.class) Context context) {
    try {
      return members.readMember(_this, symbol.getName());
    } catch (UnsupportedMessageException | UnknownIdentifierException e) {
      throw new IllegalStateException(
          "Impossible to reach here. The member is checked to be readable.");
    }
  }

  static final String ARRAY_LENGTH_NAME = "length";
  static final String ARRAY_READ_NAME = "at";
  static final String TO_TEXT_NAME = "to_text";

  @Specialization(
      guards = {"symbol.getName().equals(ARRAY_LENGTH_NAME)", "arrays.hasArrayElements(_this)"})
  Object resolveHostArrayLength(
      UnresolvedSymbol symbol,
      Object _this,
      Object[] args,
      @CachedLibrary(limit = "LIB_LIMIT") InteropLibrary arrays,
      @CachedContext(Language.class) Context ctx) {
    if (args.length != 0) {
      throw new PanicException(ctx.getBuiltins().error().makeArityError(1, 1 + args.length), this);
    }
    try {
      return arrays.getArraySize(_this);
    } catch (UnsupportedMessageException e) {
      throw new IllegalStateException("Impossible to reach here, _this is checked to be an array");
    }
  }

  @Specialization(
      guards = {"symbol.getName().equals(ARRAY_READ_NAME)", "arrays.hasArrayElements(_this)"})
  Object resolveHostArrayRead(
      UnresolvedSymbol symbol,
      Object _this,
      Object[] args,
      @CachedLibrary(limit = "LIB_LIMIT") InteropLibrary arrays,
      @CachedContext(Language.class) Context ctx) {
    if (args.length != 1) {
      throw new PanicException(ctx.getBuiltins().error().makeArityError(2, 1 + args.length), this);
    }
    if (!(args[0] instanceof Long)) {
      throw new PanicException(
          ctx.getBuiltins()
              .error()
              .makeTypeError(ctx.getBuiltins().number().getInteger().newInstance(), args[0]),
          this);
    }
    long idx = (Long) args[0];
    try {
      return arrays.readArrayElement(_this, idx);
    } catch (UnsupportedMessageException e) {
      throw new IllegalStateException("Impossible to reach here, _this is checked to be an array");
    } catch (InvalidArrayIndexException e) {
      throw new PanicException(
          ctx.getBuiltins().error().makeInvalidArrayIndexError(_this, idx), this);
    }
  }

  @Fallback
  Object reportNoMethod(UnresolvedSymbol symbol, Object _this, Object[] args) {
    Context ctx = lookupContextReference(Language.class).get();
    throw new PanicException(ctx.getBuiltins().error().makeNoSuchMethodError(_this, symbol), this);
  }
}
