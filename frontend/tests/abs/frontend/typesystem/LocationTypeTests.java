package abs.frontend.typesystem;

import static org.junit.Assert.*;


import java.util.Map;

import org.junit.Test;

import abs.frontend.FrontendTest;
import abs.frontend.analyser.SemanticErrorList;
import abs.frontend.ast.ClassDecl;
import abs.frontend.ast.Model;
import abs.frontend.ast.VarDeclStmt;
import abs.frontend.typechecker.Type;
import abs.frontend.typechecker.locationtypes.LocationType;
import abs.frontend.typechecker.locationtypes.LocationTypeCheckerException;
import abs.frontend.typechecker.locationtypes.LocationTypeExtension;
import abs.frontend.typechecker.locationtypes.infer.LocationTypeInferrerExtension;
import static abs.ABSTest.Config.*;


public class LocationTypeTests extends FrontendTest {
    
    @Test
    public void fieldDecl() {
        Model m = assertParse("interface I { } class C { [Far] I i; }",WITH_STD_LIB);
        ClassDecl decl = getFirstClassDecl(m);
        LocationType ft = LocationTypeExtension.getLocationTypeFromAnnotations(decl.getField(0).getType());
        assertEquals(LocationType.FAR,ft);
    }

    @Test
    public void varDecl() {
        Model m = assertParse("interface I { } { [Somewhere] I i; [Near] I jloc; i = jloc; }",WITH_STD_LIB);
        m.typeCheck();
        assertEquals(LocationType.NEAR,LocationTypeExtension.getLocationTypeFromAnnotations(getTypeOfFirstAssignment(m)));
    }
    private static String INT = "interface I { [Near] I m(); [Far] I n([Near] I i); Unit farM([Far] I i);}" +
    		" class C([Somewhere] I f) implements I { " +
    		"    [Far] I farField; " +
    		"    [Near] I nearField; " +
    		"    [Near] I m() { [Near] I i; i = this; return nearField; }  " +
    		"    [Far] I n([Near] I i) { return farField; }" +
    		"    Unit farM([Far] I i) { }}" +
    		" interface J { } class E implements J { }";

    
    @Test
    public void syncCall() {
        assertTypeOk("{ [Near] I i; i = i.m(); }");
    }

    @Test
    public void syncCallOnThis() {
        assertTypeOk("class D { Unit m() { this.m(); } }");
    }
    
    @Test
    public void nullLit() {
        assertTypeOk("{ [Near] I i; i = null; [Far] I j; j = null; }");
    }
    
    @Test
    public void syncCallParam() {
        assertTypeOk("{ [Near] I i; [Far] I j; j = i.n(i); }");
    }

    @Test
    public void newCog() {
        assertTypeOk("{ [Far] I i; i = new cog C(i); }");
    }

    @Test
    public void newObject() {
        assertTypeOk("{ [Near] J i; i = new E(); }");
        assertTypeOk("{ [Somewhere] J i; i = new E(); }");
    }

    @Test
    public void typeMaybe() {
        assertTypeOk("{ [Near] I i; Maybe<[Near] I> m = Just(i); }");
    }
    
    @Test
    public void syncCallOnMaybeThis() {
        String s = "def X id<X>(X x) = x; interface K { Unit m(Maybe<[Near] K> p); } " +
          "class D implements K { [Near] K getThis() { return this; } Unit m(Maybe<[Near] K> p)";
        assertTypeOk(s+ "{ this.m(Just(this)); } }");
        assertTypeOk(s+ "{ [Near] K k; k = this; this.m(Just(k)); } }");
        assertTypeOk(s+ "{ [Near] K k; this.m(case Just(k) { Just(x) => Just(x); }); } }");
        assertTypeOk(s+ "{ [Near] K k; this.m(Just(id(k))); } }");
    }

    @Test
    public void typeParamInference() {
        assertTypeOk("{ [Near] I i; Maybe<Maybe<Bool>> m = Nothing; }");
    }
    
    @Test
    public void defaultTyping() {
        assertTypeOk("{ I i; [Far] I f; i = new C(f); }");
    }

    @Test
    public void futureTyping() {
        assertTypeOk("{ I i; [Far] I f; Fut<I> fut; i = new C(f); fut = i!m(); }");
    }
    
    
    @Test
    public void syncCallInfer() {
        assertInferOk("interface I { Unit m(); } { I i; i.m(); }", LocationType.NEAR);
    }
    
    @Test
    public void callParamInfer() {
        Model m = assertInferOk("interface I { Unit m(I i); } class C implements I { Unit m(I i) { I j; j = new C(); i.m(j); } } { }");
        ClassDecl cd = (ClassDecl) m.getCompilationUnit(1).getModuleDecl(0).getDecl(1);
        Type t = cd.getMethod(0).getMethodSig().getParam(0).getType();
        LocationType lt = m.getLocationTypeInferenceResult().get(LocationTypeInferrerExtension.getLV(t));
        assertEquals(LocationType.NEAR, lt);
    }
    
    
    @Test
    public void newCOGInfer() {
        assertInferOk("interface I { } class C implements I {} { I i; i = new cog C(); }", LocationType.FAR);
    }
    
    @Test
    public void newObjectInfer() {
        assertInferOk("interface I { } class C implements I {} { I i; i = new C(); }", LocationType.NEAR);
    }

    @Test
    public void newObjectInfer2() {
        assertInferOk("interface I { I getI(); } class C implements I { I i; { i = new C(); } I getI() { return i; } } " +
        		"{ I i; I j; j = new C(); i = j.getI(); }", LocationType.NEAR);
    }
    
    @Test
    public void newObjectInfer3() {
        assertInferOk("interface I { } class C implements I {} { I i; i = new C(); i = new cog C(); }", LocationType.SOMEWHERE);
    }
    
