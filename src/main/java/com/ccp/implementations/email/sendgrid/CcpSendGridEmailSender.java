package com.ccp.implementations.email.sendgrid;

import com.ccp.dependency.injection.CcpInstanceProvider;
import com.ccp.especifications.email.CcpEmailSender;

/**
 * Provedor de DI que expõe {@code SendGridEmailSender} como implementação de {@code CcpEmailSender}.
 */
public class CcpSendGridEmailSender implements CcpInstanceProvider<CcpEmailSender> {

	public CcpEmailSender getInstance() {
		return new SendGridEmailSender();
	}

}
