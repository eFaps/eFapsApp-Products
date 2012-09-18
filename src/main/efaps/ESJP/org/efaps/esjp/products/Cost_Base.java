/*
 * Copyright 2003 - 2009 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.products;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 * 
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a7a6c100-8fce-4217-bc8e-c9066b1ab9e2")
@EFapsRevision("$Rev$")
public abstract class Cost_Base
{
    /**
     * Method to get the value for the date field "Valid until". On create mode
     * a date in ten years future is returned.
     * 
     * @param _parameter Paramter as passed from the eFaps esjp API
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return getValidUntilUI(final Parameter _parameter)
        throws EFapsException
    {
        final FieldValue fValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final DateTime value;
        if (fValue.getTargetMode().equals(TargetMode.CREATE)) {
            value = new DateTime().plusYears(10);
        } else {
            value = (DateTime) fValue.getValue();
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, value);
        return ret;
    }

    /**
     * Method is executed as a insert trigger on type "Products_ProductCost". It
     * corrects the valid until date of all other Products_ProductCost.
     * 
     * @param _parameter Paramter as passed from the eFaps esjp API
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return trigger4Insert(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Instance costInstance = _parameter.getInstance();
        final Map<String, Object[]> name2Value = new HashMap<String, Object[]>();
        for (final Entry<?, ?> entry : values.entrySet()) {
            final Attribute attr = (Attribute) entry.getKey();
            name2Value.put(attr.getName(), (Object[]) entry.getValue());
        }
        final Object from = name2Value.get("ValidFrom")[0];
        final DateTime date = new DateTime(from);
        final PrintQuery print = new PrintQuery(costInstance);
        print.addAttribute(CIProducts.ProductCost.ProductLink);
        print.execute();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCost);
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, date);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink,
                        print.getAttribute(CIProducts.ProductCost.ProductLink));
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            if (!query.getCurrentValue().equals(costInstance)) {
                final Update update = new Update(query.getCurrentValue());
                update.add(CIProducts.ProductCost.ValidUntil, date.minusDays(1));
                update.executeWithoutTrigger();
            }
        }
        return new Return();
    }
}
