package com.github.shiraji.kenkenpa.compiler.model;

import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.annotations.KenKenPa;
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KenKenPaModel {
    String mDefaultState;
    Set<String> mStates = new HashSet<>();
    Map<String, Element> mLandMap = new HashMap<>();
    Map<String, Element> mTakeOffMap = new HashMap<>();
    Map<Element, Annotation> mHopMap = new HashMap<>();
    ProcessingEnvironment mProcessingEnv;

    public KenKenPaModel(ProcessingEnvironment processingEnv, TypeElement typeElement) {
        mProcessingEnv = processingEnv;
        init(typeElement);
    }

    private void init(TypeElement typeElement) {
        KenKenPa annotation = typeElement.getAnnotation(KenKenPa.class);
        mDefaultState = annotation.value();

        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            handleHopAnnotation(enclosedElement);
            handleHopsMethods(enclosedElement);
//            addLandAnnotationInfo(enclosedElement);
//            addTakeOffAnnotationInfo(enclosedElement);
        }
    }

    private void handleHopAnnotation(Element element) {
        Hop hop = element.getAnnotation(Hop.class);
        if (hop == null) {
            return;
        }
        mStates.add(hop.from());
        mStates.add(hop.to());
        mHopMap.put(element, hop);
    }

    private void handleHopsMethods(Element element2) {
        Hops hops = element2.getAnnotation(Hops.class);
        if (hops == null) {
            return;
        }
        addHopsToStates(element2, hops);
        mHopMap.put(element2, hops);
    }

    private void addHopsToStates(Element element2, Hops hops) {
        List<String> hopsFrom = new ArrayList<>();
        for (Hop hop : hops.value()) {
            if (hopsFrom.contains(hop.from())) {
                KenKenPaHelper.logParsingError(mProcessingEnv, element2, Hops.class, new IllegalArgumentException(
                        "'from' must not have the same state."));
            }
            hopsFrom.add(hop.from());
            mStates.add(hop.from());
            mStates.add(hop.to());
        }
    }

}
