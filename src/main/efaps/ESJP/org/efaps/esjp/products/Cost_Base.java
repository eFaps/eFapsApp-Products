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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormProducts;
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
        print.executeWithoutAccessCheck();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCost);
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil, date);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink,
                        print.getAttribute(CIProducts.ProductCost.ProductLink));
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        while (query.next()) {
            if (!query.getCurrentValue().equals(costInstance)) {
                final Update update = new Update(query.getCurrentValue());
                update.add(CIProducts.ProductCost.ValidUntil, date.minusDays(1));
                update.executeWithoutTrigger();
            }
        }
        return new Return();
    }

    /**
     * Method for update a field of the contact.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return retVal with values of the contact.
     * @throws EFapsException on error.
     */
    public Return updateFields4CostCalculation(final Parameter _parameter)
        throws EFapsException
    {
        final String quantityStr = _parameter
                        .getParameterValue(CIFormProducts.Products_ProductCostCalculateForm.quantity.name);
        final String costStr = _parameter.getParameterValue(CIFormProducts.Products_ProductCostCalculateForm.cost.name);

        final BigDecimal quantity = parse(quantityStr);
        final BigDecimal cost = parse(costStr);
        BigDecimal price = BigDecimal.ZERO;
        if (quantity.signum() > 0 && cost.signum() > 0) {

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCost);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, _parameter.getInstance().getId());
            queryBldr.addOrderByAttributeDesc(CIProducts.ProductCost.ValidUntil);
            final InstanceQuery query = queryBldr.getQuery();
            query.setLimit(1);
            final List<Instance> instances = query.execute();
            if (!instances.isEmpty()) {
                final PrintQuery print = new PrintQuery(instances.get(0));
                print.addAttribute(CIProducts.ProductCost.Price);
                print.execute();
                final BigDecimal oldPrice = print.<BigDecimal>getAttribute(CIProducts.ProductCost.Price);
                final QueryBuilder invQueryBldr = new QueryBuilder(CIProducts.Inventory);
                invQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _parameter.getInstance().getId());
                final MultiPrintQuery multi = invQueryBldr.getPrint();
                multi.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.Reserved);
                multi.execute();

                BigDecimal oldQuantity = BigDecimal.ZERO;
                while (multi.next()) {
                    final BigDecimal quantTmp = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
                    final BigDecimal resTmp = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Reserved);
                    if (quantTmp != null) {
                        oldQuantity = oldQuantity.add(quantTmp);
                    }
                    if (resTmp != null) {
                        oldQuantity = oldQuantity.add(resTmp);
                    }
                }
                price = oldPrice.multiply(oldQuantity).add(cost.multiply(quantity)).setScale(8)
                                .divide(oldQuantity.add(quantity), BigDecimal.ROUND_HALF_UP)
                                .setScale(getFractionDigit(), BigDecimal.ROUND_HALF_UP);
            } else {
                price = cost;
            }
        }

        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();

        final String targetFieldName = (String) properties.get("fieldName");
        if (targetFieldName != null) {
            map.put(targetFieldName, price.toString());
        }

        final String diplayFieldName = (String) properties.get("displayFieldName");
        if (diplayFieldName != null) {
            final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                            .getLocale());
            formater.setMaximumFractionDigits(getFractionDigit());
            map.put(diplayFieldName, formater.format(price));
        }

        if (!map.isEmpty()) {
            list.add(map);
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    protected Integer getFractionDigit()
    {
        return 2;
    }

    /**
     * @param _value value to be parsed to an BigDecimal
     * @return BigDecimal
     * @throws EFapsException on parse exception
     */
    public BigDecimal parse(final String _value)
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setParseBigDecimal(true);
        BigDecimal ret;
        try {
            if (_value != null && _value.length() > 0) {
                ret = (BigDecimal) formater.parse(_value);
            } else {
                ret = BigDecimal.ZERO;
            }
        } catch (final ParseException e) {
            ret = BigDecimal.ZERO;
        }
        return ret;
    }

    public CostBean getCost(final Parameter _parameter,
                            final Instance _prodInst)
        throws EFapsException
    {
        return getCost(_parameter, new DateTime(), _prodInst);
    }

    public CostBean getCost(final Parameter _parameter,
                            final DateTime _date,
                            final Instance _prodInst)
        throws EFapsException

    {
        return getCosts(_parameter, _prodInst).get(_prodInst);
    }

    public Map<Instance, CostBean> getCosts(final Parameter _parameter,
                                            final Instance... _prodInst)
        throws EFapsException
    {

        return getCosts(_parameter, new DateTime(), _prodInst);
    }

    public Map<Instance, CostBean> getCosts(final Parameter _parameter,
                                            final DateTime _date,
                                            final Instance... _prodInsts)
        throws EFapsException
    {
        final Map<Instance, CostBean> ret = new HashMap<>();
        if (_prodInsts != null && _prodInsts.length > 0) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCost);
            queryBldr.addWhereAttrLessValue(CIProducts.ProductCost.ValidFrom, _date.withTimeAtStartOfDay()
                            .plusMinutes(1));
            queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCost.ValidUntil,
                            _date.withTimeAtStartOfDay().minusMinutes(1));
            queryBldr.addWhereAttrEqValue(CIProducts.ProductCost.ProductLink, (Object[]) _prodInsts);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCurInst = SelectBuilder.get().linkto(CIProducts.ProductCost.CurrencyLink).instance();
            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.ProductCost.ProductLink).instance();
            multi.addSelect(selCurInst, selProdInst);
            multi.addAttribute(CIProducts.ProductCost.Price);
            multi.execute();
            while (multi.next()) {
                final Instance curInst = multi.getSelect(selCurInst);
                final Instance prodInst = multi.getSelect(selProdInst);
                final BigDecimal cost = multi.getAttribute(CIProducts.ProductCost.Price);
                final CostBean bean = new CostBean().setProductInstance(prodInst).setCurrencyInstance(curInst)
                                .setCost(cost);
                ret.put(prodInst, bean);
            }
        }
        return ret;
    }

    protected CostBean getBean(final Parameter _parameter)
    {
        return new CostBean();
    }


    public static class CostBean
    {

        private Instance currencyInstance;
        private Instance productInstance;
        private BigDecimal cost;

        /**
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currencyInstance}.
         *
         * @param _currencyInstance value for instance variable
         *            {@link #currencyInstance}
         */
        public CostBean setCurrencyInstance(final Instance _currencyInstance)
        {
            this.currencyInstance = _currencyInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #productInstance}.
         *
         * @return value of instance variable {@link #productInstance}
         */
        public Instance getProductInstance()
        {
            return this.productInstance;
        }

        /**
         * Setter method for instance variable {@link #productInstance}.
         *
         * @param _productInstance value for instance variable
         *            {@link #productInstance}
         */
        public CostBean setProductInstance(final Instance _productInstance)
        {
            this.productInstance = _productInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}
         */
        public BigDecimal getCost()
        {
            return this.cost;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         */
        public CostBean setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
            return this;
        }

    }

}
