package com.github.shiraji.kenkenpa.compiler.model

import com.github.shiraji.kenkenpa.annotations.Hop
import com.github.shiraji.kenkenpa.annotations.Hops
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper

import javax.lang.model.element.Element
import java.util.ArrayList
import java.util.HashMap

class HopModel {
    internal var mHopMap: MutableMap<Element, Annotation> = HashMap()

    fun isValidHops(element: Element): Boolean {
        val hops = element.getAnnotation(Hops::class.java) ?: return true
        val hopsFrom = ArrayList<String>()
        hops.value.forEach {
            if (hopsFrom.contains(it.from)) {
                return false
            }
            hopsFrom.add(it.from)
        }
        return true
    }

    private fun handleHopAnnotation(element: Element) {
        val hop = element.getAnnotation(Hop::class.java) ?: return
        mHopMap.put(element, hop)
    }

    private fun handleHopsMethods(element: Element) {
        val hops = element.getAnnotation(Hops::class.java) ?: return
        mHopMap.put(element, hops)
    }
}
