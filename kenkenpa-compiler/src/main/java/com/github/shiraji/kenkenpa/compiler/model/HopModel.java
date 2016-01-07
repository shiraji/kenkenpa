package com.github.shiraji.kenkenpa.compiler.model;

import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopModel {
    Map<Element, Annotation> mHopMap = new HashMap<>();

    public boolean isValidHops(Element element) {
        Hops hops = element.getAnnotation(Hops.class);
        if(hops == null) {
            return true;
        }

        List<String> hopsFrom = new ArrayList<>();
        for (Hop hop : hops.value()) {
            if (hopsFrom.contains(hop.from())) {
                return false;
            }
        }
        return true;
    }

    private void handleHopAnnotation(Element element) {
        Hop hop = element.getAnnotation(Hop.class);
        if (hop == null) {
            return;
        }
        mHopMap.put(element, hop);
    }

    private void handleHopsMethods(Element element) {
        Hops hops = element.getAnnotation(Hops.class);
        if (hops == null) {
            return;
        }
        mHopMap.put(element, hops);
    }
}
