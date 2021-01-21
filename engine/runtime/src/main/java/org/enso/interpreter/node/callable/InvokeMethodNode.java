package org.enso.interpreter.node.callable;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import java.util.UUID;
import org.enso.interpreter.Language;
import org.enso.interpreter.node.BaseNode;
import org.enso.interpreter.node.callable.dispatch.InvokeFunctionNode;
import org.enso.interpreter.node.callable.resolver.*;
import org.enso.interpreter.node.callable.resolver.HostMethodCallNode;
import org.enso.interpreter.node.callable.thunk.ThunkExecutorNode;
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

public abstract class InvokeMethodNode extends BaseNode {
  private @Child InvokeFunctionNode invokeFunctionNode;
  private final ConditionProfile errorProfile = ConditionProfile.createCountingProfile();
  private final int argumentCount;

  /**
   * Creates a new node for method invocation.
   *
   * @param schema a description of the arguments being applied to the callable
   * @param defaultsExecutionMode the defaulted arguments handling mode for this call
   * @param argumentsExecutionMode the arguments execution mode for this call
   * @return a new invoke method node
   */
  public static InvokeMethodNode build(
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode) {
    return InvokeMethodNodeGen.create(schema, defaultsExecutionMode, argumentsExecutionMode);
  }

  InvokeMethodNode(
      CallArgumentInfo[] schema,
      InvokeCallableNode.DefaultsExecutionMode defaultsExecutionMode,
      InvokeCallableNode.ArgumentsExecutionMode argumentsExecutionMode) {
    this.invokeFunctionNode =
        InvokeFunctionNode.build(schema, defaultsExecutionMode, argumentsExecutionMode);
    this.argumentCount = schema.length;
  }

  @Override
  public void setTailStatus(TailStatus tailStatus) {
    super.setTailStatus(tailStatus);
    this.invokeFunctionNode.setTailStatus(tailStatus);
  }

  public abstract Stateful execute(
      VirtualFrame frame, Object state, UnresolvedSymbol symbol, Object _this, Object[] arguments);

  @Specialization
  Stateful doAtom(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Atom _this,
      Object[] arguments,
      @Cached AtomResolverNode atomResolverNode) {
    Function function = atomResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doConstructor(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      AtomConstructor _this,
      Object[] arguments,
      @Cached ConstructorResolverNode constructorResolverNode) {
    Function function = constructorResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doBigInteger(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      EnsoBigInteger _this,
      Object[] arguments,
      @Cached BigIntegerResolverNode bigIntegerResolverNode) {
    Function function = bigIntegerResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doLong(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      long _this,
      Object[] arguments,
      @Cached LongResolverNode longResolverNode) {
    Function function = longResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doDouble(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      double _this,
      Object[] arguments,
      @Cached DoubleResolverNode doubleResolverNode) {
    Function function = doubleResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doBoolean(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      boolean _this,
      Object[] arguments,
      @Cached BooleanResolverNode booleanResolverNode) {
    Function function = booleanResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doText(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Text _this,
      Object[] arguments,
      @Cached TextResolverNode textResolverNode) {
    Function function = textResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doFunction(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Function _this,
      Object[] arguments,
      @Cached FunctionResolverNode functionResolverNode) {
    Function function = functionResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization
  Stateful doDataflowError(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      DataflowError _this,
      Object[] arguments,
      @Cached DataflowErrorResolverNode dataflowErrorResolverNode) {
    Function function = dataflowErrorResolverNode.execute(symbol, _this);
    if (errorProfile.profile(function == null)) {
      return new Stateful(state, _this);
    } else {
      return invokeFunctionNode.execute(function, frame, state, arguments);
    }
  }

  @Specialization
  Stateful doPanicSentinel(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      PanicSentinel _this,
      Object[] arguments) {
    throw _this;
  }

  @Specialization
  Stateful doArray(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Array _this,
      Object[] arguments,
      @Cached ArrayResolverNode arrayResolverNode) {
    Function function = arrayResolverNode.execute(symbol, _this);
    return invokeFunctionNode.execute(function, frame, state, arguments);
  }

  @Specialization(guards = "isHostObject(context, _this)")
  @ExplodeLoop
  Stateful doHost(
      VirtualFrame frame,
      Object state,
      UnresolvedSymbol symbol,
      Object _this,
      Object[] arguments,
      @Cached HostMethodCallNode hostMethodCallNode,
      @Cached(value = "buildExecutors()") ThunkExecutorNode[] argumentExecutors,
      @Cached AnyResolverNode anyResolverNode,
      @Cached ConditionProfile handledProfile,
      @CachedContext(Language.class) Context context) {
    Object[] args = new Object[arguments.length - 1];
    // TODO Push down to host method call
    for (int i = 0; i < argumentExecutors.length; i++) {
      Stateful r = argumentExecutors[i].executeThunk(arguments[i + 1], state, TailStatus.NOT_TAIL);
      args[i] = r.getValue();
      state = r.getState();
    }
    Object r = hostMethodCallNode.execute(symbol, _this, args);
    if (handledProfile.profile(r == HostMethodCallNode.NO_RESOLUTION)) {
      Function function = anyResolverNode.execute(symbol, _this);
      return invokeFunctionNode.execute(function, frame, state, arguments);
    } else {
      return new Stateful(state, r);
    }
  }

  @Fallback
  Stateful doOther(
      VirtualFrame frame, Object state, UnresolvedSymbol symbol, Object _this, Object[] arguments) {
    CompilerDirectives.transferToInterpreter();
    Context context = lookupContextReference(Language.class).get();
    throw new PanicException(
        context.getBuiltins().error().makeNoSuchMethodError(_this, symbol), this);
  }

  @Override
  public SourceSection getSourceSection() {
    Node parent = getParent();
    return parent == null ? null : parent.getSourceSection();
  }

  ThunkExecutorNode[] buildExecutors() {
    ThunkExecutorNode[] result = new ThunkExecutorNode[argumentCount - 1];
    for (int i = 0; i < argumentCount - 1; i++) {
      result[i] = ThunkExecutorNode.build();
    }
    return result;
  }

  boolean isHostObject(Context context, Object object) {
    return context.getEnvironment().isHostObject(object);
  }

  /**
   * Sets the expression ID of this node.
   *
   * @param id the expression ID to assign this node.
   */
  public void setId(UUID id) {
    invokeFunctionNode.setId(id);
  }
}
