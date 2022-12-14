package org.abs_models.frontend.typesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.abs_models.frontend.FrontendTest;
import org.abs_models.frontend.analyser.BitVec;
import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.abs_models.frontend.typechecker.nullable.NullCheckerExtension;
import org.abs_models.frontend.typechecker.nullable.NullableType;
import org.junit.Test;

public class NullableTests extends FrontendTest {
    @Test
    public void testMethodDeclNewExp() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { I i = new C(); i; } }");

        Block b = met.getBlock();
        VarDeclStmt s = (VarDeclStmt) b.getStmt(0);
        VarDecl d = s.getVarDecl();
        ExpressionStmt es = (ExpressionStmt) b.getStmt(1);
        VarOrFieldUse v = (VarOrFieldUse) es.getExp();

        assertEquals(1, b.getStmt(0).nonnull_out().size());
        assertTrue(b.getStmt(0).nonnull_out().contains(d));
        assertTrue(v.nonnull());
        assertEquals(NullableType.Nonnull, v.getNullableType());
    }

    @Test
    public void testMethodDeclCall() {
        MethodImpl met = getMethod("interface I { I m(); } class C implements I { Unit m(I i) { I j = i.m(); } }");
        Block b = met.getBlock();

        assertEquals(1, b.getStmt(0).nonnull_out().size());
        assertTrue(b.getStmt(0).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignAccess() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { I i = new C(); I j; j = i; j; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        VarDecl d1 = ((VarDeclStmt) b.getStmt(1)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(2);
        ExpressionStmt es = (ExpressionStmt) b.getStmt(3);

        assertEquals(2, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
        assertTrue(a.nonnull_out().contains(d1));
        assertEquals(NullableType.Nullable, d0.getNullableType());
        assertEquals(NullableType.Nonnull, es.getExp().getNullableType());
    }

    @Test
    public void testMethodAssignAs() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { I i = new C(); I j; j = i as C; j; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(2);
        ExpressionStmt es = (ExpressionStmt) b.getStmt(3);

        assertEquals(1, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
        assertEquals(NullableType.Nullable, es.getExp().getNullableType());
    }

    @Test
    public void testMethodAssignBinary() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { Int n; n = 1 + 4; n; } }");
        Block b = met.getBlock();

        AssignStmt a = (AssignStmt) b.getStmt(1);
        ExpressionStmt es = (ExpressionStmt) b.getStmt(2);

        assertEquals(0, a.nonnull_out().size());
        assertNull(es.getExp().getNullableType());
    }

    @Test
    public void testMethodAssignCase1() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m([Nonnull] I i1, [Nonnull] I i2, Int n) { I j; j = case n { 0 => i1; 1 => i2; }; j; } }");
        Block b = met.getBlock();

        ParamDecl p0 = met.getMethodSig().getParam(0);
        ParamDecl p1 = met.getMethodSig().getParam(1);
        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(1);
        ExpressionStmt es = (ExpressionStmt) b.getStmt(2);

        assertEquals(3, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(p0));
        assertTrue(a.nonnull_out().contains(p1));
        assertTrue(a.nonnull_out().contains(d0));

        assertEquals(NullableType.Nonnull, p0.getNullableType());
        assertEquals(NullableType.Nonnull, p1.getNullableType());
        assertEquals(NullableType.Nullable, d0.getNullableType());
        assertEquals(NullableType.Nonnull, es.getExp().getNullableType());
    }

    @Test
    public void testMethodAssignDataConstructor() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { List<Int> l; l = Cons(1, Nil); l; } }");
        Block b = met.getBlock();

        VarDeclStmt ds = (VarDeclStmt) b.getStmt(0);
        AssignStmt a = (AssignStmt) b.getStmt(1);
        ExpressionStmt es = (ExpressionStmt) b.getStmt(2);

        assertEquals(0, a.nonnull_out().size());
        assertNull(ds.getVarDecl().getNullableType());
    }

    @Test
    public void testMethodAssignFn1() {
        MethodImpl met = getMethod("def I f(I i) = i; interface I { Unit m(); } class C implements I { Unit m() { I i; i = f(this); } }");
        Block b = met.getBlock();

        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(0, a.nonnull_out().size());
        assertEquals(NullableType.Nullable, ((FunctionDecl) met.getModuleDecl().getDecl(0)).getNullableType());
    }

    @Test
    public void testMethodAssignFn2() {
        MethodImpl met = getMethod("def [Nonnull] I f(I i1, [Nonnull] I i2) = if i1 == null then i2 else i1; interface I { Unit m(); } class C implements I { Unit m() { I i; i = f(this, this); i; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(1);
        ExpressionStmt es = (ExpressionStmt) b.getStmt(2);

        assertEquals(1, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
        assertEquals(NullableType.Nonnull, ((FunctionDecl) met.getModuleDecl().getDecl(0)).getNullableType());
        assertEquals(NullableType.Nonnull, es.getExp().getNullableType());
    }

    @Test
    public void testMethodAssignFn3() {
        MethodImpl met = getMethod("def [Nonnull] I f(I i1, [Nonnull] I i2) = if i1 == null then i2 else i1; interface I { Unit m(); } class C implements I { Unit m() { I i; i = f(this, this); } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(1, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
    }

    @Test
    public void testMethodAssignImplements() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { I i = new C(); Bool b; b = i implements C; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(2);

        assertEquals(1, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
        assertNull(a.getValue().getNullableType());
    }

    @Test
    public void testMethodAssignLet1() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { I i = new C(); I j; j = let I v = i in v; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        VarDecl d1 = ((VarDeclStmt) b.getStmt(1)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(2);

        assertEquals(2, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
        assertTrue(a.nonnull_out().contains(d1));
    }

    @Test
    public void testMethodAssignLet2() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m(I i) { I j; j = let I v = i in v; } }");
        Block b = met.getBlock();

        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(0, a.nonnull_out().size());
    }

    @Test
    public void testMethodAssignLiteral() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { Int n; n = 2; } }");
        Block b = met.getBlock();

        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(0, a.nonnull_out().size());
    }

    @Test
    public void testMethodAssignNull() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m(I i) { I i = new C(); i = null; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(1, a.nonnull_in().size());
        assertTrue(a.nonnull_in().contains(d0));
        assertEquals(0, a.nonnull_out().size());
    }

    @Test
    public void testMethodAssignParFn1() {
        MethodImpl met = getMethod("def I p(f)(I i) = i; interface I { Unit m(); } class C implements I { Unit m() { I i; i = p(toString)(this); } }");
        Block b = met.getBlock();

        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(0, a.nonnull_out().size());
    }

    @Test
    public void testMethodAssignParFn2() {
        MethodImpl met = getMethod("def [Nonnull] I p(f)(I i1, [Nonnull] I i2) = if i1 == null then i2 else i1; interface I { Unit m(); } class C implements I { Unit m() { I i; i = p(toString)(this, this); } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(1, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(d0));
    }

    @Test
    public void testMethodAssignThis() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i = this; } }");

        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();

        BitVec<VarOrFieldDecl> nonnull = b.getStmt(0).nonnull_out();
        assertTrue(nonnull.contains(p));
        assertEquals(1, nonnull.size());
    }

    @Test
    public void testMethodAssignUnary() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m() { Int n; n = -4; } }");
        Block b = met.getBlock();

        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(0, a.nonnull_out().size());
    }

    @Test
    public void assignWhen() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m(I i1, [Nonnull] I i2) { I res; res = when i1 == null then i2 else i1; } }");
        Block b = met.getBlock();

        ParamDecl vd2 = met.getMethodSig().getParam(1);
        VarDecl vd3 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        AssignStmt a = (AssignStmt) b.getStmt(1);

        assertEquals(1, a.nonnull_in().size());
        assertTrue(a.nonnull_in().contains(vd2));
        assertEquals(2, a.nonnull_out().size());
        assertTrue(a.nonnull_out().contains(vd2));
        assertTrue(a.nonnull_out().contains(vd3));
    }

    @Test
    public void testMethodAssignCall1() {
        MethodImpl met = getMethod("interface I { I m(); } class C implements I { Unit m(I i) { I j; j = i.m(); } }");
        Block b = met.getBlock();

        assertEquals(1, b.getStmt(1).nonnull_out().size());
        assertTrue(b.getStmt(1).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignCall2() {
        MethodImpl met = getMethod("interface I { [Nonnull] I m(); } class C implements I { Unit m(I i) { I j; j = i.m(); } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();

        assertEquals(2, b.getStmt(1).nonnull_out().size());
        assertTrue(b.getStmt(1).nonnull_out().contains(d0));
        assertTrue(b.getStmt(1).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignAsyncCall1() {
        MethodImpl met = getMethod("interface I { I m(); } class C implements I { Unit m(I i) { Fut<I> f; f = i!m(); } }");
        Block b = met.getBlock();
        assertEquals(1, b.getStmt(1).nonnull_out().size());
        assertTrue(b.getStmt(1).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignAsyncCall2() {
        MethodImpl met = getMethod("interface I { [Nonnull] I m(); } class C implements I { Unit m(I i) { Fut<I> f; f = i!m(); } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();

        assertEquals(2, b.getStmt(1).nonnull_out().size());
        assertTrue(b.getStmt(1).nonnull_out().contains(d0));
        assertTrue(b.getStmt(1).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignGet1() {
        MethodImpl met = getMethod("interface I { I m(); } class C implements I { Unit m(I i) { Fut<I> f; f = i!m(); I j; j = f.get; } }");
        Block b = met.getBlock();

        assertEquals(1, b.getStmt(3).nonnull_out().size());
        assertTrue(b.getStmt(3).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignGet2() {
        MethodImpl met = getMethod("interface I { [Nonnull] I m(); } class C implements I { Unit m(I i) { Fut<I> f; f = i!m(); I j; j = f.get; } }");
        Block b = met.getBlock();

        VarDecl d0 = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        VarDecl d1 = ((VarDeclStmt) b.getStmt(2)).getVarDecl();

        assertEquals(3, b.getStmt(3).nonnull_out().size());
        assertTrue(b.getStmt(3).nonnull_out().contains(d0));
        assertTrue(b.getStmt(3).nonnull_out().contains(d1));
        assertTrue(b.getStmt(3).nonnull_out().contains(met.getMethodSig().getParam(0)));
    }

    @Test
    public void testMethodAssignNewExp() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i = new C(); } }");

        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();

        BitVec<VarOrFieldDecl> nonnull = b.getStmt(0).nonnull_out();
        assertTrue(nonnull.contains(p));
        assertEquals(1, nonnull.size());
        assertEquals(NullableType.Nullable, p.getNullableType());
    }

    @Test
    public void testMethodAssignOriginal() {
        // TODO
    }

    @Test
    public void testMethodSecondCall() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i.m(this); } }");

        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();

        BitVec<VarOrFieldDecl> nonnull1 = b.getStmt(0).nonnull_in();
        BitVec<VarOrFieldDecl> nonnull2 = b.getStmt(0).nonnull_out();
        assertTrue(nonnull1.isEmpty());
        assertTrue(nonnull2.contains(p));
    }

    @Test
    public void testMethodAssert() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { assert i != null; skip; } }");

        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();
        Stmt skip = b.getStmt(1);

        BitVec<VarOrFieldDecl> nonnull1 = b.getStmt(0).nonnull_in();
        BitVec<VarOrFieldDecl> nonnull2 = b.getStmt(0).nonnull_out();
        assertTrue(nonnull1.isEmpty());
        assertTrue(nonnull2.isEmpty());
        assertTrue(skip.nonnull_in().contains(p));
    }

    @Test
    public void testMethodAssertCatch1() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { try { assert i == null; } catch { AssertionFailException => skip; } } }");
        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();
        TryCatchFinallyStmt t = (TryCatchFinallyStmt) b.getStmt(0);
        CaseBranchStmt cs = t.getCatch(0);
        Stmt skip = cs.getRight().getStmt(0);

        assertEquals(1, skip.nonnull_out().size());
        assertTrue(skip.nonnull_out().contains(p));
    }

    @Test
    public void testMethodAssertCatch2() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { try { assert i != null; } catch { AssertionFailException => skip; } } }");
        Block b = met.getBlock();
        TryCatchFinallyStmt t = (TryCatchFinallyStmt) b.getStmt(0);
        CaseBranchStmt cs = t.getCatch(0);
        Stmt skip = cs.getRight().getStmt(0);

        assertEquals(0, skip.nonnull_out().size());
    }

    @Test
    public void testMethodIfCondition() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { if (i != null) skip; else skip; } }");

        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();

        IfStmt ifStmt = (IfStmt) b.getStmt(0);

        BitVec<VarOrFieldDecl> nonnull = ifStmt.getThen().nonnull_in();
        assertTrue(nonnull.contains(p));
        assertTrue(ifStmt.getElse().null_in().contains(p));
    }

    @Test
    public void testMethodIfInvertedCondition() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { if (i == null) skip; else skip; } }");

        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();

        IfStmt ifStmt = (IfStmt) b.getStmt(0);

        BitVec<VarOrFieldDecl> nonnull = ifStmt.getElse().nonnull_in();
        assertTrue(nonnull.contains(p));
        assertTrue(ifStmt.getThen().null_in().contains(p));
    }

    @Test
    public void testMethodAnnotatedParam() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m([Nonnull] I i) { skip; } }");
        ParamDecl p = met.getMethodSig().getParam(0);
        Block b = met.getBlock();

        assertTrue(met.entry().nonnull_out().contains(p));
        assertEquals(1, met.entry().nonnull_out().size());
        assertTrue(b.getStmt(0).nonnull_in().contains(p));
    }

    @Test
    public void testIfNoElse() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { I j = i; if (j == null) { j = new C(); } skip; } }");
        Block b = met.getBlock();

        VarDecl d = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        Stmt s = b.getStmt(2);

        assertEquals(1, s.nonnull_in().size());
        assertTrue(s.nonnull_out().contains(d));
    }

    @Test
    public void testIfNoElseNull() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { I j = i; if (j != null) { j = null; } skip; } }");
        Block b = met.getBlock();

        VarDecl d = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        Stmt s = b.getStmt(2);

        assertEquals(1, s.null_out().size());
        assertTrue(s.null_out().contains(d));
    }

    @Test
    public void testAfterIf() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { I j; if (i == null) { j = new C(); } else { j = i; } skip; } }");
        Block b = met.getBlock();

        VarDecl d = ((VarDeclStmt) b.getStmt(0)).getVarDecl();
        Stmt s = b.getStmt(2);

        assertEquals(1, s.nonnull_in().size());
        assertTrue(s.nonnull_out().contains(d));
    }

    @Test
    public void testCatch() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m([Nonnull] I i1, [Nonnull] I i2, Int n) { I j; try { j = case n { 0 => i1; 1 => i2; }; } catch { PatternMatchFailException => skip; } } }");
        Block b = met.getBlock();

        ParamDecl p0 = met.getMethodSig().getParam(0);
        ParamDecl p1 = met.getMethodSig().getParam(1);
        TryCatchFinallyStmt t = (TryCatchFinallyStmt) b.getStmt(1);
        CaseBranchStmt cs = t.getCatch(0);
        Stmt skip = cs.getRight().getStmt(0);

        assertEquals(2, skip.nonnull_out().size());
        assertTrue(skip.nonnull_out().contains(p0));
        assertTrue(skip.nonnull_out().contains(p1));
    }

    @Test
    public void testWhile1() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m([Nonnull] I i, Int n) { while (n > 0) { n = n - 1; } skip;  } }");
        Block b = met.getBlock();

        ParamDecl p = met.getMethodSig().getParam(0);
        WhileStmt w = (WhileStmt) b.getStmt(0);
        Block body = w.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.nonnull_out().size());
        assertTrue(s.nonnull_out().contains(p));

        assertEquals(1, skip.nonnull_out().size());
        assertTrue(skip.nonnull_out().contains(p));
    }

    @Test
    public void testWhile2() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m(I i, Int n) { while (n > 0) { i = new C(); } skip;  } }");
        Block b = met.getBlock();

        ParamDecl p = met.getMethodSig().getParam(0);
        WhileStmt w = (WhileStmt) b.getStmt(0);
        Block body = w.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.nonnull_out().size());
        assertTrue(s.nonnull_out().contains(p));

        assertEquals(0, skip.nonnull_out().size());
    }

    @Test
    public void testWhile3() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m(I i, Int n) { while (i == null) { skip; } skip;  } }");
        Block b = met.getBlock();

        ParamDecl p = met.getMethodSig().getParam(0);
        WhileStmt w = (WhileStmt) b.getStmt(0);
        Block body = w.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(0, s.nonnull_out().size());
        assertEquals(1, s.null_out().size());
        assertTrue(s.null_out().contains(p));

        assertEquals(1, skip.nonnull_out().size());
        assertTrue(skip.nonnull_out().contains(p));
        assertEquals(0, skip.null_out().size());
    }

    @Test
    public void testWhile4() {
        MethodImpl met = getMethod("interface I { Unit m(); } class C implements I { Unit m(I i, Int n) { while (i != null) { skip; } skip;  } }");
        Block b = met.getBlock();

        ParamDecl p = met.getMethodSig().getParam(0);
        WhileStmt w = (WhileStmt) b.getStmt(0);
        Block body = w.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.nonnull_out().size());
        assertTrue(s.nonnull_out().contains(p));

        assertEquals(0, skip.nonnull_out().size());
        assertEquals(1, skip.null_out().size());
        assertTrue(skip.null_out().contains(p));
    }

    @Test
    public void testClassParamInitB() {
        ClassDecl c = getClass("interface I {} class C(I i1, [Nonnull] I i2, Int n) implements I { { skip; } }");

        InitBlock i = c.getInitBlock();
        Stmt skip = i.getStmt(0);
        ParamDecl p0 = c.getParam(0);
        ParamDecl p1 = c.getParam(1);
        ParamDecl p2 = c.getParam(2);

        assertEquals(NullableType.Nullable, p0.getNullableType());
        assertEquals(NullableType.Nonnull, p1.getNullableType());
        assertNull(p2.getNullableType());
        assertEquals(1, skip.nonnull_in().size());
        assertTrue(skip.nonnull_in().contains(p1));
    }

    @Test
    public void testClassParamMethod() {
        MethodImpl met = getMethod("interface I {} class C(I i1, [Nonnull] I i2, Int n) implements I { Unit m() { skip; } }");

        ClassDecl c = (ClassDecl) met.getParent().getParent();

        ParamDecl p0 = c.getParam(0);
        ParamDecl p1 = c.getParam(1);
        ParamDecl p2 = c.getParam(2);
        Stmt skip = met.getBlock().getStmt(0);

        assertEquals(NullableType.Nullable, p0.getNullableType());
        assertEquals(NullableType.Nonnull, p1.getNullableType());
        assertNull(p2.getNullableType());
        assertEquals(1, skip.nonnull_in().size());
        assertTrue(skip.nonnull_in().contains(p1));
    }

    @Test
    public void testClassFieldInitB() {
        ClassDecl c = getClass("interface I {} class C implements I { I i1; [Nonnull] I i2; Int n = 2; { skip; } }");

        InitBlock i = c.getInitBlock();
        Stmt skip = i.getStmt(0);
        FieldDecl f0 = c.getField(0);
        FieldDecl f1 = c.getField(1);
        FieldDecl f2 = c.getField(2);

        assertEquals(NullableType.Nullable, f0.getNullableType());
        assertEquals(NullableType.Nonnull, f1.getNullableType());
        assertNull(f2.getNullableType());
        assertEquals(0, skip.nonnull_in().size());
        assertEquals(2, skip.null_in().size());
        assertTrue(skip.null_in().contains(f0));
        assertTrue(skip.null_in().contains(f1));
    }

    @Test
    public void testClassFieldMethod() {
        MethodImpl met = getMethod("interface I {} class C implements I { I i1; [Nonnull] I i2; Int n = 2; Unit m() { skip; } }");

        ClassDecl c = (ClassDecl) met.getParent().getParent();

        FieldDecl f0 = c.getField(0);
        FieldDecl f1 = c.getField(1);
        FieldDecl f2 = c.getField(2);
        Stmt skip = met.getBlock().getStmt(0);

        assertEquals(NullableType.Nullable, f0.getNullableType());
        assertEquals(NullableType.Nonnull, f1.getNullableType());
        assertNull(f2.getNullableType());
        assertEquals(1, skip.nonnull_in().size());
        assertTrue(skip.nonnull_in().contains(f1));
    }

    @Test
    public void varDeclNoInit() {
        Model m = getModel("interface I {} class C implements I { } { I i; i; }");

        MainBlock mb = m.getMainBlock();
        VarDeclStmt s0 = (VarDeclStmt) mb.getStmt(0);
        ExpressionStmt s1 = (ExpressionStmt) mb.getStmt(1);

        assertEquals(0, s0.null_in().size());
        assertEquals(1, s0.null_out().size());
        assertTrue(s0.null_out().contains(s0.getVarDecl()));

        assertEquals(1, s1.null_in().size());
        assertTrue(s1.null_in().contains(s0.getVarDecl()));
        assertEquals(1, s1.null_out().size());
        assertTrue(s1.null_out().contains(s0.getVarDecl()));

        assertEquals(NullableType.Null, s1.getExp().getNullableType());
    }

    @Test
    public void varDeclInitNull() {
        Model m = getModel("interface I {} class C implements I { } { I i = null; i; }");

        MainBlock mb = m.getMainBlock();
        VarDeclStmt s0 = (VarDeclStmt) mb.getStmt(0);
        ExpressionStmt s1 = (ExpressionStmt) mb.getStmt(1);

        assertEquals(0, s0.null_in().size());
        assertEquals(1, s0.null_out().size());
        assertTrue(s0.null_out().contains(s0.getVarDecl()));

        assertEquals(1, s1.null_in().size());
        assertTrue(s1.null_in().contains(s0.getVarDecl()));
        assertEquals(1, s1.null_out().size());
        assertTrue(s1.null_out().contains(s0.getVarDecl()));

        assertEquals(NullableType.Null, s1.getExp().getNullableType());
    }

    @Test
    public void adt() {
        Model m = getModel("interface I {} class C implements I { } { I i = new C(); List<[Nonnull] I> l = list[i, i]; I j = head(l); }");

        MainBlock mb = m.getMainBlock();

        VarDeclStmt vds0 = (VarDeclStmt) mb.getStmt(0);
        VarDecl vd0 = vds0.getVarDecl();

        VarDeclStmt vds1 = (VarDeclStmt) mb.getStmt(1);
        VarDecl vd1 = vds1.getVarDecl();

        VarDeclStmt vds2 = (VarDeclStmt) mb.getStmt(2);
        VarDecl vd2 = vds2.getVarDecl();

        System.out.println(vd2.getInitExp());

        assertEquals(2, vds2.nonnull_out().size());
        assertTrue(vds2.nonnull_out().contains(vd0));
        assertTrue(vds2.nonnull_out().contains(vd2));
    }

    static private Model getModel(String prog) {
        Model m = assertParse(prog);
        m.registerTypeSystemExtension(new NullCheckerExtension(m));
        m.typeCheck();
        return m;
    }

    static private ModuleDecl getModule(String prog) {
        return getTestModule(getModel(prog));
    }

    static private ClassDecl getClass(String prog) {
        return (ClassDecl) getModule(prog).lookup(new KindedName(KindedName.Kind.CLASS, "UnitTest.C"));
    }

    static private MethodImpl getMethod(String prog) {
        return getClass(prog).getMethod(0);
    }

    static private ModuleDecl getTestModule(Model m) {
        ModuleDecl md = m.lookupModule("UnitTest");
        assertNotNull("Module UnitTest not found", md);
        return md;
    }
}
