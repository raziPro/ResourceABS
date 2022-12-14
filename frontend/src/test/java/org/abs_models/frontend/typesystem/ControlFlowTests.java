package org.abs_models.frontend.typesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.abs_models.frontend.FrontendTest;
import org.abs_models.frontend.ast.*;
import org.abs_models.frontend.typechecker.KindedName;
import org.junit.Test;

public class ControlFlowTests extends FrontendTest {
    @Test
    public void simple() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i = new C(); skip; suspend; } }");

        Block b = met.getBlock();

        CFGEntry entry = met.entry();
        CFGExit exit = met.exit();
        Stmt s0 = b.getStmt(0);
        Stmt s1 = b.getStmt(1);
        Stmt s2 = b.getStmt(2);

        assertEquals(1, b.pred().size());
        assertTrue(b.pred().contains(entry));
        assertEquals(1, b.succ().size());
        assertTrue(b.succ().contains(s0));

        assertEquals(1, s0.pred().size());
        assertTrue(s0.pred().contains(b));
        assertEquals(1, s0.succ().size());
        assertTrue(s0.succ().contains(s1));

        assertEquals(1, s1.pred().size());
        assertTrue(s1.pred().contains(s0));
        assertEquals(1, s1.succ().size());
        assertTrue(s1.succ().contains(s2));

        assertEquals(1, s2.pred().size());
        assertTrue(s2.pred().contains(s1));
        assertEquals(1, s2.succ().size());
        assertTrue(s2.succ().contains(exit));
    }

    @Test
    public void testAssert() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { assert i != null; skip; } }");
        Block b = met.getBlock();

        Stmt assertStmt = b.getStmt(0);
        Stmt skip = b.getStmt(1);
        Stmt exit = met.exit();

        assertEquals(2, assertStmt.succ().size());
        assertTrue(assertStmt.succ().contains(skip));
        assertTrue(assertStmt.succ().contains(exit));
    }

    @Test
    public void testAssign() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { Int n = 1 / 0; n = 0 / 1; skip; } }");
        Block b = met.getBlock();

        Stmt assignStmt1 = b.getStmt(0);
        Stmt assignStmt2 = b.getStmt(1);
        Stmt skip = b.getStmt(2);
        Stmt exit = met.exit();

        assertEquals(2, assignStmt1.succ().size());
        assertTrue(assignStmt1.succ().contains(assignStmt2));
        assertTrue(assignStmt1.succ().contains(exit));
        assertEquals(1, assignStmt2.succ().size());
        assertTrue(assignStmt2.succ().contains(skip));
    }

    @Test
    public void testCase1() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { Pair<Int, Int> p = Pair(2, 3); Int x = 0; case p { Pair(2, y) => { x = y; skip; } Pair(3, y) => skip; _ => x = -1; } skip; } }");
        Block b = met.getBlock();

        CaseStmt c = (CaseStmt) b.getStmt(2);
        CaseBranchStmt cb0 = c.getBranch(0);
        Block cbr0 = cb0.getRight();
        CaseBranchStmt cb1 = c.getBranch(1);
        Block cbr1 = cb1.getRight();
        CaseBranchStmt cb2 = c.getBranch(2);
        Block cbr2 = cb2.getRight();
        Stmt skip = b.getStmt(3);

        assertEquals(1, c.succ().size());
        assertTrue(c.succ().contains(cb0));

        assertEquals(2, cb0.succ().size());
        assertTrue(cb0.succ().contains(cbr0));
        assertTrue(cb0.succ().contains(cb1));
        assertEquals(1, cbr0.succ().size());
        assertTrue(cbr0.succ().contains(cbr0.getStmt(0)));
        assertEquals(1, cbr0.getStmt(0).succ().size());
        assertTrue(cbr0.getStmt(0).succ().contains(cbr0.getStmt(1)));
        assertEquals(1, cbr0.getStmt(1).succ().size());
        assertTrue(cbr0.getStmt(1).succ().contains(skip));

        assertEquals(2, cb1.succ().size());
        assertTrue(cb1.succ().contains(cbr1));
        assertTrue(cb1.succ().contains(cb2));
        assertEquals(1, cbr1.succ().size());
        assertTrue(cbr1.succ().contains(cbr1.getStmt(0)));
        assertEquals(1, cbr1.getStmt(0).succ().size());
        assertTrue(cbr1.getStmt(0).succ().contains(skip));

        assertEquals(2, cb2.succ().size());
        assertTrue(cb2.succ().contains(cbr2));
        assertTrue(cb2.succ().contains(skip));
        assertEquals(1, cbr2.succ().size());
        assertTrue(cbr2.succ().contains(cbr2.getStmt(0)));
        assertEquals(1, cbr2.getStmt(0).succ().size());
        assertTrue(cbr1.getStmt(0).succ().contains(skip));
    }

    @Test
    public void testCase2() {
        MethodImpl met = getMethod("interface I { Unit m1(I i); } class C implements I { Unit m1(I i) { Pair<Int, Int> p = Pair(2, 3); Int x = 0; case p { Pair(2, y) => { x = y; skip; } Pair(3, y) => skip; } skip; } }");
        Block b = met.getBlock();

        CaseStmt c = (CaseStmt) b.getStmt(2);
        CaseBranchStmt cb0 = c.getBranch(0);
        Block cbr0 = cb0.getRight();
        CaseBranchStmt cb1 = c.getBranch(1);
        Block cbr1 = cb1.getRight();
        Stmt skip = b.getStmt(3);

        assertEquals(1, c.succ().size());
        assertTrue(c.succ().contains(cb0));

        assertEquals(2, cb0.succ().size());
        assertTrue(cb0.succ().contains(cbr0));
        assertTrue(cb0.succ().contains(cb1));
        assertEquals(1, cbr0.succ().size());
        assertTrue(cbr0.succ().contains(cbr0.getStmt(0)));
        assertEquals(1, cbr0.getStmt(0).succ().size());
        assertTrue(cbr0.getStmt(0).succ().contains(cbr0.getStmt(1)));
        assertEquals(1, cbr0.getStmt(1).succ().size());
        assertTrue(cbr0.getStmt(1).succ().contains(skip));

        assertEquals(2, cb1.succ().size());
        assertTrue(cb1.succ().contains(cbr1));
        assertTrue(cb1.succ().contains(c.getBranch(2)));
        assertEquals(1, cbr1.succ().size());
        assertTrue(cbr1.succ().contains(cbr1.getStmt(0)));
        assertEquals(1, cbr1.getStmt(0).succ().size());
        assertTrue(cbr1.getStmt(0).succ().contains(skip));
    }

    @Test
    public void testDuration() {
        MethodImpl met = getMethod("interface I { Unit m(Int i); } class C implements I { Unit m(Int i) { duration(1, 5); duration(case i { 0 => 10; 1 => 20; }, 500); skip; } }");
        Block b = met.getBlock();

        Stmt d0 = b.getStmt(0);
        Stmt d1 = b.getStmt(1);
        Stmt skip = b.getStmt(2);

        assertEquals(1, d0.succ().size());
        assertTrue(d0.succ().contains(d1));
        assertEquals(2, d1.succ().size());
        assertTrue(d1.succ().contains(skip));
        assertTrue(d1.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_As1() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i as I; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_As2() {
        MethodImpl met = getMethod("interface I { Unit m(I i1, I i2, Int n); } class C implements I { Unit m(I i1, I i2, Int n) { case n { 0 => i1; 1 => i2; } as I; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Binary1() {
        MethodImpl met = getMethod("interface I { Unit m(I i1, I i2, Int n); } class C implements I { Unit m(I i1, I i2, Int n) { n + 1; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_Binary2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { case n1 { 0 => n1; 1 => n2; } * 2; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Binary3() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { 2 * case n1 { 0 => n1; 1 => n2; }; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_BinaryDiv1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { n1 / 500; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_BinaryDiv2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { n1 / 0; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_BinaryDiv3() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { n1 / n2; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Case1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { case n1 { 0 => n1; _ => n2 }; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_Case2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { case n1 { 0 => n1; 1 => n2 }; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_DataConstructor1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { Cons(1, Nil); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_DataConstructor2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { Cons(case n1 { 0 => n1; 1 => n2 }, Nil); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_FnApp1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { length(Cons(1, Nil)); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_FnApp2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { tail(case n1 { 0 => Cons(n1, Nil); 1 => Cons(n2, Nil); }); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_If1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { if n1 == 0 then n2 else n1; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_If2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { if n1 / n2 == 0 then n2 else n1; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_If3() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { if n1 == 0 then n2 / n2 else n1; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_If4() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { if n1 == 0 then n2 else n1 / n2; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Implements1() {
        MethodImpl met = getMethod("interface I { Unit m(I i1, I i2); } class C implements I { Unit m(I i1, I i2) { i1 implements I; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_Implements2() {
        MethodImpl met = getMethod("interface I { Unit m(I i1, I i2); } class C implements I { Unit m(I i1, I i2) { case 4 { 0 => i1; 1 => i2; } implements I; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Let1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { let Int x = 2 * n1 in n2 + x; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_Let2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { let Int x = case n1 { 0 => n2; 1 => n1;} in n2 + x; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Let3() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { let Int x = 2 * n1 in n2 / x; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_ParFnApp1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { map(toString)(list[n1, n2]); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_ParFnApp2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { map(toString)(list[n1, n2 / n1]); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_ParFnApp3() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { map((Int i) => i / n1)(list[n1, n2]); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Unary1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { -(n1 * n2); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_Unary2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n1, Int n2); } class C implements I { Unit m(Int n1, Int n2) { -(n1 / n2); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Call1() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i.m(this); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Call2() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { i!m(this); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_Get() {
        MethodImpl met = getMethod("interface I { Unit m(Fut<Int> f); } class C implements I { Unit m(Fut<Int> f) { f.get; skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_New1() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C(Int n, I i) implements I { Unit m(I i) { new C(1, i); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testExpressionStmt_New2() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C(Int n, I i) implements I { Unit m(I i) { new C(1 / 0, i); skip; } }");
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testExpressionStmt_OriginalCall() {
        Model m = assertParse("trait T = { Bool m() { original(); skip; }}");
        ModuleDecl md = m.lookupModule("UnitTest");

        TraitDecl td = (TraitDecl) md.getDecl(0);
        TraitSetExpr te = (TraitSetExpr) td.getTraitExpr();
        MethodImpl met = te.getMethodImpl(0);
        Block b = met.getBlock();

        ExpressionStmt s = (ExpressionStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testForEach() {
        MethodImpl met = getMethod("interface I { Unit m(Int n); } class C implements I { Unit m(Int n) { foreach (i in list[1, n, 3]) { i - 1; } skip; } }");
        Block b = met.getBlock();

        ForeachStmt fs = (ForeachStmt) b.getStmt(0);
        Block body = fs.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, fs.succ().size());
        assertTrue(fs.succ().contains(body));
        assertTrue(fs.succ().contains(skip));
        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(fs));
    }

    @Test
    public void testIf() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { if (i == null) skip; else suspend; } }");
        Block b = met.getBlock();

        IfStmt ifStmt = (IfStmt) b.getStmt(0);
        Block then = ifStmt.getThen();
        Block elseStmt = ifStmt.getElse();

        assertEquals(1, ifStmt.pred().size());
        assertEquals(2, ifStmt.succ().size());
        assertTrue(ifStmt.succ().contains(then));
        assertTrue(ifStmt.succ().contains(elseStmt));

        assertEquals(1, then.pred().size());
        assertTrue(then.pred().contains(ifStmt));
        assertEquals(1, then.succ().size());
        assertTrue(then.getStmt(0).succ().contains(met.exit()));

        assertEquals(1, elseStmt.pred().size());
        assertTrue(elseStmt.pred().contains(ifStmt));
        assertEquals(1, elseStmt.succ().size());
        assertTrue(elseStmt.getStmt(0).succ().contains(met.exit()));
    }

    @Test
    public void testMoveCogTo() {
        // TODO
    }

    @Test
    public void testThrow() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { throw AssertionFailException; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testTry() {
        MethodImpl met = getMethod("interface I { Unit m(Int n); } class C implements I { Unit m(Int n) { try { Rat z = 1/x; } catch { DivisionByZeroException => -n; NullPointerException => n + 1; } skip; } }");
        Block b = met.getBlock();

        TryCatchFinallyStmt t = (TryCatchFinallyStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);
        Block tBlock = t.getBody();
        Stmt s = tBlock.getStmt(0);
        CaseBranchStmt c0 = t.getCatch(0);
        Block cr0 = c0.getRight();
        CaseBranchStmt c1 = t.getCatch(1);
        Block cr1 = c1.getRight();

        assertEquals(1, t.succ().size());
        assertTrue(t.succ().contains(tBlock));
        assertEquals(1, tBlock.succ().size());
        assertTrue(tBlock.succ().contains(s));
        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(c0));

        assertEquals(2, c0.succ().size());
        assertTrue(c0.succ().contains(c1));
        assertTrue(c0.succ().contains(cr0));
        assertEquals(1, cr0.succ().size());
        assertTrue(cr0.succ().contains(cr0.getStmt(0)));
        assertEquals(1, cr0.getStmt(0).succ().size());
        assertTrue(cr0.getStmt(0).succ().contains(skip));

        assertEquals(2, c1.succ().size());
        assertTrue(c1.succ().contains(met.exit()));
        assertTrue(c1.succ().contains(cr1));
        assertEquals(1, cr1.succ().size());
        assertTrue(cr1.succ().contains(cr1.getStmt(0)));
        assertEquals(1, cr1.getStmt(0).succ().size());
        assertTrue(cr1.getStmt(0).succ().contains(skip));
    }

    @Test
    public void testTryFinally() {
        MethodImpl met = getMethod("interface I { Unit m(Int n); } class C implements I { Unit m(Int n) { try { Rat z = 1/x; } catch { DivisionByZeroException => -n; NullPointerException => n + 1; } finally { 1 + n; } skip; } }");
        Block b = met.getBlock();

        TryCatchFinallyStmt t = (TryCatchFinallyStmt) b.getStmt(0);
        Stmt skip = b.getStmt(1);
        Block tBlock = t.getBody();
        Stmt s = tBlock.getStmt(0);
        CaseBranchStmt c0 = t.getCatch(0);
        Block cr0 = c0.getRight();
        CaseBranchStmt c1 = t.getCatch(1);
        Block cr1 = c1.getRight();
        Block f = t.getFinally();

        assertEquals(1, t.succ().size());
        assertTrue(t.succ().contains(tBlock));
        assertEquals(1, tBlock.succ().size());
        assertTrue(tBlock.succ().contains(s));
        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(f));
        assertTrue(s.succ().contains(c0));

        assertEquals(2, c0.succ().size());
        assertTrue(c0.succ().contains(c1));
        assertTrue(c0.succ().contains(cr0));
        assertEquals(1, cr0.succ().size());
        assertTrue(cr0.succ().contains(cr0.getStmt(0)));
        assertEquals(1, cr0.getStmt(0).succ().size());
        assertTrue(cr0.getStmt(0).succ().contains(f));

        assertEquals(2, c1.succ().size());
        assertTrue(c1.succ().contains(f));
        assertTrue(c1.succ().contains(cr1));
        assertEquals(1, cr1.succ().size());
        assertTrue(cr1.succ().contains(cr1.getStmt(0)));
        assertEquals(1, cr1.getStmt(0).succ().size());
        assertTrue(cr1.getStmt(0).succ().contains(f));

        assertEquals(1, f.succ().size());
        assertTrue(f.succ().contains(f.getStmt(0)));
        assertEquals(2, f.getStmt(0).succ().size());
        assertTrue(f.getStmt(0).succ().contains(skip));
        assertTrue(f.getStmt(0).succ().contains(met.exit()));
    }

    @Test
    public void testVarDecl1() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { Int n; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testVarDecl2() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { Int n = 4; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(skip));
    }

    @Test
    public void testVarDecl3() {
        MethodImpl met = getMethod("interface I { Unit m(I i); } class C implements I { Unit m(I i) { Int n = 4 / 0; skip; } }");
        Block b = met.getBlock();

        Stmt s = b.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, s.succ().size());
        assertTrue(s.succ().contains(skip));
        assertTrue(s.succ().contains(met.exit()));
    }

    @Test
    public void testWhile1() {
        MethodImpl met = getMethod("interface I { Unit m(Int n); } class C implements I { Unit m(Int n) { while (n > 0) n = n - 1; skip; } }");
        Block b = met.getBlock();

        WhileStmt w = (WhileStmt) b.getStmt(0);
        Block body = w.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(2, w.succ().size());
        assertTrue(w.succ().contains(body));
        assertTrue(w.succ().contains(skip));
        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(w));
    }

    @Test
    public void testWhile2() {
        MethodImpl met = getMethod("interface I { Unit m(Int n); } class C implements I { Unit m(Int n) { while (4 / n > 0) n = n - 1; skip; } }");
        Block b = met.getBlock();

        WhileStmt w = (WhileStmt) b.getStmt(0);
        Block body = w.getBody();
        Stmt s = body.getStmt(0);
        Stmt skip = b.getStmt(1);

        assertEquals(3, w.succ().size());
        assertTrue(w.succ().contains(body));
        assertTrue(w.succ().contains(skip));
        assertTrue(w.succ().contains(met.exit()));
        assertEquals(1, s.succ().size());
        assertTrue(s.succ().contains(w));
    }

    @Test
    public void testInitBlock() {
        Model m = assertParse("interface I { Unit m(Int n); } class C implements I { { skip; skip; } }");
        ClassDecl d = (ClassDecl) getTestModule(m).lookup(new KindedName(KindedName.Kind.CLASS, "UnitTest.C"));
        InitBlock init = d.getInitBlock();

        Stmt s0 = init.getStmt(0);
        Stmt s1 = init.getStmt(1);

        assertEquals(1, init.pred().size());
        assertTrue(init.pred().contains(init.entry()));
        assertEquals(1, s0.pred().size());
        assertTrue(s0.pred().contains(init));
        assertEquals(1, s0.succ().size());
        assertTrue(s0.succ().contains(s1));
        assertEquals(1, s1.pred().size());
        assertTrue(s1.pred().contains(s0));
        assertEquals(1, s1.succ().size());
        assertTrue(s1.succ().contains(s1.exit()));
    }

    @Test
    public void testRecoverBlock() {
        Model m = assertParse("interface I { Unit m(Int n); } class C implements I { recover { NullPointerException => skip; DivisionByZeroException => skip; } }");
        ClassDecl d = (ClassDecl) getTestModule(m).lookup(new KindedName(KindedName.Kind.CLASS, "UnitTest.C"));

        CaseBranchStmt cs0 = d.getRecoverBranch(0);
        Block cr0 = cs0.getRight();
        CaseBranchStmt cs1 = d.getRecoverBranch(1);
        Block cr1 = cs1.getRight();

        assertEquals(1, cs0.pred().size());
        assertTrue(cs0.pred().contains(d.recoverEntry()));
        assertEquals(2, cs0.succ().size());
        assertTrue(cs0.succ().contains(cs1));
        assertTrue(cs0.succ().contains(cr0));

        assertEquals(1, cr0.succ().size());
        assertTrue(cr0.succ().contains(cr0.getStmt(0)));
        assertEquals(1, cr0.getStmt(0).succ().size());
        assertTrue(cr0.getStmt(0).succ().contains(cr0.exit()));

        assertEquals(1, cs1.pred().size());
        assertTrue(cs1.pred().contains(cs0));
        assertEquals(2, cs1.succ().size());
        assertTrue(cs1.succ().contains(cr1));
        assertTrue(cs1.succ().contains(cs1.exit()));

        assertEquals(1, cr1.succ().size());
        assertTrue(cr1.succ().contains(cr1.getStmt(0)));
        assertEquals(1, cr1.getStmt(0).succ().size());
        assertTrue(cr1.getStmt(0).succ().contains(cr1.exit()));
    }

    static private MethodImpl getMethod(String prog) {
        Model m = assertParse(prog);
        ClassDecl d = (ClassDecl) getTestModule(m).lookup(new KindedName(KindedName.Kind.CLASS, "UnitTest.C"));
        return d.getMethod(0);
    }

    static private ModuleDecl getTestModule(Model m) {
        ModuleDecl md = m.lookupModule("UnitTest");
        assertNotNull("Module UnitTest not found", md);
        return md;
    }
}
