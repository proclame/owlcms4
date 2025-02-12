/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.top;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class TopSinclair
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("topsinclair-template")
@JsModule("./components/TopSinclair.js")

public class TopSinclair extends AbstractTop {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TopSinclair.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	JsonArray cattempts;
	JsonArray sattempts;
	private List<Athlete> sortedMen;
	private List<Athlete> sortedWomen;
	private double topManSinclair;
	private double topWomanSinclair;
	private EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public TopSinclair() {
		super();
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			// just update the display
			doUpdate(fop.getCurAthlete(), null);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		doBreak(e);
	}

	public void doUpdate(Competition competition) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());

		// create copies because we want to change the list
		AthleteSorter.TopSinclair topSinclair;
		List<Athlete> sortedMen2 = new ArrayList<>(competition.getGlobalSinclairRanking(Gender.M));
		topSinclair = (AthleteSorter.topSinclair(sortedMen2, 25));
		setSortedMen(topSinclair.topAthletes);
		topManSinclair = topSinclair.best;

		List<Athlete> sortedWomen2 = new ArrayList<>(competition.getGlobalSinclairRanking(Gender.F));
		topSinclair = (AthleteSorter.topSinclair(sortedWomen2, 25));
		setSortedWomen(topSinclair.topAthletes);
		topWomanSinclair = topSinclair.best;

		updateBottom();
	}

	public void getAthleteJson(Athlete a, JsonObject ja, Gender g, int needed) {
		String category;
		category = a.getCategory() != null ? a.getCategory().getTranslatedName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a);
		ja.put("sattempts", sattempts);
		ja.put("cattempts", cattempts);
		ja.put("total", formatInt(a.getTotal()));
		ja.put("bw", String.format("%.2f", a.getBodyWeight()));
		ja.put("sinclair", String.format("%.3f", (a.getAttemptsDone() <= 3 ? a.getSinclairForDelta()
		        : a.getSinclair())));
		ja.put("needed", formatInt(needed));
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setSilenced(boolean)
	 */
	@Override
	public void setSilenced(boolean silent) {
		// no-op, silenced by definition
	}

	@Override
	public void setVideo(boolean video) {
	}

	@Subscribe
	public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
		computeTop(e);
	}

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			doUpdate(competition);
		});
	}

	@Override
	public void uiLog(UIEvent e) {
		if (e == null) {
			uiEventLogger.debug("### {} {}", this.getClass().getSimpleName(), LoggerUtils.whereFrom());
		} else {
			uiEventLogger.debug("### {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			        LoggerUtils.whereFrom());
		}
	}

	@Override
	protected void doEmpty() {
		logger.trace("doEmpty");
		getElement().setProperty("hidden", true);
	}

	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
		logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			if (a != null) {
				getElement().setProperty("fullName", getTranslation("Scoreboard.TopSinclair"));
				updateBottom();
			}
		});
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are formatted.
	 *
	 * @param a
	 * @return json string with nested attempts values
	 */
	protected void getAttemptsJson(Athlete a) {
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		Integer liftOrderRank = x.getLiftOrderRank();
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = (notDone ? " blink" : "");

			jri.put("goodBadClassName", "veryNarrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (!trim.isEmpty()) {
						if (trim.contentEquals("-") || trim.contentEquals("0")) {
							jri.put("goodBadClassName", "veryNarrow fail");
							jri.put("stringValue", "-");
						} else {
							boolean failed = stringValue != null && stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "veryNarrow fail" : "veryNarrow good");
							jri.put("stringValue", formatKg(stringValue));
						}
					}
					break;
				default:
					if (stringValue != null && !trim.isEmpty()) {
						String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
						        : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
						jri.put("goodBadClassName", "veryNarrow request");
						if (notDone) {
							jri.put("className", highlight);
						}
						jri.put("stringValue", stringValue);
					}
					break;
				}
			}

			if (ix < 3) {
				sattempts.set(ix, jri);
			} else {
				cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.debug("onAttach start");
		checkVideo(Config.getCurrent().getParamStylesDir() + "/video/top.css", this);
		setWide(false);
		setTranslationMap();
		for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
			// we listen on all the uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		}
		Competition competition = Competition.getCurrent();
		doUpdate(competition);
	}

	@Override
	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		this.getElement().setPropertyJson("t", translations);
	}

	private void computeTop(UIEvent e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private JsonValue getAthletesJson(List<Athlete> list2, boolean overrideTeamWidth) {
		JsonArray jath = Json.createArray();
		int athx = 0;
		List<Athlete> list3 = list2 != null ? Collections.unmodifiableList(list2) : Collections.emptyList();
		if (overrideTeamWidth) {
			// when we are called for the second time, and there was a wide team in the top
			// section.
			// we use the wide team setting for the remaining sections.
			setWide(false);
		}

		for (Athlete a : list3) {
			JsonObject ja = Json.createObject();
			Gender curGender = a.getGender();

			int needed;
			if (curGender == Gender.F) {
				needed = (int) Math
				        .round(Math.ceil((topWomanSinclair - a.getSinclairForDelta()) / a.getSinclairFactor()));
			} else {
				needed = (int) Math
				        .round(Math.ceil((topManSinclair - a.getSinclairForDelta()) / a.getSinclairFactor()));
			}
			getAthleteJson(a, ja, curGender, needed);
			String team = a.getTeam();
			if (team != null && team.length() > Competition.SHORT_TEAM_LENGTH) {
				setWide(true);
			}
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}

	@SuppressWarnings("unused")
	private Object getOrigin() {
		return this;
	}

	private List<Athlete> getSortedMen() {
		return this.sortedMen;
	}

	private List<Athlete> getSortedWomen() {
		return this.sortedWomen;
	}

	private List<Athlete> nodups(List<Athlete> athletes) {
		// massive kludge because we have same athlete in multiple age groups
		// TODO: top sinclair can be absolute or by age group
		athletes = athletes.stream()
		        .map((p) -> p instanceof PAthlete ? ((PAthlete) p)._getAthlete() : p)
		        .collect(Collectors.toSet())
		        .stream()
		        .sorted((a, b) -> ObjectUtils.compare(b.getSinclair(), a.getSinclair()))
		        .limit(5)
		        .collect(Collectors.toList());
		return athletes;
	}

	private void setSortedMen(List<Athlete> sortedMen) {
		this.sortedMen = sortedMen;
		logger.debug("sortedMen = {} -- {}", getSortedMen(), LoggerUtils.whereFrom());
	}

	private void setSortedWomen(List<Athlete> sortedWomen) {
		this.sortedWomen = sortedWomen;
		logger.debug("sortedWomen = {} -- {}", getSortedWomen(), LoggerUtils.whereFrom());
	}

	private void setWide(boolean b) {
		getElement().setProperty("wideTeamNames", b);
	}

	private void updateBottom() {
		getElement().setProperty("fullName", getTranslation("Scoreboard.TopSinclair"));
		List<Athlete> sortedMen2 = getSortedMen();
		sortedMen2 = nodups(sortedMen2);
		this.getElement().setProperty("topSinclairMen",
		        sortedMen2 != null && sortedMen2.size() > 0 ? getTranslation("Scoreboard.TopSinclairMen") : "");
		this.getElement().setPropertyJson("sortedMen", getAthletesJson(sortedMen2, true));

		List<Athlete> sortedWomen2 = getSortedWomen();
		sortedWomen2 = nodups(sortedWomen2);
		this.getElement().setProperty("topSinclairWomen",
		        sortedWomen2 != null && sortedWomen2.size() > 0 ? getTranslation("Scoreboard.TopSinclairWomen") : "");
		this.getElement().setPropertyJson("sortedWomen", getAthletesJson(sortedWomen2, false));

		logger.debug("updateBottom {} {}", sortedWomen2, sortedMen2);
	}

}
