package org.enso.interpreter.node.callable;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.enso.interpreter.Language;
import org.enso.interpreter.node.BaseNode;
import org.enso.interpreter.node.callable.dispatch.IndirectInvokeFunctionNode;
import org.enso.interpreter.node.callable.resolver.*;
import org.enso.interpreter.node.callable.resolver.HostMethodCallNode;
import org.enso.interpreter.runtime.Context;
import org.enso.interpreter.runtime.callable.UnresolvedSymbol;
import org.enso.interpreter.runtime.callable.argument.CallArgumentInfo;
import org.enso.interpreter.runtime.callable.atom.Atom;
import org.enso.interpreter.runtime.callable.atom.AtomConstructor;
import org.enso.interpreter.runtime.callable.function.Function;
import org.enso.interpreter.runtime.data.Array;
import org.enso.interpreter.runtime.data.text.Text;
import org.enso.interpreter.runtime.error.DataflowError;
import org.enso.interpreter.runtime.error.PanicSentinel;
import org.enso.interpreter.runtime.error.PanicException;
import org.enso.interpreter.runtime.number.EnsoBigInteger;
import org.enso.interpreter.runtime.state.Stateful;

@GenerateUncached
@ReportPolymorphism
public abstract class IndirectInvokeMethodNode extends Node {

  /** @return a new indirect method invocation node */
  public static IndirectInvokeMethodNode build() {
    return IndirectInvokeMethodNodeGen.create();
  }

  public abstract Stateful execute(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Object _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail);

  @Specialization
  Stateful doAtom(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Atom _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached AtomResolverNode atomResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = atomResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doConstructor(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      AtomConstructor _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached ConstructorResolverNode constructorResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = constructorResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doBigInteger(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      EnsoBigInteger _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached BigIntegerResolverNode bigIntegerResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = bigIntegerResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doLong(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      long _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached LongResolverNode longResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = longResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doDouble(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      double _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached DoubleResolverNode doubleResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = doubleResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doBoolean(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      boolean _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached BooleanResolverNode booleanResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = booleanResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doText(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Text _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached TextResolverNode textResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = textResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doFunction(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Function _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached FunctionResolverNode functionResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode) {
    Function function = functionResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(
        function,
        frame,
        state,
        arguments,
        schema,
        defaultsExecutionMode,
        argumentsExecutionMode,
        isTail);
  }

  @Specialization
  Stateful doDataflowError(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      DataflowError _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached DataflowErrorResolverNode dataflowErrorResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode,
      @Cached ConditionProfile profile) {
    Function function = dataflowErrorResolverNode.execute(symbol, _this);
    if (profile.profile(function == null)) {
      return new Stateful(state, _this);
    } else {
      return invokeFunctionNode.execute(
          function,
          frame,
          state,
          arguments,
          schema,
          defaultsExecutionMode,
          argumentsExecutionMode,
          isTail);
    }
  }

  @Specialization
  Stateful doPanicSentinel(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      PanicSentinel _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail) {
    throw _this;
  }

  @Specialization
  Stateful doArray(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Array _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached ArrayResolverNode arrayResolverNode,
      @Cached IndirectInvokeFunctionNode invokeFunctionNode,
      @Cached ConditionProfile profile) {
    Function function = arrayResolverNode.execute(symbol, _this);
    if (profile.profile(function == null)) {
      return new Stateful(state, _this);
    } else {
      return invokeFunctionNode.execute(
          function,
          frame,
          state,
          arguments,
          schema,
          defaultsExecutionMode,
          argumentsExecutionMode,
          isTail);
    }
  }

  @Specialization(guards = {"context.getEnvironment().isHostObject(_this)"})
  Stateful doHost(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Object _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @Cached HostMethodCallNode hostMethodCallNode,
      @CachedContext(Language.class) Context context) {
    Object[] args = new Object[arguments.length - 1];
    System.arraycopy(arguments, 1, args, 0, arguments.length - 1);
    return new Stateful(state, hostMethodCallNode.execute(symbol, _this, args));
  }

  static boolean notEnso(InteropLibrary langs, Object _this) {
    try {
      return langs.getLanguage(_this) != Language.class;
    } catch (UnsupportedMessageException e) {
      return true;
    }
  }

  @Specialization(guards = {"langs.hasLanguage(_this)", "notEnso(langs,_this)"})
  Stateful doPolyglot(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Object _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail,
      @CachedLibrary(limit="3")InteropLibrary langs) {
    throw new IllegalStateException("not rdy");
  }

  @Fallback
  Stateful doOther(
      MaterializedFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Object _this,
      Object[] arguments,
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode,
      BaseNode.TailStatus isTail) {
    CompilerDirectives.transferToInterpreter();
    Context context = lookupContextReference(Language.class).get();
    throw new PanicException(
        context.getBuiltins().error().makeNoSuchMethodError(_this, symbol), this);
  }

  boolean isHostObject(Context context, Object object) {
    return context.getEnvironment().isHostObject(object);
  }
}
