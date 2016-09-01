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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.RateInfo;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a7a6c100-8fce-4217-bc8e-c9066b1ab9e2")
@EFapsApplication("eFapsApp-Products")
public abstract class Cost_Base
    extends AbstractCommon
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
        final IUIValue fValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final DateTime value;
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            value = new DateTime().plusYears(10);
        } else {
            value = (DateTime) fValue.getObject();
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
    public Return trigger4InsertPost(final Parameter _parameter)
        throws EFapsException
    {
        final Instance costInstance = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(costInstance);
        print.addAttribute(CIProducts.ProductCostAbstract.ValidFrom,
                        CIProducts.ProductCostAbstract.ValidUntil,
                        CIProducts.ProductCostAbstract.ProductLink,
                        CIProducts.ProductCostAbstract.CurrencyLink);
        print.executeWithoutAccessCheck();

        final DateTime from = print.getAttribute(CIProducts.ProductCostAbstract.ValidFrom);
        final DateTime to = print.getAttribute(CIProducts.ProductCostAbstract.ValidUntil);

        final QueryBuilder queryBldr = new QueryBuilder(costInstance.getType());
        queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.StatusAbstract,
                        Status.find(CIProducts.ProductCostStatus.Active));
        queryBldr.addWhereAttrNotEqValue(CIProducts.ProductCostAbstract.ID, costInstance);
        queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCostAbstract.ValidUntil, from.minusSeconds(1));
        queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.ProductLink,
                        print.<Long>getAttribute(CIProducts.ProductCostAbstract.ProductLink));
        if (costInstance.getType().isCIType(CIProducts.ProductCostAlternative)) {
            queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.CurrencyLink,
                            print.<Long>getAttribute(CIProducts.ProductCostAbstract.CurrencyLink));
        }
        queryBldr.addOrderByAttributeAsc(CIProducts.ProductCostAbstract.ValidFrom);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.ProductCostAbstract.ValidUntil, CIProducts.ProductCostAbstract.ValidFrom,
                        CIProducts.ProductCostAbstract.Price, CIProducts.ProductCostAbstract.ProductLink,
                        CIProducts.ProductCostAbstract.CurrencyLink);
        multi.setEnforceSorted(true);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            final DateTime currentFrom = multi.getAttribute(CIProducts.ProductCostAbstract.ValidFrom);
            final DateTime currentTo = multi.getAttribute(CIProducts.ProductCostAbstract.ValidUntil);

            DateTime newFrom = currentFrom;
            DateTime newTo = currentTo;
            boolean deactivate = false;
            boolean create = false;

            if (!(currentFrom.isAfter(from) && currentFrom.isAfter(to))) {
                if (currentFrom.isBefore(from) && (currentTo.isAfter(from)
                                || currentTo.toLocalDate().equals(from.toLocalDate()))) {
                    newTo = from.minusDays(1);
                    create = true;
                    // new one is inside
                    if (currentTo.isAfter(to)) {
                        final Insert insert = new Insert(multi.getCurrentInstance().getType());
                        insert.add(CIProducts.ProductCostAbstract.ValidFrom, to.plusDays(1));
                        insert.add(CIProducts.ProductCostAbstract.ValidUntil, currentTo);
                        insert.add(CIProducts.ProductCostAbstract.Price,
                                        multi.<BigDecimal>getAttribute(CIProducts.ProductCostAbstract.Price));
                        insert.add(CIProducts.ProductCostAbstract.ProductLink,
                                        multi.<Long>getAttribute(CIProducts.ProductCostAbstract.ProductLink));
                        insert.add(CIProducts.ProductCostAbstract.CurrencyLink,
                                        multi.<Long>getAttribute(CIProducts.ProductCostAbstract.CurrencyLink));
                        insert.add(CIProducts.ProductCostAbstract.StatusAbstract,
                                        Status.find(CIProducts.ProductCostStatus.Active));
                        insert.executeWithoutTrigger();
                    }
                } else if (currentFrom.isAfter(from) && currentFrom.toLocalDate().equals(to.toLocalDate())) {
                    newFrom = to.minusDays(1);
                    create = true;
                } else if (currentFrom.isAfter(from) && currentFrom.isBefore(to) && currentTo.isAfter(to)) {
                    // in case that it must be move to far into the future just deactivate it
                    if (to.minusYears(5).isAfter(new DateTime()) && currentTo.minusYears(5).isAfter(new DateTime())) {
                        deactivate = true;
                    } else {
                        newFrom = to.plusDays(1);
                        create = true;
                    }
                } else if (currentFrom.isAfter(from) && currentFrom.isBefore(to)
                                && (currentTo.isBefore(to)  || currentTo.toLocalDate().equals(to.toLocalDate()))) {
                    deactivate = true;
                } else if (currentFrom.toLocalDate().equals(from.toLocalDate())
                                && currentFrom.toLocalDate().equals(currentTo.toLocalDate())) {
                    deactivate = true;
                } else if (currentFrom.toLocalDate().equals(from.toLocalDate())
                                && currentTo.isBefore(to)) {
                    deactivate = true;
                } else if (currentFrom.isAfter(from) && currentTo.isBefore(to)) {
                    deactivate = true;
                } else if (currentFrom.toLocalDate().equals(from.toLocalDate())
                                && currentTo.toLocalDate().equals(to.toLocalDate())) {
                    deactivate = true;
                }
            }

            if (create || deactivate) {
                final Update update = new Update(multi.getCurrentInstance());
                update.add(CIProducts.ProductCostAbstract.StatusAbstract,
                                Status.find(CIProducts.ProductCostStatus.Inactive));
                update.executeWithoutTrigger();
            }
            if (create) {
                final Insert insert = new Insert(multi.getCurrentInstance().getType());
                insert.add(CIProducts.ProductCostAbstract.ValidFrom, newFrom);
                insert.add(CIProducts.ProductCostAbstract.ValidUntil, newTo);
                insert.add(CIProducts.ProductCostAbstract.Price,
                                multi.<BigDecimal>getAttribute(CIProducts.ProductCostAbstract.Price));
                insert.add(CIProducts.ProductCostAbstract.ProductLink,
                                multi.<Long>getAttribute(CIProducts.ProductCostAbstract.ProductLink));
                insert.add(CIProducts.ProductCostAbstract.CurrencyLink,
                                multi.<Long>getAttribute(CIProducts.ProductCostAbstract.CurrencyLink));
                insert.add(CIProducts.ProductCostAbstract.StatusAbstract,
                                Status.find(CIProducts.ProductCostStatus.Active));
                insert.executeWithoutTrigger();
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
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();

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

    /**
     * Gets the fraction digit.
     *
     * @return the fraction digit
     */
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

    /**
     * Gets the cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _prodInst the prod inst
     * @return the cost
     * @throws EFapsException on error
     */
    public CostBean getCost(final Parameter _parameter,
                            final Instance _prodInst)
        throws EFapsException
    {
        return getCost(_parameter, new DateTime(), _prodInst);
    }

    /**
     * Gets the cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _prodInst the prod inst
     * @return the cost
     * @throws EFapsException on error
     */
    public CostBean getCost(final Parameter _parameter,
                            final DateTime _date,
                            final Instance _prodInst)
        throws EFapsException

    {
        return getCosts(_parameter, _date, _prodInst).get(_prodInst);
    }

    /**
     * Gets the cost.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _prodInst the prod inst
     * @return the cost
     * @throws EFapsException on error
     */
    public CostBean getAlternativeCost(final Parameter _parameter,
                                       final DateTime _date,
                                       final Instance _alterCurrencyInstance,
                                       final Instance _prodInst)
        throws EFapsException

    {
        return getAlternativeCosts(_parameter, _date, _alterCurrencyInstance, _prodInst).get(_prodInst);
    }

    /**
     * Gets the costs.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _prodInst the prod inst
     * @return the costs
     * @throws EFapsException on error
     */
    public Map<Instance, CostBean> getCosts(final Parameter _parameter,
                                            final Instance... _prodInst)
        throws EFapsException
    {

        return getCosts(_parameter, new DateTime(), _prodInst);
    }

    /**
     * Gets the costs.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _prodInsts the prod insts
     * @return the costs
     * @throws EFapsException on error
     */
    public Map<Instance, CostBean> getAlternativeCosts(final Parameter _parameter,
                                                       final DateTime _date,
                                                       final Instance _alterCurrencyInstance,
                                                       final Instance... _prodInsts)
        throws EFapsException
    {
        return getCostsInternal(_parameter, _date, _alterCurrencyInstance, _prodInsts);
    }

    /**
     * Gets the costs.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _prodInsts the prod insts
     * @return the costs
     * @throws EFapsException on error
     */
    public Map<Instance, CostBean> getCosts(final Parameter _parameter,
                                            final DateTime _date,
                                            final Instance... _prodInsts)
        throws EFapsException
    {
        return getCostsInternal(_parameter, _date, null, _prodInsts);
    }

    /**
     * Gets the costs.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _prodInsts the prod insts
     * @return the costs
     * @throws EFapsException on error
     */
    protected Map<Instance, CostBean> getCostsInternal(final Parameter _parameter,
                                                       final DateTime _date,
                                                       final Instance _alterCurrencyInstance,
                                                       final Instance... _prodInsts)
        throws EFapsException
    {
        final Map<Instance, CostBean> ret = new HashMap<>();
        if (_prodInsts != null && _prodInsts.length > 0) {
            final QueryBuilder queryBldr;
            if (_alterCurrencyInstance != null && _alterCurrencyInstance.isValid()) {
                queryBldr = new QueryBuilder(CIProducts.ProductCostAlternative);
                queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAlternative.CurrencyLink, _alterCurrencyInstance);
            } else {
                queryBldr = new QueryBuilder(CIProducts.ProductCost);
            }

            queryBldr.addWhereAttrLessValue(CIProducts.ProductCostAbstract.ValidFrom, _date.withTimeAtStartOfDay()
                            .plusMinutes(1));
            queryBldr.addWhereAttrGreaterValue(CIProducts.ProductCostAbstract.ValidUntil, _date.withTimeAtStartOfDay()
                            .minusMinutes(1));
            queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.ProductLink, (Object[]) _prodInsts);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAbstract.StatusAbstract,
                            Status.find(CIProducts.ProductCostStatus.Active));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selCurInst = SelectBuilder.get().linkto(CIProducts.ProductCostAbstract.CurrencyLink)
                            .instance();
            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.ProductCostAbstract.ProductLink)
                            .instance();
            multi.addSelect(selCurInst, selProdInst);
            multi.addAttribute(CIProducts.ProductCostAbstract.Price, CIProducts.ProductCostAbstract.ValidFrom,
                            CIProducts.ProductCostAbstract.ValidUntil, CIProducts.ProductCostAbstract.Created);
            multi.execute();
            while (multi.next()) {
                final Instance prodInst = multi.getSelect(selProdInst);
                final CostBean bean = new CostBean()
                                .setDate(_date)
                                .setProductInstance(prodInst)
                                .setCurrencyInstance(multi.<Instance>getSelect(selCurInst))
                                .setCost(multi.<BigDecimal>getAttribute(CIProducts.ProductCost.Price))
                                .setValidFrom(multi.<DateTime>getAttribute(CIProducts.ProductCost.ValidFrom))
                                .setValidUntil(multi.<DateTime>getAttribute(CIProducts.ProductCost.ValidUntil))
                                .setCreated(multi.<DateTime>getAttribute(CIProducts.ProductCost.Created));
                ret.put(prodInst, bean);
            }
        }
        return ret;
    }

    /**
     * Gets the bean.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the bean
     */
    protected CostBean getBean(final Parameter _parameter)
    {
        return new CostBean();
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    protected static BigDecimal getCost4Currency(final Parameter _parameter,
                                                 final Instance _productInstance,
                                                 final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost.getCost4Currency(_parameter, new DateTime(), _productInstance, _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _alterCurrencyInstance the alter currency instance
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    protected static BigDecimal getAlternativeCost4Currency(final Parameter _parameter,
                                                            final Instance _alterCurrencyInstance,
                                                            final Instance _productInstance,
                                                            final Instance _currencyInstance)
        throws EFapsException
    {
        return Cost.getAlternativeCost4Currency(_parameter, new DateTime(), _alterCurrencyInstance,
                        _productInstance, _currencyInstance);
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    protected static BigDecimal getCost4Currency(final Parameter _parameter,
                                                 final DateTime _date,
                                                 final Instance _productInstance,
                                                 final Instance _currencyInstance)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final CostBean costBean = new Cost().getCost(_parameter, _date, _productInstance);
        if (costBean != null) {
            ret = costBean.getCost4Currency(_parameter, _currencyInstance);
        }
        return ret;
    }

    /**
     * Gets the cost4 currency.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _date the date
     * @param _alterCurrencyInstance the alter currency instance
     * @param _productInstance the product instance
     * @param _currencyInstance the currency instance
     * @return the cost4 currency
     * @throws EFapsException on error
     */
    protected static BigDecimal getAlternativeCost4Currency(final Parameter _parameter,
                                                            final DateTime _date,
                                                            final Instance _alterCurrencyInstance,
                                                            final Instance _productInstance,
                                                            final Instance _currencyInstance)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final CostBean costBean = new Cost().getAlternativeCost(_parameter, _date, _alterCurrencyInstance,
                        _productInstance);
        if (costBean != null) {
            ret = costBean.getCost4Currency(_parameter, _currencyInstance);
        }
        return ret;
    }


    /**
     * The Class CostBean.
     */
    public static class CostBean
    {

        /** The date. */
        private DateTime validFrom;

        /** The date. */
        private DateTime validUntil;

        /** The date. */
        private DateTime created;

        /** The date. */
        private DateTime date;

        /** The currency instance. */
        private Instance currencyInstance;

        /** The product instance. */
        private Instance productInstance;

        /** The cost. */
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
         * @return the cost bean
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
         * @return the cost bean
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
         * Gets the cost4 currency.
         *
         * @param _parameter the _parameter
         * @param _currencyInst the _currency inst
         * @return the cost4 currency
         * @throws EFapsException the e faps exception
         */
        public BigDecimal getCost4Currency(final Parameter _parameter,
                                           final Instance _currencyInst)
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (getCurrencyInstance().equals(_currencyInst)) {
                ret = getCost();
            } else if (getCost().compareTo(BigDecimal.ZERO) != 0) {
                final RateInfo[] rateInfos = new Currency().evaluateRateInfos(_parameter, getDate(),
                                getCurrencyInstance(), _currencyInst);
                final RateInfo rateInfo = rateInfos[2];
                ret = getCost().setScale(8, BigDecimal.ROUND_HALF_UP)
                                .divide(rateInfo.getRate(), BigDecimal.ROUND_HALF_UP);
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         * @return the cost bean
         */
        public CostBean setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

        /**
         * Setter method for instance variable {@link #date}.
         *
         * @param _date value for instance variable {@link #date}
         * @return the cost bean
         */
        public CostBean setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #validFrom}.
         *
         * @return value of instance variable {@link #validFrom}
         */
        public DateTime getValidFrom()
        {
            return this.validFrom;
        }

        /**
         * Setter method for instance variable {@link #validFrom}.
         *
         * @param _validFrom value for instance variable {@link #validFrom}
         * @return the cost bean
         */
        public CostBean setValidFrom(final DateTime _validFrom)
        {
            this.validFrom = _validFrom;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #validUntil}.
         *
         * @return value of instance variable {@link #validUntil}
         */
        public DateTime getValidUntil()
        {
            return this.validUntil;
        }

        /**
         * Setter method for instance variable {@link #validUntil}.
         *
         * @param _validUntil value for instance variable {@link #validUntil}
         * @return the cost bean
         */
        public CostBean setValidUntil(final DateTime _validUntil)
        {
            this.validUntil = _validUntil;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #created}.
         *
         * @return value of instance variable {@link #created}
         */
        public DateTime getCreated()
        {
            return this.created;
        }

        /**
         * Setter method for instance variable {@link #created}.
         *
         * @param _created value for instance variable {@link #created}
         * @return the cost bean
         */
        public CostBean setCreated(final DateTime _created)
        {
            this.created = _created;
            return this;
        }
    }
}
