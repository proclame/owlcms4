/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.LazyCrudListener;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.team.TeamSelectionTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.IAthleteEditing;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireLogin;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ResultsContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "preparation/teams", layout = OwlcmsLayout.class)
public class TeamSelectionContent extends BaseContent
        implements OwlcmsContent, RequireLogin, IAthleteEditing {

	public static final String TITLE = "TeamMembership.Title";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamSelectionContent.class);

	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}
	protected FlexLayout topBar;
	protected ComboBox<Group> topBarGroupSelect;
	// private boolean teamFilterRecusion;
	private List<AgeDivision> adItems;
	private AgeDivision ageDivision;
	private String ageGroupPrefix;
	private OwlcmsCrudGrid<TeamTreeItem> crudGrid;
	private Group currentGroup;
	private Button download;
	private Anchor finalPackage;

//	private DecimalFormat floatFormat;
	// private ComboBox<Category> categoryFilter;
	private ComboBox<Gender> genderFilter;
	private OwlcmsLayout routerLayout;
	private ComboBox<AgeDivision> topBarAgeDivisionSelect;
	// private ComboBox<String> teamFilter;
	private ComboBox<String> topBarAgeGroupPrefixSelect;
	private JXLSCompetitionBook xlsWriter;

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public TeamSelectionContent() {
		super();
		OwlcmsFactory.waitDBInitialized();
	}

	@Override
	public void closeDialog() {
		crudGrid.getCrudLayout().hideForm();
		crudGrid.getGrid().asSingleSelect().clear();
	}

	/**
	 * Create the top bar.
	 *
	 * Note: the top bar is created before the content.
	 *
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 *
	 * @param topBar
	 */
	@Override
	public FlexLayout createMenuArea() {
		topBar = new FlexLayout();
		xlsWriter = new JXLSCompetitionBook(true, UI.getCurrent());
		StreamResource href = new StreamResource(TITLE + "Report" + ".xls", () -> xlsWriter.createInputStream());
		finalPackage = new Anchor(href, "");
		finalPackage.getStyle().set("margin-left", "1em");
		download = new Button(getTranslation(TITLE + ".Report"), new Icon(VaadinIcon.DOWNLOAD_ALT));

		topBarAgeGroupPrefixSelect = new ComboBox<>();
		topBarAgeGroupPrefixSelect.setPlaceholder(getTranslation("AgeGroup"));

		topBarAgeGroupPrefixSelect.setEnabled(false);
		topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		topBarAgeGroupPrefixSelect.setValue(null);
		topBarAgeGroupPrefixSelect.setWidth("8em");
		topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		topBarAgeGroupPrefixSelect.getStyle().set("margin-left", "1em");
		setAgeGroupPrefixSelectionListener();

		topBarAgeDivisionSelect = new ComboBox<>();
		topBarAgeDivisionSelect.setPlaceholder(getTranslation("AgeDivision"));
		adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		topBarAgeDivisionSelect.setItems(adItems);
		topBarAgeDivisionSelect.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		topBarAgeDivisionSelect.setClearButtonVisible(true);
		topBarAgeDivisionSelect.setWidth("8em");
		topBarAgeDivisionSelect.getStyle().set("margin-left", "1em");
		setAgeDivisionSelectionListener();

		finalPackage.add(download);
		HorizontalLayout buttons = new HorizontalLayout(finalPackage);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar.getStyle().set("flex", "100 1");
		topBar.removeAll();
		topBar.add(topBarAgeDivisionSelect, topBarAgeGroupPrefixSelect);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		return topBar;
	}

	/**
	 * Get the content of the crudGrid. Invoked by refreshGrid. Not currently used because we are using instead a
	 * TreeGrid and a LazyCrudListener<TeamTreeItem>()
	 *
	 * @see TreeDataProvider
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	public Collection<TeamTreeItem> findAll() {
		List<TeamTreeItem> allTeams = new ArrayList<>();

		TeamSelectionTreeData teamTreeData = new TeamSelectionTreeData(getAgeGroupPrefix(), getAgeDivision(),
		        getGenderFilter().getValue(), Ranking.SNATCH_CJ_TOTAL, false);
		Map<Gender, List<TeamTreeItem>> teamsByGender = teamTreeData.getTeamItemsByGender();

		List<TeamTreeItem> mensTeams = teamsByGender.get(Gender.M);
		if (mensTeams != null) {
			allTeams.addAll(mensTeams);
		}
		List<TeamTreeItem> womensTeams = teamsByGender.get(Gender.F);
		if (womensTeams != null) {
			allTeams.addAll(womensTeams);
		}

		return allTeams;
	}

	public AgeDivision getAgeDivision() {
		return ageDivision;
	}

	public String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	@Override
	public OwlcmsCrudGrid<?> getEditingGrid() {
		return crudGrid;
	}

	public ComboBox<Gender> getGenderFilter() {
		return genderFilter;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation(TITLE);
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	public void refresh() {
		crudGrid.refreshGrid();
	}

	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	/**
	 * Parse the http query parameters
	 *
	 * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
	 *
	 * @param event     Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query
	 *                  parameters instead.
	 *
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
	 *      java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		setLocation(event.getLocation());
		setLocationUI(event.getUI());
		QueryParameters queryParameters = getLocation().getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
		HashMap<String, List<String>> params = new HashMap<>(parametersMap);

		logger.debug("parsing query parameters");
		List<String> groupNames = params.get("group");
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			currentGroup = GroupRepository.findByName(groupName);
		} else {
			currentGroup = null;
		}
		if (currentGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(currentGroup.getName())));
		} else {
			params.remove("group");
		}
		params.remove("fop");

		// change the URL to reflect group
		event.getUI().getPage().getHistory().replaceState(null,
		        new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(
		        location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	protected HorizontalLayout announcerButtons(FlexLayout topBar2) {
		return null;
	}

	/**
	 * Gets the crudGrid.
	 *
	 * @return the crudGrid crudGrid
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createCrudGrid(app.owlcms.nui.crudui.OwlcmsCrudFormFactory)
	 */
	protected OwlcmsCrudGrid<TeamTreeItem> createCrudGrid(OwlcmsCrudFormFactory<TeamTreeItem> crudFormFactory) {
		TreeGrid<TeamTreeItem> grid = new TreeGrid<>();
		grid.addHierarchyColumn(TeamTreeItem::formatName).setHeader(Translator.translate("Name")).setWidth("32ch");
		grid.addColumn(TeamTreeItem::getCategory).setHeader(Translator.translate("Category"))
		        .setTextAlign(ColumnTextAlign.CENTER);

		ComponentRenderer<Component, TeamTreeItem> warningRenderer = new ComponentRenderer<>(p -> {
			if (p.isWarning()) {
				NativeLabel label = new NativeLabel("\u26a0");
				return label;
			} else {
				return new NativeLabel();
			}
		});
		grid.addColumn(warningRenderer).setHeader(Translator.translate("Competition.TooManyPerCat"))
		        .setTextAlign(ColumnTextAlign.CENTER);

		ComponentRenderer<Component, TeamTreeItem> membershipRenderer = new ComponentRenderer<>(p -> {
			if (p.getAthlete() == null) {
				long nb = p.getTeamMembers().stream().filter(pa -> pa.isTeamMember()).count();
				NativeLabel label = new NativeLabel(
				        nb > Competition.getCurrent().getMaxTeamSize() ? nb + "\u26a0" : nb + "");
				p.setMembershipLabel(label);
				return label;
			} else {
				// checkbox to avoid entering in the form
				Checkbox activeBox = new Checkbox("Name");
				activeBox.setLabel(null);
				activeBox.getElement().getThemeList().set("secondary", true);
				activeBox.setValue(p.isTeamMember() != null ? p.isTeamMember() : false);
				activeBox.addValueChangeListener(click -> {
					Boolean value = click.getValue();
					activeBox.setValue(value);
					JPAService.runInTransaction(em -> toggleTeamMember(p, value, em));
				});
				// prevent grid row selection from triggering
				activeBox.getElement().addEventListener("click", ignore -> {}).addEventData("event.stopPropagation()");
				return activeBox;
			}
		});
		grid.addColumn(membershipRenderer).setHeader(Translator.translate("TeamMembership.TeamMember"))
		        .setSortable(true).setTextAlign(ColumnTextAlign.CENTER);

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(TeamTreeItem.class);
		OwlcmsCrudGrid<TeamTreeItem> crudGrid = new OwlcmsCrudGrid<>(TeamTreeItem.class, gridLayout,
		        crudFormFactory, grid) {
			@SuppressWarnings("deprecation")
			@Override
			public void refreshGrid() {
				if (topBar == null) {
					return;
				}
				// logger.debug("refreshing grid {} {} {}",getAgeGroupPrefix(),
				// getAgeDivision(),
				// genderFilter.getValue());
				TeamSelectionTreeData teamTreeData = new TeamSelectionTreeData(getAgeGroupPrefix(), getAgeDivision(),
				        genderFilter.getValue(), Ranking.SNATCH_CJ_TOTAL, false);
				grid.setDataProvider(new TreeDataProvider<>(teamTreeData));
			}

			@Override
			protected void initToolbar() {
			}

			@Override
			protected void updateButtonClicked() {
				TeamTreeItem item = grid.asSingleSelect().getValue();
				if (item.getAthlete() == null) {
					return;
				}

				TeamTreeItem domainObject = grid.asSingleSelect().getValue();
				showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
					try {
						TeamTreeItem updatedObject = updateOperation.perform(domainObject);
						grid.asSingleSelect().clear();
						refreshGrid();
						grid.asSingleSelect().setValue(updatedObject);
					} catch (IllegalArgumentException ignore) {
					} catch (CrudOperationException e1) {
						refreshGrid();
					} catch (Exception e2) {
						refreshGrid();
						throw e2;
					}
				});
			}

			@Override
			protected void updateButtons() {
			}
		};

		defineFilters(crudGrid);
		defineContent(crudGrid);
		crudGrid.setClickRowToUpdate(true);
		crudGrid.setWidth("100ch");
		return crudGrid;
	}

	/**
	 * We do not control the groups on other screens/displays
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(OwlcmsCrudGrid<TeamTreeItem> crudGrid2) {
//        if (teamFilter == null) {
//            teamFilter = new ComboBox<>();
//            teamFilter.setPlaceholder(getTranslation("Team"));
//            teamFilter.setClearButtonVisible(true);
//            teamFilter.addValueChangeListener(e -> {
//                if (!teamFilterRecusion) return;
//                crudGrid2.refreshGrid();
//            });
//            teamFilter.setWidth("10em");
//        }
//        crudGrid2.getCrudLayout().addFilterComponent(teamFilter);

		if (genderFilter == null) {
			genderFilter = new ComboBox<>();
			genderFilter.setPlaceholder(getTranslation("Gender"));
			genderFilter.setItems(Gender.M, Gender.F);
			genderFilter.setItemLabelGenerator((i) -> {
				return i == Gender.M ? getTranslation("Gender.Men") : getTranslation("Gender.Women");
			});
			genderFilter.setClearButtonVisible(true);
			genderFilter.addValueChangeListener(e -> {
				crudGrid2.refreshGrid();
			});
			genderFilter.setWidth("10em");
		}
		crudGrid2.getCrudLayout().addFilterComponent(genderFilter);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsCrudFormFactory<TeamTreeItem> crudFormFactory = new TeamItemSelectionFormFactory(TeamTreeItem.class,
		        this);
		crudGrid = createCrudGrid(crudFormFactory);
		fillHW(crudGrid, this);
		AgeDivision value = (adItems != null && adItems.size() > 0) ? adItems.get(0) : null;
		setAgeDivision(value);
		topBarAgeDivisionSelect.setValue(value);
	}

	private int countTeamMembers(List<TeamTreeItem> teamMembers) {
		return (int) teamMembers.stream().filter(m -> m.isTeamMember()).count();
	}

	private void defineContent(OwlcmsCrudGrid<TeamTreeItem> crudGrid) {
		crudGrid.setCrudListener(new LazyCrudListener<TeamTreeItem>() {
			@Override
			public TeamTreeItem add(TeamTreeItem user) {
				AthleteRepository.save(user.getAthlete());
				return user;
			}

			@Override
			public void delete(TeamTreeItem user) {
				AthleteRepository.delete(user.getAthlete());
			}

			@Override
			public DataProvider<TeamTreeItem, ?> getDataProvider() {
				return new TreeDataProvider<>(
				        new TeamSelectionTreeData(getAgeGroupPrefix(), getAgeDivision(), getGenderFilter().getValue(),
				                Ranking.SNATCH_CJ_TOTAL, false));
			}

			@Override
			public TeamTreeItem update(TeamTreeItem user) {
				AthleteRepository.save(user.getAthlete());
				return user;
			}
		});
	}

	private void setAgeDivisionSelectionListener() {
		topBarAgeDivisionSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			AgeDivision ageDivisionValue = e.getValue();
			setAgeDivision(ageDivisionValue);
			// logger.debug("ageDivisionSelectionListener {}",ageDivisionValue);
			if (ageDivisionValue == null) {
				topBarAgeGroupPrefixSelect.setValue(null);
				topBarAgeGroupPrefixSelect.setItems(new ArrayList<String>());
				topBarAgeGroupPrefixSelect.setEnabled(false);
				topBarAgeGroupPrefixSelect.setValue(null);
				crudGrid.refreshGrid();
				return;
			}

			List<String> ageDivisionAgeGroupPrefixes;
			ageDivisionAgeGroupPrefixes = AgeGroupRepository.findActiveAndUsed(ageDivisionValue);

			topBarAgeGroupPrefixSelect.setItems(ageDivisionAgeGroupPrefixes);
			boolean notEmpty = ageDivisionAgeGroupPrefixes.size() > 0;
			topBarAgeGroupPrefixSelect.setEnabled(notEmpty);
			String first = (notEmpty && ageDivisionValue == AgeDivision.IWF) ? ageDivisionAgeGroupPrefixes.get(0)
			        : null;
			// logger.debug("ad {} ag {} first {} select {}", ageDivisionValue,
			// ageDivisionAgeGroupPrefixes, first,
			// topBarAgeGroupPrefixSelect);

			xlsWriter.setAgeDivision(ageDivisionValue);
			finalPackage.getElement().setAttribute("download",
			        "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
			                : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

			String value = notEmpty ? first : null;
			// logger.debug("setting prefix to {}", value);
			topBarAgeGroupPrefixSelect.setValue(value);
			updateFilters();

			if (crudGrid != null && value == null) {
				// if prefix is already null, does not refresh. Force it.
				crudGrid.refreshGrid();
			}

		});
	}

	private void setAgeGroupPrefixSelectionListener() {
		topBarAgeGroupPrefixSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			String prefix = e.getValue();
			setAgeGroupPrefix(prefix);

			// logger.debug("ageGroupPrefixSelectionListener {}",prefix);
			// updateFilters(getAgeDivision(), getAgeGroupPrefix());
			xlsWriter.setAgeGroupPrefix(ageGroupPrefix);
			finalPackage.getElement().setAttribute("download",
			        "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
			                : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

			if (crudGrid != null) {
				crudGrid.refreshGrid();
			}

		});
	}

	private Object toggleTeamMember(TeamTreeItem tti, Boolean value, EntityManager em) {
		logger.info("{} {} as team member for category {}", value ? "setting" : "removing",
		        tti.getAthlete().getShortName(), tti.getAthlete().getCategory().getTranslatedName());
		Participation _getOriginalParticipation = ((PAthlete) tti.getAthlete())._getOriginalParticipation();
		boolean member = Boolean.TRUE.equals(value);
		_getOriginalParticipation.setTeamMember(member);
		tti.setTeamMember(member);
		em.merge(_getOriginalParticipation);
		TeamTreeItem parent = tti.getParent();
		List<TeamTreeItem> teamMembers = tti.getParent().getTeamMembers();
		parent.getMembershipLabel().setText("" + (teamMembers != null ? countTeamMembers(teamMembers) : 0));
		return null;
	}

	private void updateFilters() {
	}

}
