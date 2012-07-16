/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.frontend.delta;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.*;

import abs.frontend.ast.*;
import abs.frontend.delta.exceptions.*;

/* test: 
 * 1. add/remove implemented interfaces of a class
 * 2. add/remove/modify interface declarations
 */
public class AddRemoveInterfacesTest extends DeltaTest {
    
    @Test
    public void addIfaceDecl() throws ASTNodeNotFoundException {
        final ArrayList<String> codeVariants = new ArrayList<String>(Arrays.asList(
                "module M;"
                + "interface I { Int fooi(); }"
                + "delta D;"
                + "adds interface M.J { Int fooj(); }"
                ,
                "module M;"
                + "interface I { Int fooi(); }"
                + "delta D;"
                + "adds interface M.J extends I { Int fooj(); }"
                ,
                "module M;"
                + "interface I { Int fooi(); }"
                + "delta D;"
                + "adds interface M.J extends M.I { Int fooj(); }"
                ,
                "module M;"
                + "interface I { Int fooi(); }"
                + "delta D; uses M;"
                + "adds interface J { Int fooj(); }"
                ,
                "module M;"
                + "interface I { Int fooi(); }"
                + "delta D; uses M;"
                + "adds interface J extends I { Int fooj(); }"
        ));
        
        // both qualified and unqualified interface names should yield the same result
        for (String code : codeVariants) {
            Model model = assertParseOk(code);

            InterfaceDecl ifaceI = (InterfaceDecl) findDecl(model, "M", "I");
            assertNotNull(ifaceI);
            DeltaDecl delta = findDelta(model, "D");
            assertNotNull(delta);
            InterfaceDecl ifaceJ = (InterfaceDecl) findDecl(model, "M", "J");
            assertNull(ifaceJ);

            model.applyDelta(delta);
            ifaceJ = (InterfaceDecl) findDecl(model, "M", "J");
            assertNotNull(ifaceJ);
        }
    }

    @Test
    public void modifyIfaceDeclAddMethodSig() throws ASTNodeNotFoundException {
        final ArrayList<String> codeVariants = new ArrayList<String>(Arrays.asList(
                "module M;"
                + "interface I { Int a(); }"
                + "delta D;"
                + "modifies interface M.I { adds Unit b(); }"
                ,
                "module M;"
                + "interface I { Int a(); }"
                + "delta D; uses M;"
                + "modifies interface I { adds Unit b(); }"
        ));

        for (String code : codeVariants) {
            Model model = assertParseOk(code);

            InterfaceDecl iface = (InterfaceDecl) findDecl(model, "M", "I");
            assertEquals(1, iface.getBodys().getNumChild());
            assertEquals("a", iface.getBody(0).getName());

            DeltaDecl delta = findDelta(model, "D");
            model.applyDelta(delta);

            assertEquals(2, iface.getBodys().getNumChild());
            assertEquals("a", iface.getBody(0).getName());
            assertEquals("b", iface.getBody(1).getName());
        }
    }

    @Test
    public void modifyIfaceDeclRemoveMethodSig() throws ASTNodeNotFoundException {
        final ArrayList<String> codeVariants = new ArrayList<String>(Arrays.asList(
                "module M;"
                + "interface I { Int a(); Unit b(); }"
                + "delta D;"
                + "modifies interface M.I { removes Int a(); }"
                ,
                "module M;"
                + "interface I { Int a(); Unit b(); }"
                + "delta D; uses M;"
                + "modifies interface I { removes Int a(); }"
        ));
                
        for (String code : codeVariants) {
            Model model = assertParseOk(code);
            
            InterfaceDecl iface = (InterfaceDecl) findDecl(model, "M", "I");
            assertEquals(2, iface.getBodys().getNumChild());
            assertEquals("a", iface.getBody(0).getName());
            assertEquals("b", iface.getBody(1).getName());

            DeltaDecl delta = findDelta(model, "D");
            model.applyDelta(delta);

            assertEquals(1, iface.getBodys().getNumChild());
            assertEquals("b", iface.getBody(0).getName());
        }
    }
        
    @Test
    public void addImplements() throws ASTNodeNotFoundException {
        Model model = assertParseOk(
                "module M;"
                + "interface I {}"
                + "class C implements I {}"
                + "delta D;"
                + "adds interface M.J { Int foo(); }"
                + "modifies class M.C adds J { adds Int foo() { return 99; } }"
        );

        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        DeltaDecl delta = findDelta(model, "D");
        assertEquals(1, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("I", cls.getImplementedInterfaceUse(0).getName());
        
        model.applyDelta(delta);
        // make sure the class now also implements interface J
        assertEquals(2, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("I", cls.getImplementedInterfaceUse(0).getName());
        assertEquals("J", cls.getImplementedInterfaceUse(1).getName());
    }

    @Test
    public void removeImplements() throws ASTNodeNotFoundException {
        Model model = assertParseOk(
                "module M;"
                + "interface I {}"
                + "class C implements I {}"
                + "delta D;"
                + "modifies class M.C removes I {}"
        );

        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        DeltaDecl delta = findDelta(model, "D");
        assertEquals(1, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("I", cls.getImplementedInterfaceUse(0).getName());
        
        model.applyDelta(delta);
        assertEquals(0, cls.getImplementedInterfaceUses().getNumChild());
    }

    @Test
    public void removeImplementsNotFound() throws ASTNodeNotFoundException {
        Model model = assertParseOk(
                "module M;"
                + "interface I {}"
                + "class C implements I {}"
                + "delta D;"
                + "modifies class M.C removes K {}"
        );

        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        DeltaDecl delta = findDelta(model, "D");
        assertEquals(1, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("I", cls.getImplementedInterfaceUse(0).getName());

        try {
            model.applyDelta(delta);
        } catch (ASTNodeNotFoundException e) {
            return; // this is the expected outcome
        }
        fail("Expected ASTNodeNotFoundException");
    }

    @Test
    public void addRemoveImplements() throws ASTNodeNotFoundException {
        Model model = assertParseOk(
                "module M;"
                + "interface I {}"
                + "class C implements I {}"
                + "delta D;"
                + "adds interface M.J {}"
                + "modifies class M.C adds J removes I {}"
        );

        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        DeltaDecl delta = findDelta(model, "D");
        assertEquals(1, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("I", cls.getImplementedInterfaceUse(0).getName());
        
        model.applyDelta(delta);
        assertEquals(1, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("J", cls.getImplementedInterfaceUse(0).getName());
    }

    @Test
    public void addRemove2Implements() throws ASTNodeNotFoundException {
        Model model = assertParseOk(
                "module M;"
                + "interface H {}"
                + "interface I {}"
                + "class C implements H,I {}"
                + "delta D;"
                + "adds interface M.J {}"
                + "adds interface M.K {}"
                + "modifies class M.C adds J,K removes H,I {}"
        );

        ClassDecl cls = (ClassDecl) findDecl(model, "M", "C");
        DeltaDecl delta = findDelta(model, "D");
        assertEquals(2, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("H", cls.getImplementedInterfaceUse(0).getName());
        assertEquals("I", cls.getImplementedInterfaceUse(1).getName());
        
        model.applyDelta(delta);
        assertEquals(2, cls.getImplementedInterfaceUses().getNumChild());
        assertEquals("J", cls.getImplementedInterfaceUse(0).getName());
        assertEquals("K", cls.getImplementedInterfaceUse(1).getName());
    }


}
