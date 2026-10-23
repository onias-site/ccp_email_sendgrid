package com.ccp.implementations.email.sendgrid;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.email.CcpEmailSender;

/**
 * DI provider that exposes {@code SendGridEmailSender} as the {@code CcpEmailSender} implementation.
 */
public class CcpSendGridEmailSender implements CcpInstanceProvider<CcpEmailSender> {
	/** The single instance: the class keeps no state. */
	public final static CcpSendGridEmailSender INSTANCE = new CcpSendGridEmailSender();

	private CcpSendGridEmailSender() {}

	/**
	 * Builds the SendGrid implementation of {@code CcpEmailSender}.
	 * @return a new {@code SendGridEmailSender}
	 */
	public CcpEmailSender getInstance() {
		SendGridEmailSender sendGridEmailSender = SendGridEmailSender.INSTANCE;
		return sendGridEmailSender;
	}

}
