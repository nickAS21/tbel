/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.ast;

import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import java.util.HashMap;

import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.subCompileExpression;

/**
 * @author Christopher Brock
 */
public class DoNode extends BlockNode {
  protected String item;
  protected ExecutableStatement condition;

  public DoNode(char[] expr, int start, int offset, int blockStart, int blockOffset, int fields, ParserContext pCtx) {
    super(pCtx);
    this.expr = expr;
    this.start = start;
    this.offset = offset;
    this.blockStart = blockStart;
    this.blockOffset = blockOffset;

    this.condition = (ExecutableStatement) subCompileExpression(expr, start, offset, pCtx);

    expectType(pCtx, this.condition, Boolean.class, ((fields & COMPILE_IMMEDIATE) != 0));

    if (pCtx != null) {
      pCtx.pushVariableScope();
    }

    this.compiledBlock = (ExecutableStatement) subCompileExpression(expr, blockStart, blockOffset, pCtx);

    if (pCtx != null) {
      pCtx.popVariableScope();
    }
  }

  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    VariableResolverFactory ctxFactory = new MapVariableResolverFactory(new HashMap<String, Object>(0), factory);
    Object v;
    do {
      checkExecution(ctx);
      v = compiledBlock.getValue(ctx, thisValue, ctxFactory);
      if (ctxFactory.tiltFlag()) return v;
      if (ctxFactory.breakFlag()) {
        ctxFactory.setBreakFlag(false);
        break;
      }
    }
    while ((Boolean) condition.getValue(ctx, thisValue, factory));

    return null;
  }

  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    VariableResolverFactory ctxFactory = new MapVariableResolverFactory(new HashMap<String, Object>(0), factory);
    Object v;
    do {
      checkExecution(ctx);
      v = compiledBlock.getValue(ctx, thisValue, ctxFactory);
      if (ctxFactory.tiltFlag()) return v;
      if (ctxFactory.breakFlag()) {
        ctxFactory.setBreakFlag(false);
        break;
      }
    }
    while ((Boolean) condition.getValue(ctx, thisValue, factory));

    return null;
  }

}