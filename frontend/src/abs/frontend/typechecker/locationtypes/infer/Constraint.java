package abs.frontend.typechecker.locationtypes.infer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import abs.frontend.typechecker.locationtypes.LocationType;

import static abs.frontend.typechecker.locationtypes.LocationType.*;

public abstract class Constraint {
    public final static Integer MUST_HAVE = 2147483647;
    public final static Integer NICE_TO_HAVE = 1;
    public final static Integer SHOULD_HAVE = 100;
    
    protected void prependAll(Integer i, List<List<Integer>> l) {        
        for (List<Integer> subl : l) {
            subl.add(0, i);
        }
    }
    
    public abstract List<List<Integer>> generateSat(Environment e);
    
    public abstract void variables(Set<LocationTypeVariable> vars);
    
    
    List<Integer> generate(LocationTypeVariable v, Environment e, LocationType... types) {
        List<Integer> values = new ArrayList<Integer>();
        for (LocationType t : types) {
            values.add(e.get(v, t));
        }
        return values;
    }
    
    private static class SubConstraint extends Constraint {
        LocationTypeVariable tv1, tv2;

        public SubConstraint(LocationTypeVariable tv1, LocationTypeVariable tv2) {
            this.tv1 = tv1;
            this.tv2 = tv2;
        }

        @Override
        public String toString() {
            return tv1+ " <: " + tv2;
        }
        
