/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.URLUtils;

/**
 * Utility methods for creating the texts and buttons on navigation pages.
 *
 * @author Jean-François Lamy
 *
 */
public interface NavigationPage extends ContentWrapping {

	/**
	 * Create a paragraph with HTML inside.
	 *
	 * @param intro
	 * @param text
	 * @return the formatted paragraph
	 */
	public default Paragraph addP(HasComponents intro, String text) {
		Paragraph paragraph = new Paragraph();
		paragraph.getElement().setProperty("innerHTML", text);
		paragraph.getStyle().set("margin-bottom", "0");
		paragraph.getStyle().set("margin-top", "0");
		paragraph.getStyle().set("width", "75%");
		intro.add(paragraph);
		return paragraph;
	}

	public default UnorderedList addUL(HasComponents intro, String... bullets) {
		UnorderedList ul = new UnorderedList();
		for (String b : bullets) {
			ListItem item = new ListItem(b);
			item.getElement().setProperty("innerHTML", b);
			ul.add(item);
		}
		ul.getStyle().set("margin-bottom", "0");
		ul.getStyle().set("margin-top", "0");
		intro.add(ul);
		return ul;
	}

	public default void doGroup(String label, FlexibleGridLayout grid1, VerticalLayout wrapper) {
		VerticalLayout content1 = new VerticalLayout();
		content1.setSpacing(false);
		content1.setPadding(true);
		NativeLabel label2 = new NativeLabel(label);
		label2.getStyle().set("font-weight", "bold");
		content1.add(label2);
		label2.getStyle().set("margin-bottom", "0.8ex");
		content1.getStyle().set("margin-bottom", "-2ex");
		fillH(content1, wrapper);
		grid1.setPadding(false);
		grid1.getStyle().set("padding-top", "0.5em");
		grid1.getStyle().set("padding-left", "1em");
		grid1.getStyle().set("padding-bottom", "1em");
		fillH(grid1, wrapper);
	}

	public default void doGroup(String label, VerticalLayout intro, FlexibleGridLayout grid1, VerticalLayout wrapper) {
		VerticalLayout content1 = new VerticalLayout();
		content1.setSpacing(false);
		content1.setPadding(true);
		NativeLabel label2 = new NativeLabel(label);
		label2.getStyle().set("margin-bottom", "0.8ex");
		label2.getStyle().set("font-weight", "bold");
		content1.add(label2);
		intro.setPadding(false);
		intro.getStyle().set("padding-left", "0");
		content1.add(intro);
		content1.getStyle().set("margin-bottom", "-1ex");
		fillH(content1, wrapper);
		grid1.setPadding(false);
		grid1.getStyle().set("padding-top", "0.5em");
		grid1.getStyle().set("padding-left", "1em");
		grid1.getStyle().set("padding-bottom", "1em");
		fillH(grid1, wrapper);
	}

	public default <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<T> targetClass,
	        String parameter) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		String name = fop == null ? "" : "_" + fop.getName();
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass, parameter) + "','"
		        + targetClass.getSimpleName() + name + "')";
	}
	
	public default <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<?> class1,
	        String parameter, QueryParameters qp) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		String name = fop == null ? "" : "_" + fop.getName();
		return "window.open('" + URLUtils.getUrlFromTargetClass(class1, parameter, qp) + "','"
		        + class1.getSimpleName() + name + "')";
	}


	public default <T extends Component> String getWindowOpenerFromClassNoParam(Class<T> targetClass) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		String name = fop == null ? "" : "_" + fop.getName();
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "','"
		        + targetClass.getSimpleName() + name + "')";
	}

	public default <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass,
	        String label) {
		return openInNewTab(targetClass, label, null);
	}

	public default <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass,
	        String label, String parameter) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
		return button;
	}
	
	public default <T extends Component & HasUrlParameter<String>> Button openInNewTabQueryParameters(Class<T> targetClass,
	        String label, String queryParameters) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, null, QueryParameters.fromString(queryParameters)));
		return button;
	}

	public default <T extends Component> Button openInNewTabNoParam(Class<T> targetClass,
	        String label) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClassNoParam(targetClass));
		return button;
	}
	
	public default <T extends Component & HasUrlParameter<String>> void doOpenInNewTab(Class<?> class1, String label, String parameter, QueryParameters qp) {
		String windowOpenerFromClass = getWindowOpenerFromClass(class1, parameter, qp);
		UI.getCurrent().getPage().executeJs(windowOpenerFromClass);
	}

}