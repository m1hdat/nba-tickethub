package com.example.nbatickethub.geometry;
import org.junit.Test;
import static org.junit.Assert.*;
public class SectionHitTesterTest {
    private final float[][] polygons = {{0,0,10,0,10,10,0,10},{12,0,22,0,22,10,12,10}};
    private final float[] labels = {5,5,17,5};
    @Test public void regionsEdgesAislesAndHiddenSections() {
        assertEquals(0,SectionHitTester.find(1,1,0,0,polygons,labels,new boolean[]{true,true}));
        assertEquals(1,SectionHitTester.find(22,10,0,0,polygons,labels,new boolean[]{true,true}));
        assertEquals(-1,SectionHitTester.find(11,1,0,0,polygons,labels,new boolean[]{true,true}));
        assertEquals(-1,SectionHitTester.find(17,5,2,2,polygons,labels,new boolean[]{true,false}));
    }
    @Test public void nearestLabelWinsOverOverlappingTouchTargets() {
        assertEquals(1,SectionHitTester.find(17,5,15,5,polygons,labels,new boolean[]{true,true}));
        assertEquals(0,SectionHitTester.find(5,-2,8,8,polygons,labels,new boolean[]{true,true}));
    }
}
