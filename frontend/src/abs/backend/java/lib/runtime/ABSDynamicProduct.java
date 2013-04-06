/**
 * Copyright (c) 2009-2011, The HATS Consortium. All rights reserved. 
 * This file is licensed under the terms of the Modified BSD License.
 */
package abs.backend.java.lib.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import abs.backend.java.codegeneration.dynamic.DynamicException;
import abs.backend.java.lib.runtime.metaABS.Product;

public class ABSDynamicProduct extends ABSDynamicObject {

    public ABSDynamicProduct() {
        super(Product.singleton());
    }
    
    private String name;
    private Set<String> features = new HashSet<String>();
    private List<ABSDynamicProduct> configurableProducts = new ArrayList<ABSDynamicProduct>();
    private HashMap<String, List<String>> deltas = new HashMap<String, List<String>>();
    private HashMap<String, String> update = new HashMap<String, String>();
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
   
    public Set<String> getFeatures() {
        return features;
    }
    public void setFeatures(Set<String> features) {
        this.features = features;
    }
    public void addFeature(String feature) {
        features.add(feature);
    }

    public void setConfigurableProducts(List<ABSDynamicProduct> productNames) {
        configurableProducts = productNames;
    }
    public List<ABSDynamicProduct> getConfigurableProducts() {
        return configurableProducts;
    }
    
    public void setDeltas(String productName, List<String> deltaNames) {
        deltas.put(productName, deltaNames);
    }

    public List<String> getDeltas(String productName) {
        if (deltas.containsKey(productName))
            return deltas.get(productName);
        else 
            throw new DynamicException("Product " + name + " cannot be reconfigured into product " + productName + ".");
    }

    public void setUpdate(String productName, String upd) {
        if (! update.containsKey(productName))
            update.put(productName, upd);
        else
            throw new DynamicException("Product " + name + " already defined an update for " + productName + ".");
    }
    public String getUpdate(String productName) {
        return update.get(productName);
    }
}