        @Override
        public List<List<Integer>> generateSat(Environment e) {
            List<List<Integer>> result = new ArrayList<List<Integer>>();
            List<Integer> values;
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, BOTTOM));
            values.addAll(generate(tv2, e, ALLTYPES));
            result.add(values);
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, NEAR));
            values.addAll(generate(tv2, e, NEAR, SOMEWHERE));
            result.add(values);
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, FAR));
            values.addAll(generate(tv2, e, FAR, SOMEWHERE));
            result.add(values);
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, SOMEWHERE));
            values.addAll(generate(tv2, e, SOMEWHERE));
            result.add(values);
            prependAll(SHOULD_HAVE, result);
            return result;
        }

        @Override
        public void variables(Set<LocationTypeVariable> vars) {
            vars.add(tv1);
            vars.add(tv2);
        }
        
    }
    
    private static class FarConstraint extends Constraint {
        LocationTypeVariable tv1, tv2;

        public FarConstraint(LocationTypeVariable tv1, LocationTypeVariable tv2) {
            this.tv1 = tv1;
            this.tv2 = tv2;
        }

        @Override
        public String toString() {
            return tv1+ " <:_FAR " + tv2;
        }
        
        @Override
        public List<List<Integer>> generateSat(Environment e) {
            List<List<Integer>> result = new ArrayList<List<Integer>>();
            List<Integer> values;
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, BOTTOM));
            values.addAll(generate(tv2, e, ALLTYPES));
            result.add(values);
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, NEAR));
            values.addAll(generate(tv2, e, FAR, SOMEWHERE));
            result.add(values);
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, FAR));
            values.addAll(generate(tv2, e, FAR, SOMEWHERE));
            result.add(values);
            values = new ArrayList<Integer>();
            values.add(- e.get(tv1, SOMEWHERE));
            values.addAll(generate(tv2, e, FAR, SOMEWHERE));
            result.add(values);
            prependAll(SHOULD_HAVE, result);
            return result;
        }

        @Override
        public void variables(Set<LocationTypeVariable> vars) {
            vars.add(tv1);
            vars.add(tv2);
        }
        
    }
    
    public static class DeclConstraint extends Constraint {
        LocationTypeVariable tv;

        public DeclConstraint(LocationTypeVariable tv) {
            this.tv = tv;
        }

        @Override
        public List<List<Integer>> generateSat(Environment e) {
            List<List<Integer>> result = new ArrayList<List<Integer>>();
            List<Integer> values = new ArrayList<Integer>();
            for (LocationType it : ALLTYPES) {
                values.add(e.get(tv, it));
            }
            result.add(values);
            for (LocationType it1 : ALLTYPES) {
                for (LocationType it2 : ALLTYPES) {
                    if (!it1.equals(it2)) {
                        values = new ArrayList<Integer>();
                        values.add(- e.get(tv, it1));
                        values.add(- e.get(tv, it2));
                        result.add(values);
                    }
                }
            }
            prependAll(MUST_HAVE, result);
            return result;
        }

        
        @Override
        public String toString() {
            return "unique "+tv;
        }

        @Override
        public void variables(Set<LocationTypeVariable> vars) {
            vars.add(tv);
        }

    }
    
    private static class AdaptConstraint extends Constraint {
        LocationTypeVariable resultTv, tv, adaptToTv;

        public AdaptConstraint(LocationTypeVariable resultTv, LocationTypeVariable tv, LocationTypeVariable adaptToTv) {
            if (resultTv == null || tv == null || adaptToTv == null)
                throw new IllegalArgumentException("some variable is null: "+resultTv+", "+tv+", "+adaptToTv);
            this.resultTv = resultTv;
            this.tv = tv;
            this.adaptToTv = adaptToTv;
        }

        @Override
        public String toString() {
            return tv + " >> " + adaptToTv + " = " + resultTv;
        }
        
        @Override
        public List<List<Integer>> generateSat(Environment e) {
            List<List<Integer>> result = new ArrayList<List<Integer>>();
            List<Integer> values;
            // if adaptToTv= NEAR then resultTv = tv
            for (LocationType t : ALLTYPES) {
                // adaptToTv!= NEAR or resultTv != T or tv = T
                values = new ArrayList<Integer>();
                values.add(- e.get(adaptToTv, NEAR));
                values.add(- e.get(resultTv, t));
                values.add(e.get(tv, t));
                result.add(values);
            }
            // if adaptToTv = FAR  and tv = NEAR then resultTv = FAR
            values = new ArrayList<Integer>();
            values.add(- e.get(adaptToTv, FAR));
            values.add(- e.get(tv, NEAR));
            values.add(e.get(resultTv, FAR));
            result.add(values);
            // if adaptToTv= FAR  and tv = {FAR, SOMEWHERE} then resultTv = SOMEWHERE
            for (LocationType t : new LocationType[]{FAR, SOMEWHERE}) {
                values = new ArrayList<Integer>();
                values.add(- e.get(adaptToTv, FAR));
                values.add(- e.get(tv, t));
                values.add(e.get(resultTv, SOMEWHERE));
                result.add(values);
            }
            // if adaptToTv= SOMEWHERE  and tv = t then resultTv = SOMEWHERE
            for (LocationType t : new LocationType[]{NEAR, FAR, SOMEWHERE}) {
                values = new ArrayList<Integer>();
                values.add(- e.get(adaptToTv, SOMEWHERE));
                values.add(- e.get(tv, t));
                values.add(e.get(resultTv, SOMEWHERE));
                result.add(values);
            }
            // if tv = BOTTOM then resultTv = BOTTOM
            values = new ArrayList<Integer>();
            values.add(- e.get(tv, BOTTOM));
            values.add(e.get(resultTv, BOTTOM));
            result.add(values);
            
            prependAll(MUST_HAVE, result);
            // adaptToTv should always be unequal to BOTTOM
            //result.addAll(Constraint.neqConstraint(adaptToTv, LocationTypeVariable.ALWAYS_BOTTOM, SHOULD_HAVE).generateSat(e));
            return result;
        }

        @Override
        public void variables(Set<LocationTypeVariable> vars) {
            vars.add(resultTv);
            vars.add(tv);
            vars.add(adaptToTv);
        }
        
    }
    
    private static class EqConstraint extends Constraint {
        LocationTypeVariable tv1, tv2;
        Integer importance;

        public EqConstraint(LocationTypeVariable tv1, LocationTypeVariable tv2, Integer importance) {
            this.tv1 = tv1;
            this.tv2 = tv2;
            this.importance = importance;
        }

        @Override
        public List<List<Integer>> generateSat(Environment e) {
            List<List<Integer>> result = new ArrayList<List<Integer>>();
            List<Integer> values;
            for (LocationType t : ALLTYPES) {
                values = new ArrayList<Integer>();
                values.add(- e.get(tv1, t));
                values.add(e.get(tv2, t));
                result.add(values);
            }
            prependAll(importance, result);
            return result;
        }
        
        @Override
        public String toString() {
            return tv1 + " = " + tv2;
        }

        @Override
        public void variables(Set<LocationTypeVariable> vars) {
            vars.add(tv1);
            vars.add(tv2);
        }
        
    }
    
    private static class ConstConstraint extends Constraint {
        LocationTypeVariable tv;
        LocationType[] t;
        Integer importance;

        public ConstConstraint(LocationTypeVariable tv, LocationType[] t, Integer importance) {
            this.tv = tv;
            this.t = t;
            this.importance = importance;
        }

        @Override
        public List<List<Integer>> generateSat(Environment e) {
            List<List<Integer>> result = new ArrayList<List<Integer>>();
            List<Integer> values;
            values = new ArrayList<Integer>();
            for (LocationType lt : t) {
                values.add(e.get(tv, lt));
            }
            result.add(values);
            /*for (LocationType t2 : LocationType.ALLTYPES) {
                if (!t.equals(t2)) {
                    values = new ArrayList<Integer>();
                    values.add(- e.get(tv, t2));
                    result.add(values);
                }
            }*/
            prependAll(importance, result);
            return result;
        }
        
        @Override
        public String toString() {
            return tv + " := " + Arrays.toString(t);
        }

        @Override
        public void variables(Set<LocationTypeVariable> vars) {
            vars.add(tv);
        }
        
    }
    
    // tv1 < tv2
    public static Constraint subConstraint(LocationTypeVariable tv1, LocationTypeVariable tv2) {
        return new SubConstraint(tv1, tv2);
    }
    
    // tv1 = tv2
    public static Constraint eqConstraint(LocationTypeVariable tv1, LocationTypeVariable tv2, Integer importance) {
        return new EqConstraint(tv1, tv2, importance);
    }
    
    // tv := t
    public static Constraint constConstraint(LocationTypeVariable tv, LocationType t, Integer importance) {
        return new ConstConstraint(tv, new LocationType[]{t}, importance);
    }
    
    public static Constraint constConstraint(LocationTypeVariable tv, LocationType[] tl, Integer importance) {
        return new ConstConstraint(tv, tl, importance);
    }

    // tv1 = tv2 |> tv3
    public static Constraint adaptConstraint(LocationTypeVariable resultTv, LocationTypeVariable tv, LocationTypeVariable adaptTo) {
        return new AdaptConstraint(resultTv, tv, adaptTo);
    }
    
    public static Constraint declConstraint(LocationTypeVariable tv) {
        return new DeclConstraint(tv);
    }

    // tv1 <_far tv2
    public static Constraint farConstraint(LocationTypeVariable tv1, LocationTypeVariable tv2) {
        return new FarConstraint(tv1, tv2);
    }
}
