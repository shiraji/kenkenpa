package com.github.shiraji.kenkenpa.compiler.model;

import com.github.shiraji.kenkenpa.annotations.TakeOff;
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper;

import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;

public class TakeOffModel {
    private Map<String, Element> mTakeOffMap = new HashMap<>();

    public boolean isValidTakeOff(TakeOff takeOff) {
        return !mTakeOffMap.containsKey(takeOff.value());
    }

    public void addTakeOffAnnotationInfo(Element element) {
        TakeOff takeOff = element.getAnnotation(TakeOff.class);
        if (takeOff == null) {
            return;
        }

        if (!KenKenPaHelper.hasNoParameters(element)) {
            return;
        }
//
//        if (mTakeOffMap.containsKey(takeOff.value())) {
//            logParsingError(element, TakeOff.class,
//                    new IllegalArgumentException(String.format("state %s has multiple @TakeOff", takeOff.value())));
//        }

//        if(!hasFromState(takeOff.value())) {
//            logParsingError(element, TakeOff.class,
//                    new IllegalArgumentException(String.format("No state %s set Hop(from)", takeOff.value())));
//        }

        mTakeOffMap.put(takeOff.value(), element);
    }

}
