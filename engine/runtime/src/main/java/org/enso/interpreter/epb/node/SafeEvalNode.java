package org.enso.interpreter.epb.node;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import org.enso.interpreter.epb.EpbContext;
import org.enso.interpreter.epb.EpbLanguage;
import org.enso.interpreter.runtime.callable.function.Function;
import org.enso.interpreter.runtime.state.Stateful;

import java.util.Arrays;
import java.util.List;

public abstract class SafeEvalNode extends RootNode {
  private final String source;
  private @Child DirectCallNode callNode;
  private @Children OuterToInnerNode[] argConverters;
  private @Child InnerToOuterNode innerToOuterNode = InnerToOuterNodeGen.create();
  private final String[] argNames;
  private final String lang;

  public SafeEvalNode(EpbLanguage language, String lang, String source, List<String> arguments) {
    super(language, new FrameDescriptor());
    this.source = source;
    argNames = arguments.toArray(new String[0]);
    if (argNames.length > 0 && argNames[0].equals("this")) {
      argNames[0] = "here";
    }
    argConverters = new OuterToInnerNode[argNames.length];
    for (int i = 0; i < argNames.length; i++) {
      argConverters[i] = OuterToInnerNodeGen.create();
    }
    this.lang = lang.equals("r") ? "R" : lang;
  }

  @Specialization
  Stateful doExecute(
      VirtualFrame frame,
      @CachedContext(EpbLanguage.class) TruffleLanguage.ContextReference<EpbContext> contextRef) {
    EpbContext context = contextRef.get();
    //    System.out.println("Original context is: " + context);
    if (!context.isInner()) {
      TruffleContext outer = context.getEnv().getContext();
      TruffleContext inner = context.getInnerContext();
      Object p = inner.enter();
      try {
        //        System.out.println("Entered inner context");
        return doRun(frame, contextRef, inner, outer);
      } finally {
        inner.leave(p);
      }
    } else {
      TruffleContext inner = context.getEnv().getContext();
      TruffleContext outer = inner.getParent();
      return doRun(frame, contextRef, inner, outer);
    }
  }

  private Stateful doRun(
      VirtualFrame frame,
      TruffleLanguage.ContextReference<EpbContext> contextRef,
      TruffleContext inner,
      TruffleContext outer) {
    Object state = Function.ArgumentsHelper.getState(frame.getArguments());
    if (callNode == null) {
      //      System.out.println("Uncached: Parsing Foreign Code: " + lang);
      CompilerDirectives.transferToInterpreterAndInvalidate();
      Source source = Source.newBuilder(lang, this.source, "kupa").build();
      EpbContext ctx = contextRef.get();
      //      System.out.println("Inner context is: " + ctx);
      //      System.out.println("Arguments are: " + Arrays.toString(argNames));
      CallTarget ct = ctx.getEnv().parseInternal(source, argNames);
      callNode = Truffle.getRuntime().createDirectCallNode(ct);
    }
    Object[] args =
        prepareArgs(
            Function.ArgumentsHelper.getPositionalArguments(frame.getArguments()), inner, outer);
    Object r = callNode.call(args);
    Object wrapped = innerToOuterNode.execute(r, contextRef.get().getEnv().getContext());
    //    if (lang.equals("R")) {
    //
    //    }
    return new Stateful(state, wrapped);
  }

  @ExplodeLoop
  private Object[] prepareArgs(Object[] args, TruffleContext inner, TruffleContext outer) {
    Object[] newArgs = new Object[argConverters.length];
    for (int i = 0; i < argConverters.length; i++) {
      newArgs[i] = argConverters[i].execute(args[i], inner, outer);
    }
    return newArgs;
  }
}
