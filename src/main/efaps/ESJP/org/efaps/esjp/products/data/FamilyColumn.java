/*
 * Copyright 2003 - 2016 The eFaps Team
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

package org.efaps.esjp.products.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.data.IColumnValidate;
import org.efaps.esjp.data.IColumnValue;
import org.efaps.esjp.data.jaxb.AbstractDef;
import org.efaps.esjp.data.jaxb.AttrDef;
import org.efaps.esjp.products.ProductFamily;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("7641c1ce-0e4f-45e2-8fba-d5b0620eceb3")
@EFapsApplication("eFapsApp-Products")
public class FamilyColumn
    extends AbstractCommon
    implements IColumnValue, IColumnValidate
{
    /**
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FamilyColumn.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final Parameter _parameter,
                           final AttrDef _attrDef,
                           final Map<String, Integer> _headers,
                           final String[] _value,
                           final Integer _idx)
        throws EFapsException
    {
        String ret = null;
        final String column = _attrDef.getProperty("Column");
        if (column != null) {
            final String family = _value[_headers.get(column)].trim();
            final Instance famInst = ProductFamily.getFamilyInstance4Code(_parameter, family);
            if (famInst != null && famInst.isValid()) {
                ret = String.valueOf(famInst.getId());
            }
        } else {
            FamilyColumn.LOG.error("Missing property 'Column'.");
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean validate(final Parameter _parameter,
                            final AbstractDef _def,
                            final Map<String, Integer> _headers,
                            final String[] _value,
                            final Integer _idx)
        throws EFapsException
    {
        Boolean ret;
        if (_def instanceof AttrDef) {
            final String column = ((AttrDef) _def).getProperty("Column");
            if (column != null) {
                final String family = _value[_headers.get(column)].trim();
                final Instance famInst = ProductFamily.getFamilyInstance4Code(_parameter, family);
                if (famInst != null && famInst.isValid()) {
                    FamilyColumn.LOG.debug("Row: {} - {}", _idx, famInst);
                    ret = true;
                } else {
                    FamilyColumn.LOG.warn("no Instance found in Row: {} - {}", _idx, _def);
                    ret = false;
                }
            } else {
                FamilyColumn.LOG.error("Missing property 'Column'.");
                ret = false;
            }
        } else {
            FamilyColumn.LOG.error("Validation only works for AttrDef.");
            ret = false;
        }
        return ret;
    }

    @Override
    public Collection<String> getColumnNames(final Parameter _parameter,
                                             final AbstractDef _def)
    {
        final List<String> ret = new ArrayList<>();
        final String column = ((AttrDef) _def).getProperty("Column");
        ret.add(column);
        return ret;
    }
}
