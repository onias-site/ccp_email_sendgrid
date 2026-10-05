package com.ccp.implementations.email.sendgrid;

import com.ccp.decorators.CcpJsonFieldName;


/**
 * Header names whose real value contains a character that a Java identifier cannot have: legal name in the constant,
 * real value in the constructor, exposed by {@code getValue()}.
 */
enum SendGridEmailSenderSpecialWords implements CcpJsonFieldName{
	/** The {@code User-agent} header. */
	User_Agent("User-agent")
	;
	/** Fields of the SendGrid request. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** Unused. */
		url,
		/** Unused. */
		message,
		/** The subject. */
		subject,
		/** Unused. */
		sender,
		/** Unused. */
		format,
		/** Unused. */
		method,
		/** The sender block. */
		from,
		/** The recipient blocks. */
		personalizations,
		/** The body blocks. */
		content,
		/** The recipients of a personalization. */
		to,
		/** An e-mail address. */
		email
	}
	/** The real name. */
	private final String value;
	
	/**
	 * Associates the constant with its real name.
	 * @param value the real name
	 */
	private SendGridEmailSenderSpecialWords(String value) {
		this.value = value;
	}

	/**
	 * Returns the real name.
	 * @return the real name
	 */
	public String getValue() {
		return this.value;
	}
}
