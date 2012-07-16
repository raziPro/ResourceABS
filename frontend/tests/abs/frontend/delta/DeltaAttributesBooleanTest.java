/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.frontend.delta;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import abs.common.WrongProgramArgumentException;
import abs.frontend.ast.*;
import abs.frontend.delta.exceptions.ASTNodeNotFoundException;

@RunWith(Parameterized.class)
public class DeltaAttributesBooleanTest extends DeltaTest {
    protected String product;
    protected String expected;
    public DeltaAttributesBooleanTest(String p, String x) {
        this.product = p;
        this.expected = x;
    }
    
    @Parameters
    public static java.util.Collection<?> data() {
        final Object[][] data = new String[][] {
                {"P1", "True()"},
                {"P2", "False()"}
        };
        return Arrays.asList(data);
    }
    
    @Test
    public void passFeatureAsBoolean() throws ASTNodeNotFoundException, WrongProgramArgumentException {
        Model model = assertParseOk(
                "module M;"
                + "delta D(Bool f);"
                + "    adds class M.C { Bool myField = f; }" 
                + "productline PL;"
                + "    features F, G;"
                + "    delta D(F) when G;"
                + "product P1(F, G);"
                + "product P2(G);"
        );
        
        model.flattenForProduct(product);
        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        assertTrue(cls.getField(0).getName().equals("myField"));
        assertTrue(cls.getField(0).getInitExp().value.toString().equals(expected));
    }

    @Test
    public void passBooleanFeatureAttribute() throws ASTNodeNotFoundException, WrongProgramArgumentException {
        Model model = assertParseOk(
                "module M;"
                + "delta D(Bool attr);"
                + "adds class M.C { Bool myField = attr; }"
                + "productline PL;"
                + "    features F;"
                + "    delta D(F.a) when F;"
                + "product P1(F{a=True});"
                + "product P2(F{a=False});"
        );
        
        model.flattenForProduct(product);
        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        assertTrue(cls.getField(0).getName().equals("myField"));
        assertTrue(cls.getField(0).getInitExp().value.toString().equals(expected));
    }

    @Test
    public void passBooleanConstant() throws ASTNodeNotFoundException, WrongProgramArgumentException {
        Model model = assertParseOk(
                "module M;"
                + "delta D(Bool attr);"
                + "    adds class M.C { Bool myField = attr; }"
                + "productline PL;"
                + "    features A,B;"
                + "    delta D(True) when A;"
                + "    delta D(False) when B;"
                + "product P1(A);"
                + "product P2(B);"
        );
        
        model.flattenForProduct(product);
        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        assertEquals("myField", cls.getField(0).getName());
        assertEquals("Product " + product, expected, cls.getField(0).getInitExp().value.toString());
    }

}
