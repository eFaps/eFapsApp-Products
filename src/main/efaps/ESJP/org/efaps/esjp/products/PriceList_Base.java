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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("ccbcbc48-1c82-42f4-8e31-2eb496e862c1")
@EFapsRevision("$Rev$")
public abstract class PriceList_Base
    extends MultiPrint
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
     * Method is executed as a insert trigger on type
     * "Products_ProductPricelistAbstract" and children. It corrects the valid
     * until date of all other Instances.
     *
     * @param _parameter Parameter as passed from the eFaps esjp API
     * @return Return containing the value
     * @throws EFapsException on error
     */
    public Return trigger4Insert(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> values = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);
        final Instance costInstance = _parameter.getInstance();
        final Map<String, Object[]> name2Value = new HashMap<String, Object[]>();
        String typename = null;
        for (final Entry<?, ?> entry : values.entrySet()) {
            final Attribute attr = (Attribute) entry.getKey();
            if (typename == null) {
                typename = attr.getParent().getName();
            }
            name2Value.put(attr.getName(), (Object[]) entry.getValue());
        }
        final Object from = name2Value.get(CIProducts.ProductPricelistAbstract.ValidFrom.name)[0];
        final DateTime date = new DateTime(from);

        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        if (properties.containsKey("Type")) {
            typename = (String) properties.get("Type");
        }
        final QueryBuilder queryBldr = new QueryBuilder(Type.get(typename));
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil, date);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductPricelistAbstract.ProductAbstractLink, getProdId(costInstance));
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final List<String> updateOIDs = new ArrayList<String>();
        while (query.next()) {
            final String oid = query.getCurrentValue().getOid();
            if (!costInstance.getOid().equals(oid)) {
                updateOIDs.add(oid);
            }
        }

        for (final String oid : updateOIDs) {
            final Update update = new Update(oid);
            update.add(CIProducts.ProductPricelistAbstract.ValidUntil, date.minusDays(1));
            update.executeWithoutTrigger();
        }
        return new Return();
    }

    /**
     * Method to get the id of the related product. Needed as a criteria for the
     * queries.
     *
     * @param _instance Instance to search for
     * @return id of the product
     * @throws EFapsException on error
     */
    private Long getProdId(final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        print.addAttribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink);
        print.execute();
        return print.<Long>getAttribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink);
    }

    /**
     * @param _parameter    Parameter as passed from the eFaps API
     * @return  list of instance
     * @throws EFapsException on error
     */
    public Return getPriceListPositionInstances(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<Instance> instances = new ArrayList<Instance>();

        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Map<?, ?> filter = (Map<?, ?>) _parameter.get(ParameterValues.OTHERS);

        final String types = (String) properties.get("Types");
        for (final String typeStr : types.split(";")) {
            final Type type = Type.get(typeStr);
            final QueryBuilder queryBldr = new QueryBuilder(type);
            analyzeTable(_parameter, filter, queryBldr, type);
            final AttributeQuery attrQuery = queryBldr.getAttributeQuery(CIProducts.ProductPricelistAbstract.ID);

            final QueryBuilder posQueryBldr = new QueryBuilder(CIProducts.ProductPricelistPosition);
            posQueryBldr.addWhereAttrInQuery(CIProducts.ProductPricelistPosition.ProductPricelist, attrQuery);
            final InstanceQuery query = posQueryBldr.getQuery();
            instances.addAll(query.execute());
        }
        ret.put(ReturnValues.VALUES, instances);
        return ret;
    }
}
