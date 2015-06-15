/*
 * Copyright 2003 - 2015 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.efaps.esjp.products.variant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("dd6aae16-baa1-4ad0-8b71-2e1c64ef8812")
@EFapsApplication("eFapsApp-Products")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class VariantAttribute_Base
    implements Serializable
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XmlAttribute(name = "key")
    private String key;

    @XmlElementWrapper(name = "elements")
    @XmlElement(name = "variantElement")
    private final List<VariantElement> elements = new ArrayList<VariantElement>();

    private Iterator<VariantElement> iterator;

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getKey()
    {
        return this.key;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     */
    public void setKey(final String _name)
    {
        this.key = _name;
    }

    /**
     * Getter method for the instance variable {@link #elements}.
     *
     * @return value of instance variable {@link #elements}
     */
    public List<VariantElement> getElements()
    {
        return this.elements;
    }

    public boolean hasNext()
    {
        if (this.iterator == null) {
            this.iterator = this.elements.iterator();
        }
        return this.iterator.hasNext();
    }
}
