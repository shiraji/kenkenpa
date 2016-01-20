package com.github.shiraji.kenkenpa.compiler.model

import com.github.shiraji.kenkenpa.annotations.TakeOff
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper

import javax.lang.model.element.Element
import java.util.HashMap

class TakeOffModel {
    private val mTakeOffMap = HashMap<String, Element>()

    fun isValidTakeOff(takeOff: TakeOff): Boolean {
        return !mTakeOffMap.containsKey(takeOff.value)
    }

    fun addTakeOffAnnotationInfo(element: Element) {
        val takeOff = element.getAnnotation(TakeOff::class.java) ?: return

        if (!KenKenPaHelper().hasNoParameters(element)) {
            return
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

        mTakeOffMap.put(takeOff.value, element)
    }

}
