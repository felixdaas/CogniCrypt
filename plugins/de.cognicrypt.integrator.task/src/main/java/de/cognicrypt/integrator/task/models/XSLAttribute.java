/********************************************************************************
 * Copyright (c) 2015-2019 TU Darmstadt, Paderborn University
 * 

 * http://www.eclipse.org/legal/epl-2.0. SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package de.cognicrypt.integrator.task.models;

import de.cognicrypt.core.Constants;

public class XSLAttribute {

	private String XSLAttributeName;
	private String XSLAttributeData;

	/**
	 * For consistency, the name for XSLAttribute should come from the Array of String for the attributes associated to the {@code XSLTags} in {@link Constants} class.
	 *
	 * @param xSLAttributeName the name of the XSLAttribute.
	 * @param xSLAttributeData the value for the XSLAttribute.
	 */
	public XSLAttribute(final String xSLAttributeName, final String xSLAttributeData) {
		super();
		setXSLAttributeName(xSLAttributeName);
		setXSLAttributeData(xSLAttributeData);
	}

	/**
	 * @return the name of the XSLAttribute.
	 */
	public String getXSLAttributeName() {
		return this.XSLAttributeName;
	}

	/**
	 * @param xSLAttributeName set the name of the XSLAttribute. For consistency, the name for XSLAttribute should come from the Array of String for the attributes associated to the
	 *        {@code XSLTags} in {@link Constants} class.
	 */
	public void setXSLAttributeName(final String xSLAttributeName) {
		this.XSLAttributeName = xSLAttributeName;
	}

	/**
	 * @return the XSLAttribute value.
	 */
	public String getXSLAttributeData() {
		return this.XSLAttributeData;
	}

	/**
	 * @param xSLAttributeData set the value for the XSLAttribute.
	 */
	public void setXSLAttributeData(final String xSLAttributeData) {
		this.XSLAttributeData = xSLAttributeData;
	}

}
