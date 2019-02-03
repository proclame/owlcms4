package org.ledocte.owlcms.crudui;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.impl.WindowBasedCrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;

@SuppressWarnings("serial")
public class OwlcmsCrudLayout extends WindowBasedCrudLayout {

	private boolean disableNextShowForm = false;

	public OwlcmsCrudLayout(Class aClass) {
        getContent().setPadding(false);
        getContent().setMargin(false);
        getContent().add(mainLayout);

        mainLayout.setSizeFull();
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        setSizeFull();

        headerLayout.setVisible(false);
        headerLayout.setSpacing(true);
        headerLayout.setMargin(true);

        toolbarLayout.setVisible(false);
        // FIXME find out Lumo style equivalent
        // toolbarLayout.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        headerLayout.add(toolbarLayout);

        filterLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        filterLayout.setVisible(false);
        filterLayout.setSpacing(true);
        headerLayout.add(filterLayout);

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize(".9em");
        filterLayout.add(icon);

        mainComponentLayout.setWidth("100%");
        mainComponentLayout.setHeight(null);
        mainComponentLayout.setMargin(false);
        mainComponentLayout.setPadding(false);
        mainLayout.add(mainComponentLayout);
        mainLayout.expand(mainComponentLayout);
        mainComponentLayout.setId("mainComponentLayout");
        mainLayout.setId("mainLayout");

        setWindowCaption(CrudOperation.ADD, "Add "+ aClass.getSimpleName());
        setWindowCaption(CrudOperation.UPDATE, "Update "+ aClass.getSimpleName());
        setWindowCaption(CrudOperation.DELETE, "Are you sure you want to delete this item?");
        
    }

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.layout.impl.WindowBasedCrudLayout#showForm(org.vaadin.crudui.crud.CrudOperation, com.vaadin.flow.component.Component)
	 */
	@Override
	public void showForm(CrudOperation operation, Component form) {
		if (disableNextShowForm) {
//			System.err.println("ignoring open");
		} else {
//			System.err.println("showing form");
			super.showForm(operation, form);
		}
		disableNextShowForm(false);
	}
	
	public void disableNextShowForm(boolean isDisabled) {
		disableNextShowForm = isDisabled;
	}
	
}
