package com.github.shiraji.kenkenpa.compiler.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.github.shiraji.kenkenpa.annotations.Hop;
import com.github.shiraji.kenkenpa.annotations.Hops;
import com.github.shiraji.kenkenpa.annotations.KenKenPa;
import com.github.shiraji.kenkenpa.annotations.Land;
import com.github.shiraji.kenkenpa.annotations.TakeOff;
import com.github.shiraji.kenkenpa.compiler.helper.KenKenPaHelper;

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
			addLandAnnotationInfo(enclosedElement);
			addTakeOffAnnotationInfo(enclosedElement);
		}

		validateLandAnnotation();
	}

	private void validateLandAnnotation() {
		// hasToState(mLandMap.keySet());

		for (Map.Entry<String, Element> entry : mLandMap.entrySet()) {

			if (!mDefaultState.equals(entry.getKey())) {
				KenKenPaHelper.logParsingError(mProcessingEnv, entry.getValue(), Land.class, new IllegalArgumentException(String.format("No state %s set Hop(to)", entry.getKey())));
			}
		}
	}

	private boolean hasToState(Set<String> landStates) {
		List<Hop> hops = new LinkedList<>();
		for (Map.Entry<Element, Annotation> entry : mHopMap.entrySet()) {
			hops.addAll(createHopListFromEntry(entry.getValue()));
		}

		Set<String> hopToStates = new HashSet<>();
		for (Hop hop : hops) {
			hopToStates.add(hop.to());
		}
		hopToStates.add(mDefaultState);
		return hopToStates.containsAll(landStates);
	}

	private List<Hop> createHopListFromEntry(Annotation annotation) {
		List<Hop> hopList = new LinkedList<>();
		if (annotation instanceof Hop) {
			hopList.add((Hop) annotation);
		} else if (annotation instanceof Hops) {
			Hops hops = (Hops) annotation;
			hopList.addAll(Arrays.asList(hops.value()));
		}
		return hopList;
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
				KenKenPaHelper.logParsingError(mProcessingEnv, element2, Hops.class, new IllegalArgumentException("'from' must not have the same state."));
			}
			hopsFrom.add(hop.from());
			mStates.add(hop.from());
			mStates.add(hop.to());
		}
	}

	private void addLandAnnotationInfo(Element element) {
		Land land = element.getAnnotation(Land.class);
		if (land == null) {
			return;
		}

		if (!hasNoParameters(element, Land.class)) {
			return;
		}

		mStates.add(land.value());
		if (mLandMap.containsKey(land.value())) {
			KenKenPaHelper.logParsingError(mProcessingEnv, element, Land.class, new IllegalArgumentException(String.format("state %s has multiple @Land", land.value())));
		}
		mLandMap.put(land.value(), element);
	}

	private boolean hasNoParameters(Element element, Class<? extends Annotation> aClass) {
		ExecutableElement executableElement = (ExecutableElement) element;
		List<? extends VariableElement> parameters = executableElement.getParameters();
		if (parameters.size() > 0) {
			KenKenPaHelper.logParsingError(mProcessingEnv, element, aClass,
					new IllegalArgumentException(String.format("%s has too many parameter(s): %s", element.getSimpleName(), parameters.size())));
			return false;
		}
		return true;
	}

	private void addTakeOffAnnotationInfo(Element element) {
		TakeOff takeOff = element.getAnnotation(TakeOff.class);
		if (takeOff == null) {
			return;
		}

		if (!hasNoParameters(element, TakeOff.class)) {
			return;
		}

		mStates.add(takeOff.value());
		// if (mTakeOffMap.containsKey(takeOff.value())) {
		// KenKenPaHelper.logParsingError(mProcessingEnv, element,
		// TakeOff.class, new IllegalArgumentException(String.format(
		// "state %s has multiple @TakeOff", takeOff.value())));
		// }
		//
		// if (!hasFromState(takeOff.value())) {
		// KenKenPaHelper.logParsingError(mProcessingEnv, element,
		// TakeOff.class, new IllegalArgumentException(String.format(
		// "No state %s set Hop(from)", takeOff.value())));
		// }
		mTakeOffMap.put(takeOff.value(), element);
	}

}