    @Test
    public void annotatedlocaVarInfer() {
        assertInferOk("interface I { } class C implements I {} { I i; [Near] I j; i = j; }", LocationType.NEAR);
    }
    
    @Test
    public void overrideOK() {
        assertInferOk("interface I { [Somewhere] I m([Far] I i); } class C implements I { [Near] I m([Somewhere] I i) { return null; } } { }");
    }
    
    @Test
    public void overrideminimal() {
        assertInferOk("interface I { I m([Far] I i); } class C implements I { [Near] I m([Somewhere] I i) { return null; } } { I i; [Near] I k; Fut<I> j; j = k!m(null); i = j.get; }", LocationType.NEAR);
    }
    
    @Test
    public void callNullParam() {
        assertTypeOkOnly("interface I2 { Unit m([Near] I2 i); } { [Far] I2 i; i!m(null); }");
    }
    
    @Test
    public void callNullParam2() {
        assertTypeOk("interface I2 { Unit m([Near] I2 i); } { [Somewhere] I2 i; i!m(null); }");
    }

    @Test
    public void callReturn() {
        assertInferOk("interface I2 { [Near] I2 m(); }  { I2 i2; [Far] I2 i; Fut<I2> f; f = i!m(); i2 = f.get; }", LocationType.FAR);
    }

    @Test
    public void callReturn2() {
        assertInferOk("module M.S1; export *; interface I { [Near] I m(); } class C implements I { [Near] I m() { return null; } } " +
                      "module M.S2; import * from M.S1; { I i; i = new cog C(); i!m(); }", LocationType.FAR);
    }
    
    // negative tests:

   
    @Test
    public void typeMaybeError() {
        assertTypeErrorOnly("interface I { } { [Far] I i; Maybe<[Near] I> m = Just(i); }");
    }

    @Test
    public void typeListError() {
        assertTypeErrorOnly("interface I {} { List<[Far] I> list = Nil; [Near] I i; list = Cons(i,list); }");
    }
    
    @Test
    public void callWrongParam() {
        assertTypeErrorOnly("interface I { Unit m([Near] I i); } { [Far] I i; i!m(i); }");
    }
    
    @Test
    public void assignWrong() {
        assertTypeError("{ [Far] I i; [Near] I j; i = j; }");
    }

    @Test
    public void illegalSyncCall() {
        assertTypeError("{ [Far] I i; i.m(); }");
    }

    @Test
    public void illegalAsyncCall() {
        assertTypeError("{ [Far] I i; i!farM(i); }");
    }
    
    @Test
    public void syncCallWrongParam() {
        assertTypeError("{ [Near] I i; [Far] I j; j = i.n(j); }");
    }
    
    @Test
    public void multipleError() {
        Model m = assertParse("interface I { } class C { [Far] [Near] I i; }",WITH_STD_LIB);
        ClassDecl decl = getFirstClassDecl(m);
        try {
            LocationTypeExtension.getLocationTypeFromAnnotations(decl.getField(0).getType());
            fail("Expected exception");
        } catch(LocationTypeCheckerException e) {
            assertTrue(true);
        }
    }
    
    @Test
    public void overrideReturnFailed() {
        assertInferFails("interface I { [Near] I m(I i); } class C implements I { [Somewhere] I m(I i) { return null; } } { }");
    }
    
    @Test
    public void overrideParameterFailed() {
        assertInferFails("interface I { I m([Somewhere] I i); } class C implements I { I m([Near] I i) { return null; } } { }");
    }
    
    private void assertTypeError(String code) {
        assertTypeErrorOnly(INT+code);
    }
    
    private void assertTypeErrorOnly(String code) {
        Model m = assertParse(code,WITH_STD_LIB);
        LocationTypeExtension te = new LocationTypeExtension(m);
        m.registerTypeSystemExtension(te);
        SemanticErrorList e = m.typeCheck();
        assertFalse(e.isEmpty());
        assertInferFails(code);
    }
    
    private void assertTypeOk(String code) {
        assertTypeOkOnly(INT+code);
    }

    private void assertTypeOkOnly(String code) {
        Model m = assertParse(code,WITH_STD_LIB);
        LocationTypeExtension te = new LocationTypeExtension(m);
        m.registerTypeSystemExtension(te);
        SemanticErrorList e = m.typeCheck();
        assertTrue(e.isEmpty() ? "" : "Found error "+e.get(0).getMessage(),e.isEmpty());
        assertInferOk(code);
    }
    
    private Model assertInfer(String code, LocationType expected, boolean fails) {
        Model m = assertParse(code,WITH_STD_LIB);
        //m.setLocationTypingEnabled(true);
        LocationTypeInferrerExtension ltie = new LocationTypeInferrerExtension(m);
        m.registerTypeSystemExtension(ltie);
        SemanticErrorList e = m.typeCheck();
        //System.out.println(ltie.getConstraints());
        assertEquals(e.isEmpty() ? "" : "Found error: "+e.get(0).getMessage(), fails, !e.isEmpty()); 
        
        //assertTrue("Inference failed", generated != null);
        //assertEquals(fails, generated == null);
        if (expected != null) {
            VarDeclStmt vds = ((VarDeclStmt)m.getMainBlock().getStmt(0));
            LocationType t = m.getLocationTypeInferenceResult().get(LocationTypeInferrerExtension.getLV(vds.getVarDecl().getType()));
            assertEquals(expected, t);
        }
        return m;
    }
    
    private Model assertInferOk(String string, LocationType expected) {
        return assertInfer(string, expected, false);
    }
    
    private Model assertInferOk(String string) {
        return assertInfer(string, null, false);       
    }
    
    private void assertInferFails(String string) {
        assertInfer(string, null, true);
    }


    



}
