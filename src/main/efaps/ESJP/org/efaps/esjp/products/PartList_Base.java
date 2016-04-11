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


package org.efaps.esjp.products;


import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("3d18c18a-2a7a-4ae6-850e-19eadb6b0c0d")
@EFapsApplication("eFapsApp-Products")
public abstract class PartList_Base
{

    /**
     * Gets the part list instances.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the part list instances
     * @throws EFapsException on error
     */
    public Return getPartListInstances(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductSalesPartList);
        final InstanceQuery query = queryBldr.getQuery();
        ret.put(ReturnValues.VALUES, query.execute());
        return ret;
    }

    /**
     * Gets the part list quantity field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the part list quantity field value
     * @throws EFapsException on error
     */
    public Return getPartListQuantityFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final Instance instance = fieldValue.getInstance();
        final PartListInst partList = new PartListInst(instance);
        ret.put(ReturnValues.VALUES, partList.getStockQuantity());
        return ret;
    }
}
