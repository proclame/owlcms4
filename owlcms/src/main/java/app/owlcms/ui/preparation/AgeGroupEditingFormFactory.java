/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Arrays;
import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import app.owlcms.components.fields.CategoryListField;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.i18n.Translator;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.shared.CustomFormFactory;

@SuppressWarnings("serial")
public class AgeGroupEditingFormFactory
        extends OwlcmsCrudFormFactory<AgeGroup>
        implements CustomFormFactory<AgeGroup> {

    private AgeGroupContent origin;

    AgeGroupEditingFormFactory(Class<AgeGroup> domainType, AgeGroupContent origin) {
        super(domainType);
        this.origin = origin;
    }

    @Override
    public AgeGroup add(AgeGroup AgeGroup) {
        AgeGroupRepository.save(AgeGroup);
        return AgeGroup;
    }

    @Override
    public Binder<AgeGroup> buildBinder(CrudOperation operation, AgeGroup domainObject) {
        return super.buildBinder(operation, domainObject);
    }

    @Override
    public String buildCaption(CrudOperation operation, AgeGroup domainObject) {
        String name = domainObject.getName();
        if (name == null || name.isEmpty()) {
            return Translator.translate("AgeGroup");
        } else {
            return Translator.translate("AgeGroup")+" "+domainObject.getName();
        }
    }

    @Override
    public Component buildFooter(CrudOperation operation, AgeGroup domainObject,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter, Button... buttons) {
        return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
                deleteButtonClickListener, false, buttons);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, AgeGroup aFromDb, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth("550px");
//        if (this.responsiveSteps != null) {
//            formLayout.setResponsiveSteps(this.responsiveSteps);
//        }

        binder = buildBinder(null, aFromDb);
        String message = Translator.translate("AgeFormat");

        TextField minAgeField = new TextField();
        formLayout.addFormItem(minAgeField, Translator.translate("MinimumAge"));
        binder.forField(minAgeField)
                .withConverter(new StringToIntegerConverter(message))
                .withValidator(new IntegerRangeValidator(message, 0, 999))
                .bind(AgeGroup::getMinAge, AgeGroup::setMinAge);

        TextField maxAgeField = new TextField();
        formLayout.addFormItem(maxAgeField, Translator.translate("MaximumAge"));
        binder.forField(maxAgeField)
                .withConverter(new StringToIntegerConverter(message))
                .withValidator(new IntegerRangeValidator(message, 0, 999))
                .bind(AgeGroup::getMaxAge, AgeGroup::setMaxAge);

        ComboBox<Gender> genderField = new ComboBox<>();
        genderField.setDataProvider(new ListDataProvider<Gender>(Arrays.asList(Gender.values())));
        binder.forField(genderField).bind(AgeGroup::getGender, AgeGroup::setGender);
        formLayout.addFormItem(genderField, Translator.translate("Gender"));
        
        CategoryListField catField = new CategoryListField(aFromDb);
        
        binder.forField(catField).bind(AgeGroup::getCategories,AgeGroup::setCategories);
        formLayout.addFormItem(catField, Translator.translate("BodyWeightCategories"));
        
        binder.readBean(aFromDb);

        Component footerLayout = this.buildFooter(operation, aFromDb, cancelButtonClickListener,
                updateButtonClickListener, deleteButtonClickListener, false);

        VerticalLayout mainLayout = new VerticalLayout(formLayout, footerLayout);
        mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        return mainLayout;
    }

    @Override
    public Button buildOperationButton(CrudOperation operation, AgeGroup domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
        return super.buildOperationButton(operation, domainObject, gridCallBackAction);
    }

    @Override
    public TextField defineOperationTrigger(CrudOperation operation, AgeGroup domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        return super.defineOperationTrigger(operation, domainObject, action);
    }

    @Override
    public void delete(AgeGroup ageGroup) {
        AgeGroupRepository.delete(ageGroup);
    }

    @Override
    public Collection<AgeGroup> findAll() {
        // will not be called, handled by the grid.
        return null;
    }

    @Override
    public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
        return super.setErrorLabel(validationStatus, showErrorOnFields);
    }

    @Override
    public AgeGroup update(AgeGroup ageGroup) {
        AgeGroup saved = AgeGroupRepository.save(ageGroup);
        origin.closeDialog();
        return saved;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        binder.forField(field);
        super.bindField(field, property, propertyType);
    }

}