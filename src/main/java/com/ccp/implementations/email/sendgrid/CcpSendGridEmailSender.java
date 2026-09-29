package com.ccp.implementations.email.sendgrid;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.email.CcpEmailSender;

/**
 * DI provider that exposes {@code SendGridEmailSender} as the {@code CcpEmailSender} implementation.
 */
public class CcpSendGridEmailSender implements CcpInstanceProvider<CcpEmailSender> {

	public CcpEmailSender getInstance() {
		SendGridEmailSender sendGridEmailSender = new SendGridEmailSender();
		return sendGridEmailSender;
	}

}
