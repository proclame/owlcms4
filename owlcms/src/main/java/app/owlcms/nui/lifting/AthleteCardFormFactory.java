/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.Collection;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.github.appreciated.css.grid.GridLayoutComponent.ColumnAlign;
import com.github.appreciated.css.grid.GridLayoutComponent.RowAlign;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Int;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.GridLayout;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.ClassList;

import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.RuleViolationException.Rule15_20Violated;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import app.owlcms.nui.shared.IAthleteEditing;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class AthleteCardFormFactory extends OwlcmsCrudFormFactory<Athlete> implements CustomFormFactory<Athlete> {

    private static final String CHECKBOX_MARGIN = "0em";

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteCardFormFactory.class);

    private static final int HEADER = 1;
    private static final int AUTOMATIC = HEADER + 1;
    private static final int DECLARATION = AUTOMATIC + 1;
    private static final int CHANGE1 = DECLARATION + 1;
    private static final int CHANGE2 = CHANGE1 + 1;
    private static final int ACTUAL = CHANGE2 + 1;
    private static final int SCORE = ACTUAL + 1;

    private static final int LEFT = 1;
    private static final int SNATCH1 = LEFT + 1;
    private static final int SNATCH2 = SNATCH1 + 1;
    private static final int SNATCH3 = SNATCH2 + 1;
    private static final int CJ1 = SNATCH3 + 1;
    private static final int CJ2 = CJ1 + 1;
    private static final int CJ3 = CJ2 + 1;
    private TextField cj2AutomaticProgression;
    private TextField cj3AutomaticProgression;
    private TextField cj1ActualLift;
    private TextField cj2ActualLift;
    private TextField cj3ActualLift;

    private TextField snatch2AutomaticProgression;
    private TextField snatch3AutomaticProgression;
    private TextField snatch1ActualLift;
    private TextField snatch2ActualLift;
    private TextField snatch3ActualLift;

    /**
     * text field array to facilitate setting focus when form is opened
     */
    TextField[][] textfields = new TextField[SCORE][CJ3];

    private Athlete editedAthlete;
    private Athlete originalAthlete;

    private IAthleteEditing origin;

    private GridLayout gridLayout;

    private Boolean updatingResults;

    private Checkbox ignoreErrorsCheckbox;

    private Boolean liftResultChanged;

    public AthleteCardFormFactory(Class<Athlete> domainType, IAthleteEditing origin) {
        super(domainType);
        this.origin = origin;
    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#add(app.owlcms.data.athlete.Athlete)
     */
    @Override
    public Athlete add(Athlete athlete) {
        AthleteRepository.save(athlete);
        return athlete;
    }

    /**
     * Add bean-level validations
     *
     * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object)
     */
    @Override
    public Binder<Athlete> buildBinder(CrudOperation operation, Athlete doNotUse) {
        // we do *not* use the athlete provided by the grid selection. For some reason,
        // the grid selector returns a copy on the first invocation, instead of the
        // underlying object.
        // we use editedAthlete, which this form retrieves from the underlying data
        // source
        Athlete editedAthlete2 = getEditedAthlete();
        binder = super.buildBinder(operation, editedAthlete2);
        logger.trace("athlete from grid={} edited={}", doNotUse, editedAthlete2);
        setValidationStatusHandler(true);
        return binder;

    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#buildCaption(org.vaadin.crudui.crud.CrudOperation,
     *      app.owlcms.data.athlete.Athlete)
     */
    @Override
    public String buildCaption(CrudOperation operation, final Athlete aFromDb) {
        // If getFullId() is null, caller will build a defaut caption, so this is safe
        Integer startNumber = aFromDb.getStartNumber();
        return (startNumber != null ? "[" + startNumber + "] " : "") + aFromDb.getFullId();
    }

    /**
     * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener,
     *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.ComponentEventListener)
     */
    @Override
    public Component buildFooter(CrudOperation operation, Athlete unused,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> unused2, ComponentEventListener<ClickEvent<Button>> unused3,
            boolean shortcutEnter,
            Button... buttons) {
        ComponentEventListener<ClickEvent<Button>> postOperationCallBack = (e) -> {
        };
        Button operationButton = null;
        if (operation == CrudOperation.UPDATE) {
            operationButton = buildOperationButton(CrudOperation.UPDATE, originalAthlete, postOperationCallBack);
        } else if (operation == CrudOperation.ADD) {
            operationButton = buildOperationButton(CrudOperation.ADD, originalAthlete, postOperationCallBack);
        }
        Button deleteButton = buildDeleteButton(CrudOperation.DELETE, originalAthlete, null);
        Component withdrawButtons = buildWithdrawButtons();
        Checkbox forcedCurrentCheckbox = buildForcedCurrentCheckbox();
        Checkbox validateEntries = buildIgnoreErrorsCheckbox();
        Checkbox allowResultsEditing = buildAllowResultsEditingCheckbox();
        Button cancelButton = buildCancelButton(cancelButtonClickListener);

        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setWidth("100%");
        footerLayout.setSpacing(true);
        footerLayout.setPadding(false);

        if (deleteButton != null && operation != CrudOperation.ADD) {
            footerLayout.add(deleteButton);
        }
        if (withdrawButtons != null && operation != CrudOperation.ADD) {
            footerLayout.add(withdrawButtons);
        }
        VerticalLayout vl = new VerticalLayout();
        if (forcedCurrentCheckbox != null && operation != CrudOperation.ADD) {
            vl.setSizeUndefined();
            vl.setPadding(false);
            vl.setMargin(false);
            vl.add(validateEntries);
            vl.add(allowResultsEditing);
            vl.add(forcedCurrentCheckbox);
            footerLayout.add(vl);
        }

        Label spacer = new Label();

        footerLayout.add(spacer, operationTrigger);

        if (cancelButton != null) {
            footerLayout.add(cancelButton);
        }

        if (operationButton != null) {
            footerLayout.add(operationButton);
            if (operation == CrudOperation.UPDATE && shortcutEnter) {
                ShortcutRegistration reg = operationButton.addClickShortcut(Key.ENTER);
                reg.allowBrowserDefault();
            }
        }
        footerLayout.setFlexGrow(1.0, vl);
        return footerLayout;
    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#buildNewForm(org.vaadin.crudui.crud.CrudOperation,
     *      app.owlcms.data.athlete.Athlete, boolean, com.vaadin.flow.component.ComponentEventListener,
     *      com.vaadin.flow.component.ComponentEventListener, com.vaadin.flow.component.ComponentEventListener,
     *      com.vaadin.flow.component.button.Button)
     */
    @Override
    public Component buildNewForm(CrudOperation operation, Athlete aFromList, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {
        this.setLiftResultChanged(false);

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();
        if (this.responsiveSteps != null) {
            formLayout.setResponsiveSteps(this.responsiveSteps);
        }
        setUpdatingResults(origin instanceof AnnouncerContent);

        gridLayout = setupGrid();
        errorLabel = new Label();
        HorizontalLayout labelWrapper = new HorizontalLayout(errorLabel);
        labelWrapper.addClassName("errorMessage");
        labelWrapper.setWidthFull();
        labelWrapper.setJustifyContentMode(JustifyContentMode.CENTER);

        // We use a copy so that if the user cancels, we still have the original object.
        // This allows us to use the rich validation methods coded in the Athlete class
        // as opposed to tedious validations on the form fields using getValue().
        setEditedAthlete(new Athlete());

        // don't trust the athlete received as parameter. Fetch from database in case the athlete was edited
        // on some other screen.
        if (aFromList instanceof PAthlete) {
            originalAthlete = ((PAthlete) aFromList)._getAthlete();
        } else {
            originalAthlete = aFromList;
        }
        Athlete aFromDb = AthleteRepository.findById(aFromList.getId());
        Athlete.conditionalCopy(getEditedAthlete(), aFromDb, true);
        getEditedAthlete().setValidation(false); // turn off validation in the Athlete setters; binder will call
                                                 // the validation routines explicitly

        logger.trace("aFromDb = {} {}", System.identityHashCode(aFromList), aFromList);
        logger.trace("originalAthlete = {} {}", System.identityHashCode(originalAthlete), originalAthlete);
        logger.trace("editedAthlete = {} {}", System.identityHashCode(getEditedAthlete()), getEditedAthlete());

        bindGridFields(operation);

        Component footerLayout = this.buildFooter(operation, getEditedAthlete(), cancelButtonClickListener,
                updateButtonClickListener, deleteButtonClickListener, true);

        VerticalLayout mainLayout = new VerticalLayout(formLayout, gridLayout, errorLabel, footerLayout);
        gridLayout.setSizeFull();
        mainLayout.setFlexGrow(1, gridLayout);
        mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        return mainLayout;
    }

    /**
     * Special version because we use setBean instead of readBean
     *
     * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildOperationButton(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener)
     */
    @Override
    public Button buildOperationButton(CrudOperation operation, Athlete domainObject,
            ComponentEventListener<ClickEvent<Button>> callBack) {
        if (callBack == null) {
            return null;
        }
        Button button = doBuildButton(operation);
        operationTrigger = defineOperationTrigger(operation, domainObject, callBack);

        ComponentEventListener<ClickEvent<Button>> listener = event -> {
            // force value to be written to underlying bean. Crude Workaround for keyboard
            // shortcut
            // which does not process last field input when ENTER key is used.
            operationTrigger.focus();
        };

        button.addClickListener(listener);
        return button;
    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#defineOperationTrigger(org.vaadin.crudui.crud.CrudOperation,
     *      app.owlcms.data.athlete.Athlete, com.vaadin.flow.component.ComponentEventListener)
     */
    @Override
    public TextField defineOperationTrigger(CrudOperation operation, Athlete domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        TextField operationTrigger = new TextField();
        operationTrigger.setReadOnly(true);
        operationTrigger.setTabIndex(-1);
        operationTrigger.addFocusListener((f) -> {
            boolean valid = isValid();
            boolean ignoreErrors = isIgnoreErrors();
            if (valid || ignoreErrors) {
                // logger.debug("updating {} {}", valid, ignoreErrors);
                doUpdate();
            } else {
                // logger.debug("not updating {} {}", valid, ignoreErrors);
            }
        });
        // field must visible and added to the layout for focus() to work, so we hide it
        // brutally. operationTrigger is placed in the footer.

        operationTrigger.getStyle().set("z-index", "-10");
        operationTrigger.setWidth("1px");
        return operationTrigger;
    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#delete(app.owlcms.data.athlete.Athlete)
     */
    @Override
    public void delete(Athlete notUsed) {
        AthleteRepository.delete(originalAthlete);
    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        throw new UnsupportedOperationException(); // should be called on the grid
    }

    /**
     * Force correcting one error at a time
     *
     * @param validationStatus
     * @return
     */
    @Override
    public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean updateFieldStatus) {
        String simpleName = this.getClass().getSimpleName();
        logger.debug("{} validations", simpleName);
        StringBuilder sb = new StringBuilder();

        boolean hasErrors = validationStatus.getFieldValidationErrors().size() > 0;
        validationStatus.getBinder().getFields().forEach(f -> {
            ClassList fieldClasses = ((Component) f).getElement().getClassList();
            fieldClasses.set("error", false);
            f.setReadOnly(hasErrors);
        });
        TextField field = null;
        for (BindingValidationStatus<?> ve : validationStatus.getFieldValidationErrors()) {
            field = (TextField) ve.getField();
            ClassList fieldClasses = field.getElement().getClassList();
            fieldClasses.clear();
            fieldClasses.set("error", true);
            field.setReadOnly(false);
            field.setAutoselect(true);
            field.focus();
            if (sb.length() > 0) {
                sb.append("; ");
            }
            String message = ve.getMessage().orElse(field.getTranslation("Error"));
            sb.append(message);
        }
        for (ValidationResult ve : validationStatus.getBeanValidationErrors()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            String message = ve.getErrorMessage();
            // logger.debug("bean message: {}",message);
            sb.append(message);
        }
        doSetErrorLabel(simpleName, sb);
        if (!hasErrors) {
            resetReadOnlyFields();
        } else if (field != null) {
            field.focus();
        }
        return hasErrors;
    }

    /**
     * @see app.owlcms.nui.shared.CustomFormFactory#update(app.owlcms.data.athlete.Athlete)
     */
    @Override
    public Athlete update(Athlete athleteFromDb) {
        doUpdate();
        return originalAthlete;
    }

    private void adjustResultsFields(boolean readOnly) {
        setLiftResultStyle(snatch1ActualLift);
        setLiftResultStyle(snatch2ActualLift);
        setLiftResultStyle(snatch3ActualLift);
        setLiftResultStyle(cj1ActualLift);
        setLiftResultStyle(cj2ActualLift);
        setLiftResultStyle(cj3ActualLift);
    }

    private void atRowAndColumn(GridLayout gridLayout, Component component, int row, int column) {
        atRowAndColumn(gridLayout, component, row, column, RowAlign.CENTER, ColumnAlign.CENTER);
    }

    private void atRowAndColumn(GridLayout gridLayout, Component component, int row, int column, RowAlign ra,
            ColumnAlign ca) {
        gridLayout.add(component);
        gridLayout.setRowAndColumn(component, new Int(row), new Int(column), new Int(row), new Int(column));
        gridLayout.setRowAlign(component, ra);
        gridLayout.setColumnAlign(component, ca);
        component.getElement().getStyle().set("width", "6em");
        if (component instanceof TextField) {
            TextField textField = (TextField) component;
            textfields[row - 1][column - 1] = textField;
        }

    }

    /**
     * @param operation
     * @param operation
     * @param gridLayout
     */
    private void bindGridFields(CrudOperation operation) {
        binder = buildBinder(null, getEditedAthlete());

        TextField snatch1Declaration = createPositiveWeightField(DECLARATION, SNATCH1);
        binder.forField(snatch1Declaration)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1Declaration(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch1Declaration, snatch1ActualLift);
                })
                .bind(Athlete::getSnatch1Declaration, Athlete::setSnatch1Declaration);
        atRowAndColumn(gridLayout, snatch1Declaration, DECLARATION, SNATCH1);

        TextField snatch1Change1 = createPositiveWeightField(CHANGE1, SNATCH1);
        binder.forField(snatch1Change1)
                .withValidator(ValidationUtils
                        .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1Change1(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch1Change1, snatch1ActualLift);
                })
                .bind(Athlete::getSnatch1Change1, Athlete::setSnatch1Change1);
        atRowAndColumn(gridLayout, snatch1Change1, CHANGE1, SNATCH1);

        TextField snatch1Change2 = createPositiveWeightField(CHANGE2, SNATCH1);
        binder.forField(snatch1Change2)
                .withValidator(ValidationUtils
                        .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1Change2(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch1Change2, snatch1ActualLift);
                })
                .bind(Athlete::getSnatch1Change2, Athlete::setSnatch1Change2);
        atRowAndColumn(gridLayout, snatch1Change2, CHANGE2, SNATCH1);

        snatch1ActualLift = createActualWeightField(ACTUAL, SNATCH1);
        binder.forField(snatch1ActualLift)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateSnatch1ActualLift(v)))
                .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
                .withValidationStatusHandler(status -> setActualLiftStyle(status))
                .bind(Athlete::getSnatch1ActualLift, Athlete::setSnatch1ActualLift);
        atRowAndColumn(gridLayout, snatch1ActualLift, ACTUAL, SNATCH1);

        snatch2AutomaticProgression = new TextField();
        snatch2AutomaticProgression.setReadOnly(true);
        snatch2AutomaticProgression.setTabIndex(-1);
        binder.forField(snatch2AutomaticProgression).bind(Athlete::getSnatch2AutomaticProgression,
                Athlete::setSnatch2AutomaticProgression);
        atRowAndColumn(gridLayout, snatch2AutomaticProgression, AUTOMATIC, SNATCH2);

        TextField snatch2Declaration = createPositiveWeightField(DECLARATION, SNATCH2);
        binder.forField(snatch2Declaration)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2Declaration(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch2Declaration, snatch2ActualLift);
                })
                .bind(Athlete::getSnatch2Declaration, Athlete::setSnatch2Declaration);
        atRowAndColumn(gridLayout, snatch2Declaration, DECLARATION, SNATCH2);

        TextField snatch2Change1 = createPositiveWeightField(CHANGE1, SNATCH2);
        binder.forField(snatch2Change1)
                .withValidator(ValidationUtils
                        .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2Change1(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch2Change1, snatch2ActualLift);
                })
                .bind(Athlete::getSnatch2Change1, Athlete::setSnatch2Change1);
        atRowAndColumn(gridLayout, snatch2Change1, CHANGE1, SNATCH2);

        TextField snatch2Change2 = createPositiveWeightField(CHANGE2, SNATCH2);
        binder.forField(snatch2Change2)
                .withValidator(ValidationUtils
                        .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2Change2(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch2Change2, snatch2ActualLift);
                })
                .bind(Athlete::getSnatch2Change2, Athlete::setSnatch2Change2);
        atRowAndColumn(gridLayout, snatch2Change2, CHANGE2, SNATCH2);

        snatch2ActualLift = createActualWeightField(ACTUAL, SNATCH2);
        binder.forField(snatch2ActualLift)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateSnatch2ActualLift(v)))
                .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
                .withValidationStatusHandler(status -> setActualLiftStyle(status))
                .bind(Athlete::getSnatch2ActualLift, Athlete::setSnatch2ActualLift);
        atRowAndColumn(gridLayout, snatch2ActualLift, ACTUAL, SNATCH2);

        snatch3AutomaticProgression = new TextField();
        snatch3AutomaticProgression.setReadOnly(true);
        snatch3AutomaticProgression.setTabIndex(-1);
        binder.forField(snatch3AutomaticProgression).bind(Athlete::getSnatch3AutomaticProgression,
                Athlete::setSnatch3AutomaticProgression);
        atRowAndColumn(gridLayout, snatch3AutomaticProgression, AUTOMATIC, SNATCH3);

        TextField snatch3Declaration = createPositiveWeightField(DECLARATION, SNATCH3);
        binder.forField(snatch3Declaration)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3Declaration(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch3Declaration, snatch3ActualLift);
                }).bind(Athlete::getSnatch3Declaration, Athlete::setSnatch3Declaration);
        atRowAndColumn(gridLayout, snatch3Declaration, DECLARATION, SNATCH3);

        TextField snatch3Change1 = createPositiveWeightField(CHANGE1, SNATCH3);
        binder.forField(snatch3Change1)
                .withValidator(ValidationUtils
                        .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3Change1(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch3Change1, snatch3ActualLift);
                }).bind(Athlete::getSnatch3Change1, Athlete::setSnatch3Change1);
        atRowAndColumn(gridLayout, snatch3Change1, CHANGE1, SNATCH3);

        TextField snatch3Change2 = createPositiveWeightField(CHANGE2, SNATCH3);
        binder.forField(snatch3Change2)
                .withValidator(ValidationUtils
                        .checkUsingException(v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3Change2(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, snatch3Change2, snatch3ActualLift);
                })
                .bind(Athlete::getSnatch3Change2, Athlete::setSnatch3Change2);
        atRowAndColumn(gridLayout, snatch3Change2, CHANGE2, SNATCH3);

        snatch3ActualLift = createActualWeightField(ACTUAL, SNATCH3);
        binder.forField(snatch3ActualLift)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateSnatch3ActualLift(v)))
                .withValidationStatusHandler(status -> setActualLiftStyle(status))
                .bind(Athlete::getSnatch3ActualLift, Athlete::setSnatch3ActualLift);
        atRowAndColumn(gridLayout, snatch3ActualLift, ACTUAL, SNATCH3);

        TextField cj1Declaration = createPositiveWeightField(DECLARATION, CJ1);
        binder.forField(cj1Declaration)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1Declaration(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj1Declaration, cj1ActualLift);
                })
                .bind(Athlete::getCleanJerk1Declaration, Athlete::setCleanJerk1Declaration);
        atRowAndColumn(gridLayout, cj1Declaration, DECLARATION, CJ1);

        TextField cj1Change1 = createPositiveWeightField(CHANGE1, CJ1);
        binder.forField(cj1Change1)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1Change1(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj1Change1, cj1ActualLift);
                }).bind(Athlete::getCleanJerk1Change1, Athlete::setCleanJerk1Change1);
        atRowAndColumn(gridLayout, cj1Change1, CHANGE1, CJ1);

        TextField cj1Change2 = createPositiveWeightField(CHANGE2, CJ1);
        binder.forField(cj1Change2)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1Change2(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj1Change2, cj1ActualLift);
                }).bind(Athlete::getCleanJerk1Change2, Athlete::setCleanJerk1Change2);
        atRowAndColumn(gridLayout, cj1Change2, CHANGE2, CJ1);

        cj1ActualLift = createActualWeightField(ACTUAL, CJ1);
        binder.forField(cj1ActualLift)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk1ActualLift(v)))
                .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
                .withValidationStatusHandler(status -> setActualLiftStyle(status))
                .bind(Athlete::getCleanJerk1ActualLift, Athlete::setCleanJerk1ActualLift);
        atRowAndColumn(gridLayout, cj1ActualLift, ACTUAL, CJ1);

        cj2AutomaticProgression = new TextField();
        cj2AutomaticProgression.setReadOnly(true);
        cj2AutomaticProgression.setTabIndex(-1);
        binder.forField(cj2AutomaticProgression).bind(Athlete::getCleanJerk2AutomaticProgression,
                Athlete::setCleanJerk2AutomaticProgression);
        atRowAndColumn(gridLayout, cj2AutomaticProgression, AUTOMATIC, CJ2);

        TextField cj2Declaration = createPositiveWeightField(DECLARATION, CJ2);
        binder.forField(cj2Declaration)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2Declaration(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj2Declaration, cj2ActualLift);
                }).bind(Athlete::getCleanJerk2Declaration, Athlete::setCleanJerk2Declaration);
        atRowAndColumn(gridLayout, cj2Declaration, DECLARATION, CJ2);

        TextField cj2Change1 = createPositiveWeightField(CHANGE1, CJ2);
        binder.forField(cj2Change1)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2Change1(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj2Change1, cj2ActualLift);
                }).bind(Athlete::getCleanJerk2Change1, Athlete::setCleanJerk2Change1);
        atRowAndColumn(gridLayout, cj2Change1, CHANGE1, CJ2);

        TextField cj2Change2 = createPositiveWeightField(CHANGE2, CJ2);
        binder.forField(cj2Change2)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2Change2(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj2Change2, cj2ActualLift);
                }).bind(Athlete::getCleanJerk2Change2, Athlete::setCleanJerk2Change2);
        atRowAndColumn(gridLayout, cj2Change2, CHANGE2, CJ2);

        cj2ActualLift = createActualWeightField(ACTUAL, CJ2);
        binder.forField(cj2ActualLift)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk2ActualLift(v)))
                .withValidator(ValidationUtils.checkUsingException(v -> setAutomaticProgressions(getEditedAthlete())))
                .withValidationStatusHandler(status -> setActualLiftStyle(status))
                .bind(Athlete::getCleanJerk2ActualLift, Athlete::setCleanJerk2ActualLift);
        atRowAndColumn(gridLayout, cj2ActualLift, ACTUAL, CJ2);

        cj3AutomaticProgression = new TextField();
        cj3AutomaticProgression.setReadOnly(true);
        cj3AutomaticProgression.setTabIndex(-1);
        binder.forField(cj3AutomaticProgression).bind(Athlete::getCleanJerk3AutomaticProgression,
                Athlete::setCleanJerk3AutomaticProgression);
        atRowAndColumn(gridLayout, cj3AutomaticProgression, AUTOMATIC, CJ3);

        TextField cj3Declaration = createPositiveWeightField(DECLARATION, CJ3);
        binder.forField(cj3Declaration)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3Declaration(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj3Declaration, cj3ActualLift);
                })
                .bind(Athlete::getCleanJerk3Declaration, Athlete::setCleanJerk3Declaration);
        atRowAndColumn(gridLayout, cj3Declaration, DECLARATION, CJ3);

        TextField cj3Change1 = createPositiveWeightField(CHANGE1, CJ3);
        binder.forField(cj3Change1)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3Change1(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj3Change1, cj3ActualLift);
                })
                .bind(Athlete::getCleanJerk3Change1, Athlete::setCleanJerk3Change1);
        atRowAndColumn(gridLayout, cj3Change1, CHANGE1, CJ3);

        TextField cj3Change2 = createPositiveWeightField(CHANGE2, CJ3);
        binder.forField(cj3Change2)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3Change2(v)))
                .withValidationStatusHandler(status -> {
                    checkWithdrawal(status, cj3Change2, cj3ActualLift);
                })
                .bind(Athlete::getCleanJerk3Change2, Athlete::setCleanJerk3Change2);
        atRowAndColumn(gridLayout, cj3Change2, CHANGE2, CJ3);

        cj3ActualLift = createActualWeightField(ACTUAL, CJ3);
        binder.forField(cj3ActualLift)
                .withValidator(
                        ValidationUtils.checkUsingException(
                                v -> isIgnoreErrors() || getEditedAthlete().validateCleanJerk3ActualLift(v)))
                .withValidationStatusHandler(status -> setActualLiftStyle(status))
                .bind(Athlete::getCleanJerk3ActualLift, Athlete::setCleanJerk3ActualLift);
        atRowAndColumn(gridLayout, cj3ActualLift, ACTUAL, CJ3);

        if (Competition.getCurrent().isCustomScore()) {
            TextField custom = createPositiveWeightField(SCORE, CJ3);
            binder.forField(custom)
                    .withConverter(new StringToDoubleConverter(0.0D, Translator.translate("NumberExpected")))
                    .bind(Athlete::getCustomScoreComputed, Athlete::setCustomScore);
            atRowAndColumn(gridLayout, custom, SCORE, CJ3);
        }

        // use setBean so that changes are immediately reflected to the working copy
        // otherwise the changes are only visible in the fields, and the validation
        // routines in the
        // Athlete class don't work
        binder.setBean(getEditedAthlete());

        for (int i = SNATCH1; i <= CJ3; i++) {
            textfields[ACTUAL - 1][i - 1].addValueChangeListener(e -> setLiftResultChanged(true));
        }

        try {
            getEditedAthlete().validateStartingTotalsRule(
                    snatch1Declaration.getValue(),
                    snatch1Change1.getValue(),
                    snatch1Change2.getValue(),
                    cj1Declaration.getValue(),
                    cj1Change1.getValue(),
                    cj1Change2.getValue());
        } catch (Rule15_20Violated rv) {
            doSetErrorLabel("", new StringBuilder(rv.getLocalizedMessage()));
            // allow changing all cj values
            resetReadOnlyFields();
        }

        if (!isUpdatingResults()) {
            adjustResultsFields(true);
        }
        setFocus(getEditedAthlete());
    }

    private Checkbox buildAllowResultsEditingCheckbox() {
        Checkbox checkbox = new Checkbox(Translator.translate("AllowResultsEditing"));
        checkbox.getStyle().set("margin-left", CHECKBOX_MARGIN);
        binder.forField(checkbox).bind((a) -> isUpdatingResults(), (a, readonly) -> {
            setUpdatingResults(readonly);
            adjustResultsFields(readonly);
        });
        return checkbox;
    }

    private Checkbox buildForcedCurrentCheckbox() {
        Checkbox checkbox = new Checkbox(Translator.translate("ForcedAsCurrent"));
        checkbox.getStyle().set("margin-left", CHECKBOX_MARGIN);
        binder.forField(checkbox).bind(Athlete::isForcedAsCurrent, Athlete::setForcedAsCurrent);
        return checkbox;
    }

    private Checkbox buildIgnoreErrorsCheckbox() {
        ignoreErrorsCheckbox = new Checkbox(Translator.translate("RuleViolation.ignoreErrors"), e -> {
            if (BooleanUtils.isTrue(isIgnoreErrors())) {
                logger./**/warn/**/("{}!Errors ignored - checkbox override for athlete {}",
                        OwlcmsSession.getFop().getLoggingName(), this.getEditedAthlete().getShortName());
                binder.validate();
                binder.writeBeanAsDraft(editedAthlete, true);
            }

        });
        ignoreErrorsCheckbox.getStyle().set("margin-left", CHECKBOX_MARGIN);
        return ignoreErrorsCheckbox;
    }

    private Component buildWithdrawButtons() {
        Integer attemptsDone = getEditedAthlete().getAttemptsDone();
        VerticalLayout vl = new VerticalLayout();

        Button snatchWithdrawalButton = new Button(Translator.translate("SnatchWithdrawal"),
                IronIcons.EXIT_TO_APP.create(),
                (e) -> {
                    Athlete.conditionalCopy(originalAthlete, getEditedAthlete(), true);
                    originalAthlete.withdrawFromSnatch();
                    AthleteRepository.save(originalAthlete);
                    OwlcmsSession.withFop((fop) -> {
                        fop.fopEventPost(new FOPEvent.WeightChange(this.getOrigin(), originalAthlete, true));
                    });
                    origin.closeDialog();
                });
        snatchWithdrawalButton.getElement().setAttribute("theme", "error");

        Button withdrawalButton = new Button(Translator.translate("Withdrawal"), IronIcons.EXIT_TO_APP.create(),
                (e) -> {
                    Athlete.conditionalCopy(originalAthlete, getEditedAthlete(), true);
                    originalAthlete.withdraw();
                    AthleteRepository.save(originalAthlete);
                    OwlcmsSession.withFop((fop) -> {
                        fop.fopEventPost(new FOPEvent.WeightChange(this.getOrigin(), originalAthlete, true));
                    });
                    origin.closeDialog();
                });
        withdrawalButton.getElement().setAttribute("theme", "error");
        

        if (attemptsDone < 3) {
            vl.add(snatchWithdrawalButton, withdrawalButton);
        } else {
            vl.add(withdrawalButton);
        }

        vl.setSizeUndefined();
        return vl;
    }

    private void checkWithdrawal(BindingValidationStatus<?> status, TextField change,
            TextField lift) {
        if (!status.isError()) {
            if (change.getValue() != null && change.getValue().contentEquals("0")) {
                lift.setValue("0");
                setFocus(getEditedAthlete());
            }
        }
    }

    private int computeAutomaticProgression(int value) {
        return value <= 0 ? Math.abs(value) : value + 1;
    }

    private TextField createActualWeightField(int row, int col) {
        TextField tf = new TextField();
        tf.setPattern("^[-]{0,1}\\d*$");
        tf.setPreventInvalidInput(true);
        tf.setValueChangeMode(ValueChangeMode.ON_BLUR);
        return tf;
    }

    private TextField createPositiveWeightField(int row, int col) {
        TextField tf = new TextField();
        tf.setPattern("^\\d*$");
        tf.setPreventInvalidInput(true);
        tf.setValueChangeMode(ValueChangeMode.ON_BLUR);
        return tf;
    }

    private void doSetErrorLabel(String simpleName, StringBuilder sb) {
        if (sb.length() > 0) {
            String message = sb.toString();
            logger.debug("{} setting message {}", simpleName, message);
            errorLabel.setVisible(true);
            errorLabel.getElement().setProperty("innerHTML", message);
            errorLabel.getClassNames().set("errorMessage", true);
        } else {
            logger.debug("{} setting EMPTY", simpleName);
            errorLabel.setVisible(true);
            errorLabel.getElement().setProperty("innerHTML", "&nbsp;");
            errorLabel.getClassNames().clear();
        }
    }

    /**
     * Update the original athlete so that the lifting order picks up the change.
     */
    private void doUpdate() {
        // do not overwrite results if acting as Marshal
        // marshall has the ability to edit, but must click explicitly
        Athlete.conditionalCopy(originalAthlete, getEditedAthlete(), true);
        AthleteRepository.save(originalAthlete);
        OwlcmsSession.withFop((fop) -> {
            fop.fopEventPost(new FOPEvent.WeightChange(this.getOrigin(), originalAthlete, isLiftResultChanged()));
        });
        origin.closeDialog();
    }

    private Athlete getEditedAthlete() {
        return editedAthlete;
    }

    private Object getOrigin() {
        return origin;
    }

    private boolean isIgnoreErrors() {
        return BooleanUtils.isTrue(ignoreErrorsCheckbox == null ? null : ignoreErrorsCheckbox.getValue());
    }

    private Boolean isLiftResultChanged() {
        return liftResultChanged;
    }

    private boolean isUpdatingResults() {
        return updatingResults;
    }

    private void resetReadOnlyFields() {
        // setErrorLabel undoes the readOnly status of fields
        snatch2AutomaticProgression.setReadOnly(true);
        snatch3AutomaticProgression.setReadOnly(true);
        cj2AutomaticProgression.setReadOnly(true);
        cj3AutomaticProgression.setReadOnly(true);

        snatch1ActualLift.setReadOnly(!isUpdatingResults());
        snatch2ActualLift.setReadOnly(!isUpdatingResults());
        snatch3ActualLift.setReadOnly(!isUpdatingResults());
        cj1ActualLift.setReadOnly(!isUpdatingResults());
        cj2ActualLift.setReadOnly(!isUpdatingResults());
        cj3ActualLift.setReadOnly(!isUpdatingResults());
    }

    private void setActualLiftStyle(BindingValidationStatus<?> status) throws NumberFormatException {
        TextField field = (TextField) status.getField();
        if (status.isError()) {
            field.getElement().getClassList().set("error", true);
            field.getElement().getClassList().set("good", false);
            field.getElement().getClassList().set("bad", false);
            field.focus();
        } else {
            setLiftResultStyle(field);
        }
    }

    /**
     * set the automatic progressions. This is invoked as a validator because we don't want to be called if the entered
     * value is invalid. Only the side-effect is interesting, so we return true.
     *
     * @param athlete
     * @return true always
     */
    private boolean setAutomaticProgressions(Athlete athlete) {
        int value = Athlete.zeroIfInvalid(snatch1ActualLift.getValue());
        int autoVal = computeAutomaticProgression(value);
        snatch2AutomaticProgression.setValue(Integer.toString(autoVal));
        value = Athlete.zeroIfInvalid(snatch2ActualLift.getValue());
        autoVal = computeAutomaticProgression(value);
        snatch3AutomaticProgression.setValue(Integer.toString(autoVal));

        value = Athlete.zeroIfInvalid(cj1ActualLift.getValue());
        autoVal = computeAutomaticProgression(value);
        cj2AutomaticProgression.setValue(Integer.toString(autoVal));
        value = Athlete.zeroIfInvalid(cj2ActualLift.getValue());
        autoVal = computeAutomaticProgression(value);
        cj3AutomaticProgression.setValue(Integer.toString(autoVal));

        return true;
    }

    private void setEditedAthlete(Athlete editedAthlete) {
        this.editedAthlete = editedAthlete;
    }

    private void setFocus(Athlete a) {
        int targetRow = ACTUAL + 1;
        int targetCol = CJ3 + 1;

        // figure out whether we are searching for snatch or CJ
        int rightCol;
        int leftCol;

        if (a.getAttemptsDone() >= 3) {
            rightCol = CJ3;
            leftCol = CJ1;
        } else {
            rightCol = SNATCH3;
            leftCol = SNATCH1;
        }

        // remember location of last empty cell, going backwards
        search: for (int col = rightCol; col >= leftCol; col--) {
            for (int row = ACTUAL; row > AUTOMATIC; row--) {
                boolean empty = textfields[row - 1][col - 1].isEmpty();
                if (empty) {
                    targetRow = row - 1;
                    targetCol = col - 1;
                } else {
                    // don't go back past first non-empty (leave holes)
                    break search;
                }
            }
        }

        if (targetCol <= CJ3 && targetRow <= ACTUAL) {
            // a suitable empty cell was found, set focus
            textfields[targetRow][targetCol].setAutofocus(true);
            textfields[targetRow][targetCol].setAutoselect(true);
            textfields[targetRow][targetCol].focus();
        }
    }

    private void setLiftResultChanged(Boolean liftResultChanged) {
        // logger.debug("*** liftResultChanged {}", liftResultChanged);
        this.liftResultChanged = liftResultChanged;
    }

    private void setLiftResultStyle(TextField field) {
        String value = field.getValue();
        boolean empty = value == null || value.trim().isEmpty();
        if (empty) {
            field.getElement().getClassList().clear();
            if (!isUpdatingResults()) {
                field.getElement().getClassList().add("readonly");
                field.setReadOnly(true);
            } else {
                field.setReadOnly(false);
            }
        } else if (value != null && value.equals("-")) {
            field.getElement().getClassList().clear();
            field.getElement().getClassList().add("bad");
            if (!isUpdatingResults()) {
                field.getElement().getClassList().add("readonly");
                field.setReadOnly(true);
            } else {
                field.setReadOnly(false);
            }
        } else {
            int intValue = Integer.parseInt(value);
            field.getElement().getClassList().clear();
            field.getElement().getClassList().add((intValue <= 0 ? "bad" : "good"));
            if (!isUpdatingResults()) {
                field.getElement().getClassList().add("readonly");
                field.setReadOnly(true);
            } else {
                field.setReadOnly(false);
            }
        }
    }

    private void setUpdatingResults(Boolean b) {
        updatingResults = b;
    }

    private GridLayout setupGrid() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.setTemplateRows(new Repeat(ACTUAL, new Flex(1)));
        gridLayout.setTemplateColumns(new Repeat(CJ3, new Flex(1)));
        gridLayout.setGap(new Length("0.8ex"), new Length("1.2ex"));

        // column headers
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Snatch1")), HEADER, SNATCH1, RowAlign.CENTER,
                ColumnAlign.CENTER);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Snatch2")), HEADER, SNATCH2, RowAlign.CENTER,
                ColumnAlign.CENTER);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Snatch3")), HEADER, SNATCH3, RowAlign.CENTER,
                ColumnAlign.CENTER);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("C_and_J_1")), HEADER, CJ1, RowAlign.CENTER,
                ColumnAlign.CENTER);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("C_and_J_2")), HEADER, CJ2, RowAlign.CENTER,
                ColumnAlign.CENTER);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("C_and_J_3")), HEADER, CJ3, RowAlign.CENTER,
                ColumnAlign.CENTER);

        // row headings
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("AutomaticProgression")), AUTOMATIC, LEFT,
                RowAlign.CENTER, ColumnAlign.END);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Declaration")), DECLARATION, LEFT,
                RowAlign.CENTER, ColumnAlign.END);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Change_1")), CHANGE1, LEFT, RowAlign.CENTER,
                ColumnAlign.END);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Change_2")), CHANGE2, LEFT, RowAlign.CENTER,
                ColumnAlign.END);
        atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("WeightLifted")), ACTUAL, LEFT, RowAlign.CENTER,
                ColumnAlign.END);
        if (Competition.getCurrent().isCustomScore()) {
            atRowAndColumn(gridLayout, new Label(gridLayout.getTranslation("Score")), SCORE, LEFT, RowAlign.CENTER,
                    ColumnAlign.END);
        }

        return gridLayout;
    }
}