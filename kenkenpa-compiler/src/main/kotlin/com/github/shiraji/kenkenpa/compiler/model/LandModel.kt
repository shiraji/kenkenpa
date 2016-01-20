package com.github.shiraji.kenkenpa.compiler.model

import com.github.shiraji.kenkenpa.annotations.Land
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper

import javax.lang.model.element.Element
import java.util.HashMap

class LandModel {
    private val mLandMap = HashMap<String, Element>()

    fun isValidLandAnnotation(land: Land): Boolean {
        return !mLandMap.containsKey(land.value)
        //            KenKenPaHelper.logParsingError(mProcessingEnv, element, Land.class, new IllegalArgumentException(String.format(
        //                    "state %s has multiple @Land", land.value())));
    }

    fun addLand(element: Element) {
        val land = element.getAnnotation(Land::class.java) ?: return

        if (!KenKenPaHelper().hasNoParameters(element)) {
            return
        }

        //        if (mLandMap.containsKey(land.value())) {
        //            return;
        //            KenKenPaHelper.logParsingError(mProcessingEnv, element, Land.class, new IllegalArgumentException(String.format(
        //                    "state %s has multiple @Land", land.value())));
        //        }
        mLandMap.put(land.value, element)
    }
}
