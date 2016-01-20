package com.github.shiraji.kenkenpa.compiler.model

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

import com.github.shiraji.kenkenpa.annotations.Hop
import com.github.shiraji.kenkenpa.annotations.Hops
import com.github.shiraji.kenkenpa.annotations.KenKenPa
import com.github.shiraji.kenkenpa.annotations.Land
import com.github.shiraji.kenkenpa.annotations.TakeOff
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper

class KenKenPaModel(internal var mProcessingEnv: ProcessingEnvironment, typeElement: TypeElement) {
    internal var mDefaultState: String? = null
    internal var mStates: MutableSet<String> = HashSet()
    internal var mLandMap: MutableMap<String, Element> = HashMap()
    internal var mTakeOffMap: MutableMap<String, Element> = HashMap()
    internal var mHopMap: MutableMap<Element, Annotation> = HashMap()

    init {
        init(typeElement)
    }

    private fun init(typeElement: TypeElement) {
        val annotation = typeElement.getAnnotation(KenKenPa::class.java)
        mDefaultState = annotation.value

        for (enclosedElement in typeElement.enclosedElements) {
            if (enclosedElement.kind != ElementKind.METHOD) {
                continue
            }
            handleHopAnnotation(enclosedElement)
            handleHopsMethods(enclosedElement)
            addLandAnnotationInfo(enclosedElement)
            addTakeOffAnnotationInfo(enclosedElement)
        }

        validateLandAnnotation()
    }

    private fun validateLandAnnotation() {
        // hasToState(mLandMap.keySet());

        for (entry in mLandMap.entries) {

            if (mDefaultState != entry.key) {
                KenKenPaHelper().logParsingError(mProcessingEnv, entry.value, Land::class.java,
                        IllegalArgumentException("No state %s set Hop(to)".format(entry.key)))
            }
        }
    }

    private fun hasToState(landStates: Set<String>): Boolean {
        val hops = LinkedList<Hop>()
        for (entry in mHopMap.entries) {
            hops.addAll(createHopListFromEntry(entry.value))
        }

        val hopToStates = HashSet<String>()
        for (hop in hops) {
            hopToStates.add(hop.to)
        }
        hopToStates.add(mDefaultState!!)
        return hopToStates.containsAll(landStates)
    }

    private fun createHopListFromEntry(annotation: Annotation): List<Hop> {
        val hopList = LinkedList<Hop>()
        if (annotation is Hop) {
            hopList.add(annotation)
        } else if (annotation is Hops) {
            hopList.addAll(Arrays.asList<Hop>(*annotation.value))
        }
        return hopList
    }

    private fun handleHopAnnotation(element: Element) {
        val hop = element.getAnnotation(Hop::class.java) ?: return
        mStates.add(hop.from)
        mStates.add(hop.to)
        mHopMap.put(element, hop)
    }

    private fun handleHopsMethods(element2: Element) {
        val hops = element2.getAnnotation(Hops::class.java) ?: return
        addHopsToStates(element2, hops)
        mHopMap.put(element2, hops)
    }

    private fun addHopsToStates(element2: Element, hops: Hops) {
        val hopsFrom = ArrayList<String>()
        for (hop in hops.value) {
            if (hopsFrom.contains(hop.from)) {
                KenKenPaHelper().logParsingError(mProcessingEnv, element2, Hops::class.java, IllegalArgumentException(
                        "'from' must not have the same state."))
            }
            hopsFrom.add(hop.from)
            mStates.add(hop.from)
            mStates.add(hop.to)
        }
    }

    private fun addLandAnnotationInfo(element: Element) {
        val land = element.getAnnotation(Land::class.java) ?: return

        if (!hasNoParameters(element, Land::class.java)) {
            return
        }

        mStates.add(land.value)
        if (mLandMap.containsKey(land.value)) {
            KenKenPaHelper().logParsingError(mProcessingEnv, element, Land::class.java, IllegalArgumentException(
                    "state %s has multiple @Land".format(land.value)))
        }
        mLandMap.put(land.value, element)
    }

    private fun hasNoParameters(element: Element, aClass: Class<out Annotation>): Boolean {
        val executableElement = element as ExecutableElement
        val parameters = executableElement.parameters
        if (parameters.size > 0) {
            KenKenPaHelper().logParsingError(mProcessingEnv, element, aClass,
                    IllegalArgumentException("%s has too many parameter(s): %s".format(element.getSimpleName(), parameters.size)))
            return false
        }
        return true
    }

    private fun addTakeOffAnnotationInfo(element: Element) {
        val takeOff = element.getAnnotation(TakeOff::class.java) ?: return

        if (!hasNoParameters(element, TakeOff::class.java)) {
            return
        }

        mStates.add(takeOff.value)
        // if (mTakeOffMap.containsKey(takeOff.value)) {
        // KenKenPaHelper.logParsingError(mProcessingEnv, element,
        // TakeOff.class, new IllegalArgumentException(String.format(
        // "state %s has multiple @TakeOff", takeOff.value)));
        // }
        //
        // if (!hasFromState(takeOff.value)) {
        // KenKenPaHelper.logParsingError(mProcessingEnv, element,
        // TakeOff.class, new IllegalArgumentException(String.format(
        // "No state %s set Hop(from)", takeOff.value)));
        // }
        mTakeOffMap.put(takeOff.value, element)
    }

}
