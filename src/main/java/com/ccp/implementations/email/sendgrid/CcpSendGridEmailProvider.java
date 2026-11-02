package com.ccp.implementations.email.sendgrid;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.email.CcpEmailProvider;

/**
 * DI provider that exposes {@code SendGridEmailProvider} as the {@code CcpEmailProvider} implementation.
 */
public class CcpSendGridEmailProvider implements CcpInstanceProvider<CcpEmailProvider> {
	/** The single instance: the class keeps no state. */
	public final static CcpSendGridEmailProvider INSTANCE = new CcpSendGridEmailProvider();

	private CcpSendGridEmailProvider() {}

	/**
	 * Builds the SendGrid implementation of {@code CcpEmailProvider}.
	 * @return the {@code SendGridEmailProvider}
	 */
	public CcpEmailProvider getInstance() {
		SendGridEmailProvider sendGridEmailProvider = SendGridEmailProvider.INSTANCE;
		return sendGridEmailProvider;
	}

}
