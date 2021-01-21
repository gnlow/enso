package org.enso.interpreter.epb.runtime;

import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.enso.interpreter.epb.node.OuterToInnerNode;

@ExportLibrary(InteropLibrary.class)
public class PolyglotProxy implements TruffleObject {
  final Object delegate;
  private final TruffleContext context;

  public PolyglotProxy(Object delegate, TruffleContext context) {
    this.delegate = delegate;
    this.context = context;
  }

  @ExportMessage
  public boolean isNull(@CachedLibrary("this.delegate") InteropLibrary nulls) {
    Object p = context.enter();
    try {
      return nulls.isNull(this.delegate);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public boolean hasMembers(@CachedLibrary("this.delegate") InteropLibrary members) {
    Object p = context.enter();
    try {
      return members.hasMembers(this.delegate);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public Object getMembers(
      boolean includeInternal, @CachedLibrary("this.delegate") InteropLibrary members)
      throws UnsupportedMessageException {
    Object p = context.enter();
    try {
      return members.getMembers(this.delegate, includeInternal);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public boolean isMemberInvocable(
      String member, @CachedLibrary("this.delegate") InteropLibrary members) {
    Object p = context.enter();
    try {
      return members.isMemberInvocable(this.delegate, member);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public Object invokeMember(
      String member,
      Object[] arguments,
      @CachedLibrary("this.delegate") InteropLibrary members,
      @Cached OuterToInnerNode outerToInnerNode)
      throws ArityException, UnknownIdentifierException, UnsupportedMessageException,
          UnsupportedTypeException {
    Object p = context.enter();
    try {
      return outerToInnerNode.execute(
          members.invokeMember(this.delegate, member, arguments), context);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public boolean isMemberReadable(
      String member, @CachedLibrary("this.delegate") InteropLibrary members) {
    Object p = context.enter();
    try {
      return members.isMemberReadable(this.delegate, member);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public Object readMember(
      String member,
      @CachedLibrary("this.delegate") InteropLibrary members,
      @Cached OuterToInnerNode outerToInnerNode)
      throws UnknownIdentifierException, UnsupportedMessageException {
    Object p = context.enter();
    try {
      return outerToInnerNode.execute(members.readMember(this.delegate, member), context);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public boolean isExecutable(@CachedLibrary("this.delegate") InteropLibrary functions) {
    Object p = context.enter();
    try {
      return functions.isExecutable(this.delegate);
    } finally {
      context.leave(p);
    }
  }

  @ExportMessage
  public Object execute(
      Object[] arguments,
      @CachedLibrary("this.delegate") InteropLibrary functions,
      @Cached OuterToInnerNode outerToInnerNode)
      throws UnsupportedMessageException, ArityException, UnsupportedTypeException {
    Object p = context.enter();
    try {
      return outerToInnerNode.execute(functions.execute(this.delegate, arguments), context);
    } finally {
      context.leave(p);
    }
  }
}
