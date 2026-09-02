package com.ccp.implementations.email.sendgrid;

import com.ccp.decorators.CcpJsonFieldName;

enum SendGridEmailSenderSpecialWords implements CcpJsonFieldName{
	User_Agent("User-agent")
	;
	enum JsonFieldNames implements CcpJsonFieldName{
		token, url, message, subject, sender, format, method, emails, Authorization, Accept, from, personalizations, content, type, value, to, email
	}
	private final String value;
	
	private SendGridEmailSenderSpecialWords(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
