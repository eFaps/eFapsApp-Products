/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.products.variant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f31ff812-3740-4b7c-8686-12d6dc507e42")
@EFapsApplication("eFapsApp-Products")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class VariantConf_Base
    implements Serializable
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Entries for this Taxes Collection.
     */
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "variantAttribute")
    private final List<VariantAttribute> attributes = new ArrayList<>();

    /**
     * Getter method for the instance variable {@link #entries}.
     *
     * @return value of instance variable {@link #entries}
     */
    public List<VariantAttribute> getAttributes()
    {
        return this.attributes;
    }

    /**
     * @param _key
     */
    public VariantAttribute getAttribute(final String _key)
    {
        VariantAttribute ret = null;
        for (final VariantAttribute attr : getAttributes()) {
            if (attr.getKey().equals(_key)) {
                ret = attr;
                break;
            }
        }
        return ret;
    }
}
