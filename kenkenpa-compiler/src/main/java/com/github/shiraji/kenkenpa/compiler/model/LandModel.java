package com.github.shiraji.kenkenpa.compiler.model;

import com.github.shiraji.kenkenpa.annotations.Land;
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper;

import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;

public class LandModel {
    private Map<String, Element> mLandMap = new HashMap<>();

    public boolean isValidLandAnnotation(Land land) {
        return !mLandMap.containsKey(land.value());
//            KenKenPaHelper.logParsingError(mProcessingEnv, element, Land.class, new IllegalArgumentException(String.format(
//                    "state %s has multiple @Land", land.value())));
    }

    public void addLand(Element element) {
        Land land = element.getAnnotation(Land.class);
        if (land == null) {
            return;
        }

        if (!KenKenPaHelper.hasNoParameters(element)) {
            return;
        }

//        if (mLandMap.containsKey(land.value())) {
//            return;
//            KenKenPaHelper.logParsingError(mProcessingEnv, element, Land.class, new IllegalArgumentException(String.format(
//                    "state %s has multiple @Land", land.value())));
//        }
        mLandMap.put(land.value(), element);
    }
}
