/*
 * Copyright 2003 - 2011 The eFaps Team
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

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("82271caf-5d4e-4ec9-bb99-8cbfaff5f8a7")
@EFapsRevision("$Rev$")
public abstract class BOM_Base
{
    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return insertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (HashMap<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Attribute attr = _parameter.getInstance().getType().getAttribute("To");

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ID, (Object[]) values.get(attr));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.ProductAbstract.Dimension);
        Dimension dimension = null;
        if (multi.execute()) {
            multi.next();
            dimension = Dimension.get(multi.<Long>getAttribute(CIProducts.ProductAbstract.Dimension));
        }
        if (dimension != null) {
            final Update update = new Update(_parameter.getInstance());
            update.add(CIProducts.BOMAbstract.UoM, dimension.getBaseUoM().getId());
            update.execute();
        }
        return new Return();
    }

    public Return editBOMQuantity(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked") final Map<String, String> oidMap = (Map<String, String>) _parameter
                        .get(ParameterValues.OIDMAP4UI);
        final String[] rowKeys = _parameter.getParameterValues(EFapsKey.TABLEROW_NAME.getKey());
        final String[] quantity = _parameter.getParameterValues("quantity");
        for (int i = 0; i < rowKeys.length; i++) {
            final Instance inst = Instance.get(oidMap.get(rowKeys[i]));

            final Update update = new Update(inst);
            update.add(CIProducts.BOMAbstract.Quantity, quantity[i]);
            update.execute();
        }

        return new Return();
    }
}
