package app.owlcms.servlet;

import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class EmbeddedJetty extends VaadinBoot {

	private static Logger startLogger = (Logger) LoggerFactory.getLogger(EmbeddedJetty.class);
	private Runnable initConfig;
	private Runnable initData;
	private CountDownLatch latch;

	public EmbeddedJetty(CountDownLatch countDownLatch, String appName) {
		this.setLatch(countDownLatch);
		this.setAppName(appName);
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	public void run(Integer serverPort, String string) throws Exception {
		initConfig.run();
		initData.run();
		this.setPort(serverPort);
		this.run();
	}

	public EmbeddedJetty setInitConfig(Runnable initConfig) {
		this.initConfig = initConfig;
		return this;
	}

	public EmbeddedJetty setInitData(Runnable initData) {
		this.initData = initData;
		return this;
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public EmbeddedJetty setStartLogger(Logger startLogger) {
		EmbeddedJetty.startLogger = startLogger;
		return this;
	}

	@Override
	protected void onStarted() {
		getLatch().countDown();
		startLogger.info("started on port {}", this.port);
	}


}
